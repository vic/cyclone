package cyclone

import com.raquo.laminar.api.L._

private[cyclone] trait Effects[I, S, O] { self: Types[I, S, O] =>

  protected val noEffect: Effect[Nothing] = NoEffect

  protected def emitAction(fn: State => Action): Effect[Action] =
    EmitAction(fn(_))

  protected def emitAction(action: => Action): Effect[Action] =
    EmitAction(_ => action)

  protected def emitInput(fn: State => Input): Effect[Input] =
    EmitInput(fn(_))

  protected def emitInput(input: => Input): Effect[Input] =
    EmitInput(_ => input)

  protected def emitOutput(fn: State => Output): Effect[Output] =
    EmitOutput(fn(_))

  protected def emitOutput(output: => Output): Effect[Output] =
    EmitOutput(_ => output)

  protected def update(fn: State => State): Effect[(State, State)] =
    UpdateState(fn(_))

  protected def updateInputHandler(fn: InputHandler => InputHandler): Effect[(InputHandler, InputHandler)] =
    UpdateInputHandler(fn(_))

  protected def updateActionHandler(fn: ActionHandler => ActionHandler): Effect[(ActionHandler, ActionHandler)] =
    UpdateActionHandler(fn(_))

  protected def currentState: Effect[State] =
    Pure[State](identity)

  protected def effect[X](fn: State => X): Effect[X] =
    Pure[X](fn(_))

  protected def pure[X](fn: => X): Effect[X] =
    Pure[X](_ => fn)

  protected def stream[X](fn: => EventStream[Effect[X]]): Effect[X] =
    Stream[X](fn)

}
