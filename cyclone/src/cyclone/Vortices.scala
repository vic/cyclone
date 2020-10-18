package cyclone

import com.raquo.laminar.api.L._

trait Waterspout[I, S, O] extends Vortex[I, S, O] {
  protected def makeState(updates: EventStream[S]): Signal[S]
  protected def makeInputHandler(updates: EventStream[InputHandler]): Signal[InputHandler]
  protected def makeActionHandler(updates: EventStream[ActionHandler]): Signal[ActionHandler]

  override lazy val state: Signal[S]                               = makeState(stateStream)
  override protected lazy val inputHandler: Signal[InputHandler]   = makeInputHandler(inputHandlerStream)
  override protected lazy val actionHandler: Signal[ActionHandler] = makeActionHandler(actionHandlerStream)
}

trait Landspout[I, S, O] extends Vortex[I, S, O] {
  protected val initialState: State
  protected lazy val initialInputHandler: InputHandler   = { case _ => noEffect }
  protected lazy val initialActionHandler: ActionHandler = { case _ => noEffect }

  override lazy val state: Signal[S]                               = stateStream.startWith(initialState)
  override protected lazy val inputHandler: Signal[InputHandler]   = inputHandlerStream.startWith(initialInputHandler)
  override protected lazy val actionHandler: Signal[ActionHandler] = actionHandlerStream.startWith(initialActionHandler)
}
