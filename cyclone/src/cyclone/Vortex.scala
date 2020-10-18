package cyclone

import com.raquo.airstream.features.FlattenStrategy.ConcurrentStreamStrategy
import com.raquo.laminar.api.L._

private[cyclone] trait Vortex[I, S, O] extends Cyclone[I, S, O] {
  private lazy val effects = new EventBus[Flow[_]]

  private lazy val inputBus            = new EventBus[I]
  override lazy val input: WriteBus[I] = inputBus.writer

  protected val initialEffect: Flow[_] = noEffect

  protected val inputHandler: Signal[InputHandler[I]]

  private lazy val flatMapEffects: EventStream[Flow[_]] =
    effects.events.collect {
      case EmptyFlow        => noEffect
      case x: FlatMap[_, _] => x
      case x: Flow[_]       => x.flatMap[Nothing](_ => noEffect)
    }

  protected lazy val streamFlattenStrategy: FlattenStrategy[EventStream, EventStream, EventStream] =
    ConcurrentStreamStrategy

  private lazy val streamedEffects: EventStream[Flow[_]] = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: Stream[S, _], b: (Any => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .flatMap(a.fn)(streamFlattenStrategy)
          .map(_.flatMap(b))
    }
    flatMapEffects.collect(select).flatten(streamFlattenStrategy)
  }

  private lazy val stateStreamAndK: EventStream[((S, S), Flow[_])] = {
    def select: PartialFunction[Flow[_], EventStream[
      ((S, S), Flow[_])
    ]] = {
      case FlatMap(a: UpdateState[S], b: (((S, S)) => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(s => s -> a.fn(s))
          .map(a => a -> b(a))
    }
    flatMapEffects.collect(select).flatten
  }

  protected lazy val stateStream: EventStream[S] =
    stateStreamAndK.map(_._1._2)

  private lazy val inputHandlerStreamAndK: EventStream[((InputHandler[I], InputHandler[I]), Flow[_])] = {
    def select: PartialFunction[Flow[_], EventStream[
      ((InputHandler[I], InputHandler[I]), Flow[_])
    ]] = {
      case FlatMap(a: UpdateInputHandler[I], b: (((InputHandler[I], InputHandler[I])) => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(inputHandler)
          .map(h => h -> a.fn(h))
          .map(a => a -> b(a))
    }
    flatMapEffects.collect(select).flatten
  }

  protected lazy val inputHandlerStream: EventStream[InputHandler[I]] =
    inputHandlerStreamAndK.map(_._1._2)

  private lazy val outputStreamAndK: EventStream[(O, Flow[_])] = {
    def select: PartialFunction[Flow[_], EventStream[(O, Flow[_])]] = {
      case FlatMap(a: EmitOutput[S, O], b: (O => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(s => a.fn(s))
          .map(a => a -> b(a))
    }
    flatMapEffects.collect(select).flatten
  }

  override lazy val output: EventStream[O] = outputStreamAndK.map(_._1)

  private lazy val handledInputs = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: EmitInput[S, I], b: (Input => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(s => a.fn(s))
          .withCurrentValueOf(inputHandler)
          .filter { case (input, handler) => handler.isDefinedAt(input) }
          .map2((input, handler) => handler(input).flatMap(_ => b(input)))
    }
    flatMapEffects.collect(select).flatten
  }

  private lazy val handledPures = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: Pure[S, Any] @unchecked, b: (Any => Flow[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(s => a.fn(s))
          .map(b)
    }
    flatMapEffects.collect(select).flatten
  }

  private def inContextEffects[E <: Element](e: E) = {
    def select: PartialFunction[Flow[_], EventStream[Flow[_]]] = {
      case FlatMap(a: InContext[E, Any] @unchecked, b: (Any => Flow[_])) =>
        EventStream
          .fromValue(e, emitOnce = true)
          .map(a.fn)
          .map(b)
    }
    flatMapEffects.collect(select).flatten
  }

  private def loopbackEffects[E <: Element](e: E): EventStream[Flow[_]] = EventStream.merge(
    handledPures,
    handledInputs,
    inContextEffects(e),
    streamedEffects,
    stateStreamAndK.map(_._2),
    inputHandlerStreamAndK.map(_._2),
    outputStreamAndK.map(_._2)
  )

  override def bind[E <: Element](startEffect: Flow[_]): Mod[E] =
    inContext[E] { el =>
      el.amend(
        loopbackEffects(el) --> effects.writer,
        inputBus.events.map(i => EmitInput[S, I](_ => i)) --> effects.writer,
        EventStream.fromValue(initialEffect, emitOnce = true) --> effects.writer,
        EventStream.fromValue(startEffect, emitOnce = true) --> effects.writer
      )
    }

}
