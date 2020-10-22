package cyclone

import com.raquo.airstream.features.FlattenStrategy.ConcurrentStreamStrategy
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

import scala.util.{Failure, Success, Try}

private[cyclone] trait Vortex[E <: Element, I, S, O] extends Cyclone[E, I, S, O] {

  private type Kont[X] = X => Flow[_]

  private lazy val flowBus = new EventBus[Flow[_]]

  private lazy val inputBus   = new EventBus[I]
  lazy val input: WriteBus[I] = inputBus.writer

  protected val mainFlow: Flow[_] = EmptyFlow

  protected val inputHandler: Signal[Handler]

  private lazy val flatMapFlow: EventStream[Flow[_]] =
    flowBus.events.collect {
      case FlatMap(FlatMap(a, b: Kont[Any]), k: Kont[Any]) => FlatMap(a, b(_: Any).flatMap(k))
      case x: FlatMap[_, _]                                => x
      case x: Flow[_]                                      => x.flatMap(_ => EmptyFlow)
    }

  private lazy val nestedFlatMap: EventStream[Flow[_]] =
    flatMapFlow.collect {
      case FlatMap(FlatMap(a, b: Kont[Any]), k: Kont[Any]) => FlatMap(a, b(_: Any).flatMap(k))
    }

  protected lazy val streamFlattenStrategy: FlattenStrategy[EventStream, EventStream, EventStream] =
    ConcurrentStreamStrategy

  private lazy val fromStream: EventStream[Flow[_]] = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: FromStream[_], b: Kont[Any]) =>
        a.fn().map(_.flatMap(b))
    }
    flatMapFlow.collect(select).flatten(streamFlattenStrategy)
  }

  private lazy val nestedTryUpdate: EventStream[Flow[_]] = {
    flatMapFlow.collect {
      case FlatMap(x @ TryUpdate(y @ TryUpdate(_)), k: Kont[Try[Try[Any]]]) =>
        FlatMap(y, (v: Try[Any]) => k(Success(v)))
    }
  }

  private def updateStateEffect(u: UpdateState): EventStream[(S, S)] =
    EventStream
      .fromValue((), emitOnce = true)
      .sample(state)
      .map(s => s -> u.fn(s))

  private lazy val updateState: EventStream[(Option[(S, S)], Flow[_])] = {
    flatMapFlow.collect {
      case FlatMap(u: UpdateState, k: Kont[(S, S)]) =>
        updateStateEffect(u).map(v => Some(v) -> k(v))
      case FlatMap(TryUpdate(u: UpdateState), k: Kont[Try[(S, S)]]) =>
        updateStateEffect(u).recoverToTry.map {
          case Failure(exception) =>
            None -> k(Failure(exception))
          case Success(v) =>
            Some(v) -> k(v)
        }
    }.flatten
  }

  protected lazy val stateChanges: EventStream[S] =
    updateState.collect {
      case (Some(_ -> newState), _) => newState
    }

  private def updateHandlerEffect(u: UpdateHandler): EventStream[(Handler, Handler)] =
    EventStream
      .fromValue((), emitOnce = true)
      .sample(inputHandler)
      .map(h => h -> u.fn(h))

  private lazy val updateHandler: EventStream[(Option[(Handler, Handler)], Flow[_])] = {
    flatMapFlow.collect {
      case FlatMap(u: UpdateHandler @unchecked, k: Kont[(Handler, Handler)]) =>
        updateHandlerEffect(u).map(v => Some(v) -> k(v))
      case FlatMap(TryUpdate(u: UpdateHandler @unchecked), k: Kont[Try[(Handler, Handler)]]) =>
        updateHandlerEffect(u).recoverToTry.map {
          case Failure(exception) =>
            None -> k(Failure(exception))
          case Success(v) =>
            Some(v) -> k(v)
        }
    }.flatten
  }

  protected lazy val handlerChanges: EventStream[Handler] =
    updateHandler.collect {
      case (Some(_ -> newHandler), _) => newHandler
    }

  private lazy val emitOutput: EventStream[(O, Flow[_])] = {
    def select: PartialFunction[Flow[_], EventStream[(O, Flow[_])]] = {
      case FlatMap(a: EmitOutput, b: Kont[O]) =>
        EventStream
          .fromTry(Try(a.fn()), emitOnce = true)
          .map(a => a -> b(a))
    }
    flatMapFlow.collect(select).flatten
  }

  override lazy val output: EventStream[O] = emitOutput.map(_._1)

  private lazy val emitInput = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: EmitInput, b: Kont[I]) =>
        EventStream
          .fromTry(Try(a.fn()), emitOnce = true)
          .withCurrentValueOf(inputHandler)
          .filter { case (input, handler) => handler.isDefinedAt(input) }
          .map2((input, handler) => handler(input).flatMap(_ => b(input)))
    }
    flatMapFlow.collect(select).flatten
  }

  private lazy val pure = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: Value[_], b: Kont[Any]) =>
        EventStream
          .fromValue((), emitOnce = true)
          .map(_ => a.fn())
          .map(b)
    }
    flatMapFlow.collect(select).flatten
  }

  private lazy val emptyFlows: EventStream[EmptyFlow.type] = {
    flatMapFlow.collect {
      case FlatMap(EmptyFlow, _) => EmptyFlow
    }
  }

  private def inContextEffects(c: MountContext[E]) = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: MountedContext, b: Kont[MountContext[E]]) =>
        EventStream
          .fromValue(c, emitOnce = true)
          .map(b)
    }
    flatMapFlow.collect(select).flatten
  }

  private def loopbackEffects(c: MountContext[E]): EventStream[Flow[_]] = EventStream.merge(
    pure,
    emitInput,
    inContextEffects(c),
    fromStream,
    updateState.map(_._2),
    updateHandler.map(_._2),
    emitOutput.map(_._2),
    nestedTryUpdate,
    nestedFlatMap
  )

  override def bind(): Binder[E] = {
    ReactiveElement.bindCallback(_) { ctx =>
      ctx.thisNode.amend(
        emptyFlows --> Observer.empty,
        loopbackEffects(ctx) --> flowBus.writer,
        inputBus.events.map(i => EmitInput(() => i)) --> flowBus.writer,
        EventStream.fromValue(mainFlow, emitOnce = true) --> flowBus.writer
      )
    }
  }

}
