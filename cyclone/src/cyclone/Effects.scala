package cyclone

import com.raquo.laminar.api.L._

object Effects {
  import Types._

  val noEffect: Effect[Nothing] = NoEffect

  def emitAction[State, Action](fn: State => Action): Effect[Action] =
    EmitAction(fn(_))

  def emitAction[State, Action](action: => Action): Effect[Action] =
    EmitAction[State, Action](_ => action)

  def emitInput[State, Input](fn: State => Input): Effect[Input] =
    EmitInput(fn(_))

  def emitInput[State, Input](input: => Input): Effect[Input] =
    EmitInput[State, Input](_ => input)

  def emitOutput[State, Output](fn: State => Output): Effect[Output] =
    EmitOutput(fn(_))

  def emitOutput[State, Output](output: => Output): Effect[Output] =
    EmitOutput[State, Output](_ => output)

  def update[State](fn: State => State): Effect[(State, State)] =
    UpdateState[State](fn)

  def update[State](state: State): Effect[(State, State)] =
    UpdateState[State](_ => state)

  def updateInputHandler[I](fn: InputHandler[I] => InputHandler[I]): Effect[(InputHandler[I], InputHandler[I])] =
    UpdateInputHandler(fn)

  def updateActionHandler[A](
      fn: ActionHandler[A] => ActionHandler[A]
  ): Effect[(ActionHandler[A], ActionHandler[A])] =
    UpdateActionHandler(fn)

  def currentState[State]: Effect[State] =
    Pure[State, State](identity)

  def effect[State, X](fn: State => X): Effect[X] =
    Pure[State, X](fn)

  def pure[State, X](fn: => X): Effect[X] =
    Pure[State, X](_ => fn)

  def stream[State, X](fn: State => EventStream[Effect[X]]): Effect[X] =
    Stream[State, X](fn)

  def stream[State, X](fn: => EventStream[Effect[X]]): Effect[X] =
    Stream[State, X](_ => fn)

}
