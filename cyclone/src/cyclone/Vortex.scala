package cyclone

import com.raquo.airstream.features.FlattenStrategy.ConcurrentStreamStrategy
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ParentNode

private[cyclone] trait Vortex[I, S, O] extends Types[I, S, O] with Cyclone[I, S, O] {
  import Effects._
  import Types._

  private lazy val effects = new EventBus[Effect[_]]

  protected val inputHandler: Signal[InputHandler[I]]
  protected val actionHandler: Signal[ActionHandler[A]]

  private lazy val flatMapEffects: EventStream[Effect[_]] =
    effects.events.collect {
      case NoEffect         => NoEffect
      case x: FlatMap[_, _] => x
      case x: Effect[_]     => x.flatMap[Nothing]((_: Any) => NoEffect)
    }

  protected lazy val streamFlattenStrategy: FlattenStrategy[EventStream, EventStream, EventStream] =
    ConcurrentStreamStrategy

  private lazy val streamedEffects: EventStream[Effect[_]] = {
    def select: PartialFunction[Effect[_], EventStream[Effect[_]]] = {
      case FlatMap(a: Stream[State, _], b: Function1[Any, Effect[_]]) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .flatMap(a.fn)(streamFlattenStrategy)
          .map(_.flatMap(b))
    }
    flatMapEffects.collect(select).flatten(streamFlattenStrategy)
  }

  private lazy val stateStreamAndK: EventStream[((State, State), Effect[_])] = {
    def select: PartialFunction[Effect[_], EventStream[
      ((State, State), Effect[_])
    ]] = {
      case FlatMap(
          UpdateState(fn: Function1[S, S]),
          b: Function1[(State, State), Effect[_]]
          ) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(a => a -> fn(a))
          .map(a => a -> b(a))
    }
    flatMapEffects
      .collect(select)
      .flatten
  }

  protected lazy val stateStream: EventStream[State] =
    stateStreamAndK.map(_._1._2)

  private lazy val actionHandlerStreamAndK: EventStream[((ActionHandler[A], ActionHandler[A]), Effect[_])] = {
    def select: PartialFunction[Effect[_], EventStream[
      ((ActionHandler[A], ActionHandler[A]), Effect[_])
    ]] = {
      case FlatMap(
          UpdateActionHandler(
            fn: Function1[ActionHandler[A], ActionHandler[A]]
          ),
          b: Function1[(ActionHandler[A], ActionHandler[A]), Effect[_]]
          ) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(actionHandler)
          .map(a => a -> fn(a))
          .map(a => a -> b(a))
    }
    flatMapEffects
      .collect(select)
      .flatten
  }

  protected lazy val actionHandlerStream: EventStream[ActionHandler[A]] =
    actionHandlerStreamAndK.map(_._1._2)

  private lazy val inputHandlerStreamAndK: EventStream[((InputHandler[I], InputHandler[I]), Effect[_])] = {
    def select: PartialFunction[Effect[_], EventStream[
      ((InputHandler[I], InputHandler[I]), Effect[_])
    ]] = {
      case FlatMap(
          UpdateInputHandler(fn: Function1[InputHandler[I], InputHandler[I]]),
          b: Function1[(InputHandler[I], InputHandler[I]), Effect[_]]
          ) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(inputHandler)
          .map(a => a -> fn(a))
          .map(a => a -> b(a))
    }
    flatMapEffects
      .collect(select)
      .flatten
  }

  protected lazy val inputHandlerStream: EventStream[InputHandler[I]] =
    inputHandlerStreamAndK.map(_._1._2)

  override lazy val input: Observer[Input] =
    effects.writer.contramap(i => EmitInput[S, I](_ => i))

  private lazy val outputStreamAndK: EventStream[(Output, Effect[_])] = {
    def select: PartialFunction[Effect[_], EventStream[(Output, Effect[_])]] = {
      case FlatMap(EmitOutput(fn: Function1[S, O]), b: (Output => Effect[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(fn)
          .map(a => a -> b(a))
    }
    flatMapEffects
      .collect(select)
      .flatten
  }

  override lazy val output: Observable[Output] = outputStreamAndK.map(_._1)

  private lazy val handledActions = {
    def select: PartialFunction[Effect[_], EventStream[Effect[_]]] = {
      case FlatMap(EmitAction(fn: Function1[S, A]), b: (Action => Effect[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(fn)
          .withCurrentValueOf(actionHandler)
          .filter { case (action, handler) => handler.isDefinedAt(action) }
          .map2((action, handler) => handler(action).flatMap(_ => b(action)))
    }
    flatMapEffects
      .collect(select)
      .flatten
  }

  private lazy val handledInputs = {
    def select: PartialFunction[Effect[_], EventStream[Effect[_]]] = {
      case FlatMap(EmitInput(fn: Function1[S, I]), b: (Input => Effect[_])) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(fn)
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
      case FlatMap(Pure(fn: Function1[S, _]), b: Function1[_, Effect[_]]) =>
        EventStream
          .fromValue((), emitOnce = true)
          .sample(state)
          .map(fn)
          .map(b(_))
    }
    flatMapEffects
      .collect(select)
      .flatten
  }

  private lazy val loopbackEffects: EventStream[Effect[_]] = EventStream.merge(
    handledPures,
    handledActions,
    handledInputs,
    streamedEffects,
    stateStreamAndK.map(_._2),
    actionHandlerStreamAndK.map(_._2),
    inputHandlerStreamAndK.map(_._2),
    outputStreamAndK.map(_._2)
  )

  protected def unmountBinder(e: Element): Option[ParentNode.Base] =
    e.maybeParent

  override lazy val mod: Mod[Element] = inContext { el =>
    el.amend(
      loopbackEffects --> effects.writer,
      onMountBind(ctx =>
        EventStream
          .fromValue(EmitAction[S, A](_ => Mounted(ctx)), emitOnce = true) --> effects.writer
      )
    )
  }

}
