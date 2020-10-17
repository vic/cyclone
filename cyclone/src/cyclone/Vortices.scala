package cyclone

import com.raquo.laminar.api.L._

trait Waterspout[I, S, O] extends Vortex[I, S, O] {
  protected def makeState(updates: EventStream[S]): Signal[S]
  protected def makeInputHandler(updates: EventStream[InputHandler]): Signal[InputHandler]
  protected def makeActionHandler(updates: EventStream[ActionHandler]): Signal[ActionHandler]

  override val state: Signal[S]                               = makeState(stateStream)
  override protected val inputHandler: Signal[InputHandler]   = makeInputHandler(inputHandlerStream)
  override protected val actionHandler: Signal[ActionHandler] = makeActionHandler(actionHandlerStream)
}

trait Landspout[I, S, O] extends Vortex[I, S, O] {
  protected val initialState: State
  protected val initialInputHandler: InputHandler   = { case _ => noEffect }
  protected val initialActionHandler: ActionHandler = { case _ => noEffect }

  override val state: Signal[S]                               = stateStream.startWith(initialState)
  override protected val inputHandler: Signal[InputHandler]   = inputHandlerStream.startWith(initialInputHandler)
  override protected val actionHandler: Signal[ActionHandler] = actionHandlerStream.startWith(initialActionHandler)
}
