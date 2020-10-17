package cyclone

import com.raquo.airstream.features.FlattenStrategy.ConcurrentStreamStrategy
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ParentNode

import scala.util.Try

private[cyclone] trait Vortex[I, S, O] extends Types[I, S, O] with Cyclone[I, S, O] with Effects[I, S, O] {
  private val effects = new EventBus[Effect[_]]

  protected val inputHandler: Signal[InputHandler]
  protected val actionHandler: Signal[ActionHandler]

  private val flatMapEffects: EventStream[Effect[_]] = effects.events.collect {
    case NoEffect         => NoEffect
    case x: FlatMap[_, _] => x
    case x: Effect[_]     => x.flatMap[Nothing]((_: Any) => NoEffect)
  }

  protected val streamFlattenStrategy: FlattenStrategy[EventStream, EventStream, EventStream] =
    ConcurrentStreamStrategy

  private val streamedEffects: EventStream[Effect[_]] = {
    def select: PartialFunction[Effect[_], EventStream[Effect[_]]] = {
      case FlatMap(a: Stream[_], b: Function1[_, Effect[_]]) => a.fn.map(b(_))
    }
    flatMapEffects.collect(select).flatten(streamFlattenStrategy)
  }

  private val stateStreamAndK: EventStream[((State, State), Effect[_])] = {
    def select: PartialFunction[Effect[_], State => EventStream[((State, State), Effect[_])]] = {
      case FlatMap(UpdateState(fn), b: Function1[(State, State), Effect[_]]) =>
        previous: State =>
          EventStream
            .fromTry(Try(fn(previous)), emitOnce = true)
            .map(a => previous -> a)
            .map(a => a -> b(a))
    }
    flatMapEffects
      .collect(select)
      .withCurrentValueOf(state)
      .map2(_ apply _)
      .flatten
  }

  protected val stateStream: EventStream[State] = stateStreamAndK.map(_._1._2)

  private val actionHandlerStreamAndK: EventStream[((ActionHandler, ActionHandler), Effect[_])] = {
    def select
    : PartialFunction[Effect[_], ActionHandler => EventStream[((ActionHandler, ActionHandler), Effect[_])]] = {
      case FlatMap(UpdateActionHandler(fn), b: Function1[(ActionHandler, ActionHandler), Effect[_]]) =>
        previous: ActionHandler =>
          EventStream
            .fromTry(Try(fn(previous)), emitOnce = true)
            .map(a => previous -> a)
            .map(a => a -> b(a))
    }
    flatMapEffects
      .collect(select)
      .withCurrentValueOf(actionHandler)
      .map2(_ apply _)
      .flatten
  }

  protected val actionHandlerStream: EventStream[ActionHandler] = actionHandlerStreamAndK.map(_._1._2)

  private val inputHandlerStreamAndK: EventStream[((InputHandler, InputHandler), Effect[_])] = {
    def select: PartialFunction[Effect[_], InputHandler => EventStream[((InputHandler, InputHandler), Effect[_])]] = {
      case FlatMap(UpdateInputHandler(fn), b: Function1[(InputHandler, InputHandler), Effect[_]]) =>
        previous: InputHandler =>
          EventStream
            .fromTry(Try(fn(previous)), emitOnce = true)
            .map(a => previous -> a)
            .map(a => a -> b(a))
    }
    flatMapEffects
      .collect(select)
      .withCurrentValueOf(inputHandler)
      .map2(_ apply _)
      .flatten
  }

  protected val inputHandlerStream: EventStream[InputHandler] = inputHandlerStreamAndK.map(_._1._2)

  override val input: Observer[Input] = effects.writer.contramap(i => EmitInput(_ => i))

  private val outputStreamAndK: EventStream[(Output, Effect[_])] = {
    def select: PartialFunction[Effect[_], State => EventStream[(Output, Effect[_])]] = {
      case FlatMap(EmitOutput(fn), b: (Output => Effect[_])) =>
        state: State =>
          EventStream
            .fromTry(Try(fn(state)), emitOnce = true)
            .map(a => a -> b(a))
    }
    flatMapEffects
      .collect(select)
      .withCurrentValueOf(state)
      .map2(_ apply _)
      .flatten
  }

  override val output: Observable[Output] = outputStreamAndK.map(_._1)

  private val handledActions = {
    def select: PartialFunction[Effect[_], State => EventStream[Effect[_]]] = {
      case FlatMap(EmitAction(fn: (State => Action)), b: (Action => Effect[_])) =>
        state: State =>
          EventStream
            .fromTry(Try(fn(state)), emitOnce = true)
            .withCurrentValueOf(actionHandler)
            .filter { case (action, handler) => handler.isDefinedAt(action) }
            .map2((action, handler) => action -> handler(action))
            .map2((action, eff) => eff.flatMap(_ => b(action)))
    }
    flatMapEffects
      .collect(select)
      .withCurrentValueOf(state)
      .map2(_ apply _)
      .flatten
  }

  private val handledInputs = {
    def select: PartialFunction[Effect[_], State => EventStream[Effect[_]]] = {
      case FlatMap(EmitInput(fn), b: (Input => Effect[_])) =>
        state: State =>
          EventStream
            .fromTry(Try(fn(state)), emitOnce = true)
            .withCurrentValueOf(inputHandler)
            .filter { case (input, handler) => handler.isDefinedAt(input) }
            .map2((input, handler) => input -> handler(input))
            .map2((input, eff) => eff.flatMap(_ => b(input)))
    }
    flatMapEffects
      .collect(select)
      .withCurrentValueOf(state)
      .map2(_ apply _)
      .flatten
  }

  private val handledPures = {
    def select: PartialFunction[Effect[_], State => EventStream[Effect[_]]] = {
      case FlatMap(Pure(fn), b: Function1[_, Effect[_]]) =>
        state: State =>
          EventStream
            .fromTry(Try(fn(state)), emitOnce = true)
            .map(a => b(a))
    }
    flatMapEffects
      .collect(select)
      .withCurrentValueOf(state)
      .map2(_ apply _)
      .flatten
  }

  private val loopbackEffects: EventStream[Effect[_]] = EventStream.merge(
    handledPures,
    handledActions,
    handledInputs,
    streamedEffects,
    stateStreamAndK.map(_._2),
    actionHandlerStreamAndK.map(_._2),
    inputHandlerStreamAndK.map(_._2),
    outputStreamAndK.map(_._2)
  )

  protected def unmountBinder(e: E): Option[ParentNode.Base] = e.maybeParent

  override val mod: Mod[E] = inContext { el =>
    el.amend(
      loopbackEffects --> effects.writer,
      onMountBind(ctx => EventStream.fromValue(EmitAction(_ => Mounted(ctx)), emitOnce = true) --> effects.writer)
    )
  }

}
