package cyclone

import com.raquo.airstream.features.FlattenStrategy.ConcurrentStreamStrategy
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

private[cyclone] trait Whirl[E <: Element, I, S, O] extends Vortex[E, I, S, O] {

  private lazy val flowBus = new EventBus[Flow[_]]

  private lazy val inputBus   = new EventBus[I]
  lazy val input: WriteBus[I] = inputBus.writer

  protected val initialFlow: Flow[_] = EmptyFlow

  protected val inputHandler: Signal[Handler]

  private lazy val flatMapFlow: EventStream[Flow[_]] =
    flowBus.events.collect {
      case EmptyFlow        => EmptyFlow
      case x: FlatMap[_, _] => x
      case x: Flow[_]       => x.flatMap[Nothing](_ => EmptyFlow)
    }

  protected lazy val streamFlattenStrategy: FlattenStrategy[EventStream, EventStream, EventStream] =
    ConcurrentStreamStrategy

  private lazy val fromStreamFlows: EventStream[Flow[_]] = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: FromStream[_], b: (Any => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .flatMap(_ => a.fn())(streamFlattenStrategy)
          .map(_.flatMap(b))
    }
    flatMapFlow.collect(select).flatten(streamFlattenStrategy)
  }

  private lazy val stateStreamAndK: EventStream[((S, S), Flow[_])] = {
    def select: PartialFunction[Flow[_], EventStream[
      ((S, S), Flow[_])
    ]] = {
      case FlatMap(a: UpdateState, b: (((S, S)) => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(s => s -> a.fn(s))
          .map(a => a -> b(a))
    }
    flatMapFlow.collect(select).flatten
  }

  protected lazy val stateStream: EventStream[S] =
    stateStreamAndK.map(_._1._2)

  private lazy val inputHandlerStreamAndK: EventStream[((Handler, Handler), Flow[_])] = {
    def select: PartialFunction[Flow[_], EventStream[((Handler, Handler), Flow[_])]] = {
      case FlatMap(a: UpdateHandler @unchecked, b: (((Handler, Handler)) => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(inputHandler)
          .map(h => h -> a.fn(h))
          .map(a => a -> b(a))
    }
    flatMapFlow.collect(select).flatten
  }

  protected lazy val inputHandlerStream: EventStream[Handler] =
    inputHandlerStreamAndK.map(_._1._2)

  private lazy val outputStreamAndK: EventStream[(O, Flow[_])] = {
    def select: PartialFunction[Flow[_], EventStream[(O, Flow[_])]] = {
      case FlatMap(a: EmitOutput, b: (O => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .map(_ => a.fn())
          .map(a => a -> b(a))
    }
    flatMapFlow.collect(select).flatten
  }

  override lazy val output: EventStream[O] = outputStreamAndK.map(_._1)

  private lazy val handledInputs = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: EmitInput, b: (Input => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .map(_ => a.fn())
          .withCurrentValueOf(inputHandler)
          .filter { case (input, handler) => handler.isDefinedAt(input) }
          .map2((input, handler) => handler(input).flatMap(_ => b(input)))
    }
    flatMapFlow.collect(select).flatten
  }

  private lazy val handledPures = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: Pure[_], b: (Any => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .map(_ => a.fn())
          .map(b)
    }
    flatMapFlow.collect(select).flatten
  }

  private def inContextEffects(c: MountContext[E]) = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: MountedContext, b: (Any => Flow[_])) =>
        EventStream
          .fromValue(c, emitOnce = true)
          .map(b)
    }
    flatMapFlow.collect(select).flatten
  }

  private def loopbackEffects(c: MountContext[E]): EventStream[Flow[_]] = EventStream.merge(
    handledPures,
    handledInputs,
    inContextEffects(c),
    fromStreamFlows,
    stateStreamAndK.map(_._2),
    inputHandlerStreamAndK.map(_._2),
    outputStreamAndK.map(_._2)
  )

  override def bind(): Binder[E] = {
    ReactiveElement.bindCallback(_) { ctx =>
      ctx.thisNode.amend(
        loopbackEffects(ctx) --> flowBus.writer,
        inputBus.events.map(i => EmitInput(() => i)) --> flowBus.writer,
        EventStream.fromValue(initialFlow, emitOnce = true) --> flowBus.writer
      )
    }
  }

}
