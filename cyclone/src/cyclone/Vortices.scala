package cyclone

import com.raquo.laminar.api.L._

trait Waterspout[I, S, O] extends Vortex[I, S, O] {
  import Types._
  protected def makeState(updates: EventStream[S]): Signal[S]

  protected def makeInputHandler(
      updates: EventStream[InputHandler[I]]
  ): Signal[InputHandler[I]]

  protected def makeActionHandler(
      updates: EventStream[ActionHandler[A]]
  ): Signal[ActionHandler[A]]

  override lazy val state: Signal[S] = makeState(stateStream)

  override protected lazy val inputHandler: Signal[InputHandler[I]] =
    makeInputHandler(inputHandlerStream)

  override protected lazy val actionHandler: Signal[ActionHandler[A]] =
    makeActionHandler(actionHandlerStream)
}

trait Landspout[I, S, O] extends Vortex[I, S, O] {
  import Effects._
  import Types._

  protected val initialState: State
  protected lazy val initialInputHandler: InputHandler[I] = {
    case _ => noEffect
  }
  protected lazy val initialActionHandler: ActionHandler[A] = {
    case _ => noEffect
  }

  override lazy val state: Signal[S] = stateStream.startWith(initialState)
  override protected lazy val inputHandler: Signal[InputHandler[I]] =
    inputHandlerStream.startWith(initialInputHandler)
  override protected lazy val actionHandler: Signal[ActionHandler[A]] =
    actionHandlerStream.startWith(initialActionHandler)
}
