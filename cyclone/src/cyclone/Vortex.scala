package cyclone

import com.raquo.airstream.features.FlattenStrategy.ConcurrentStreamStrategy
import com.raquo.laminar.api.L._

import scala.util.Try

private[cyclone] trait Vortex[I, S, O] extends Cyclone[I, S, O] {
  import Effects._
  import Types._

  private lazy val effects             = new EventBus[Effect[_]]
  private lazy val inputBus            = new EventBus[I]
  override lazy val input: WriteBus[I] = inputBus.writer

  protected val inputHandler: Signal[InputHandler[I]]

  private lazy val flatMapEffects: EventStream[Effect[_]] =
    effects.events.collect {
      case NoEffect         => noEffect
      case x: FlatMap[_, _] => x
      case x: Effect[_]     => x.flatMap[Nothing](_ => noEffect)
    }

  protected lazy val streamFlattenStrategy: FlattenStrategy[EventStream, EventStream, EventStream] =
    ConcurrentStreamStrategy

  private lazy val streamedEffects: EventStream[Effect[_]] = {
    def select: PartialFunction[Effect[_], EventStream[Effect[_]]] = {
      case FlatMap(a: Stream[S, _], b: (Any => Effect[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .flatMap(a.fn)(streamFlattenStrategy)
          .map(_.flatMap(b))
    }
    flatMapEffects.collect(select).flatten(streamFlattenStrategy)
  }

  private lazy val stateStreamAndK: EventStream[((S, S), Effect[_])] = {
    def select: PartialFunction[Effect[_], EventStream[
      ((S, S), Effect[_])
    ]] = {
      case FlatMap(a: UpdateState[S], b: (((S, S)) => Effect[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(s => s -> a.fn(s))
          .map(a => a -> b(a))
    }
    flatMapEffects
      .collect(select)
      .flatten
  }

  protected lazy val stateStream: EventStream[S] =
    stateStreamAndK.map(_._1._2)

  private lazy val inputHandlerStreamAndK: EventStream[((InputHandler[I], InputHandler[I]), Effect[_])] = {
    def select: PartialFunction[Effect[_], EventStream[
      ((InputHandler[I], InputHandler[I]), Effect[_])
    ]] = {
      case FlatMap(a: UpdateInputHandler[I], b: (((InputHandler[I], InputHandler[I])) => Effect[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(inputHandler)
          .map(h => h -> a.fn(h))
          .map(a => a -> b(a))
    }
    flatMapEffects
      .collect(select)
      .flatten
  }

  protected lazy val inputHandlerStream: EventStream[InputHandler[I]] =
    inputHandlerStreamAndK.map(_._1._2)

  private lazy val outputStreamAndK: EventStream[(O, Effect[_])] = {
    def select: PartialFunction[Effect[_], EventStream[(O, Effect[_])]] = {
      case FlatMap(a: EmitOutput[S, O], b: (O => Effect[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(s => a.fn(s))
          .map(a => a -> b(a))
    }
    flatMapEffects
      .collect(select)
      .flatten
  }

  override lazy val output: EventStream[O] = outputStreamAndK.map(_._1)

  private lazy val handledInputs = {
    def select: PartialFunction[Effect[_], EventStream[Effect[_]]] = {
      case FlatMap(a: EmitInput[S, I], b: (Input => Effect[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(s => a.fn(s))
          .withCurrentValueOf(inputHandler)
          .filter { case (input, handler) => handler.isDefinedAt(input) }
          .map2((input, handler) => handler(input).flatMap(_ => b(input)))
    }
    flatMapEffects
      .collect(select)
      .flatten
  }

  private lazy val handledPures = {
    def select: PartialFunction[Effect[_], EventStream[Effect[_]]] = {
      case FlatMap(a: Pure[S, Any] @unchecked, b: (Any => Effect[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(s => a.fn(s))
          .map(b(_))
    }
    flatMapEffects
      .collect(select)
      .flatten
  }

  private lazy val loopbackEffects: EventStream[Effect[_]] = EventStream.merge(
    handledPures,
    handledInputs,
    streamedEffects,
    stateStreamAndK.map(_._2),
    inputHandlerStreamAndK.map(_._2),
    outputStreamAndK.map(_._2)
  )

  override def bind[E <: Element](initialEffect: MountContext[E] => Effect[_]): Mod[E] =
    inContext[E] { el =>
      el.amend(
        loopbackEffects --> effects.writer,
        inputBus.events.map(i => EmitInput[S, I](_ => i)) --> effects.writer,
        onMountBind(ctx => EventStream.fromTry(Try(initialEffect(ctx)), emitOnce = true) --> effects.writer)
      )
    }

}
