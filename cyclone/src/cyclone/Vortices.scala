package cyclone

import com.raquo.laminar.api.L._

trait Waterspout[E <: Element, I, S, O] extends Vortex[E, I, S, O] {
  protected def makeState(updates: EventStream[S]): Signal[S]

  protected def makeHandler(updates: EventStream[Handler]): Signal[Handler]

  override lazy val state: Signal[S] = makeState(stateChanges)

  override protected lazy val inputHandler: Signal[Handler] =
    makeHandler(handlerChanges)
}

trait Landspout[E <: Element, I, S, O] extends Vortex[E, I, S, O] with Flows[E, I, S, O] {
  protected val initialState: State
  protected lazy val initialHandler: Handler = handleNone

  override lazy val state: Signal[State] = stateChanges.startWith(initialState)
  override protected lazy val inputHandler: Signal[Handler] =
    handlerChanges.startWith(initialHandler)
}
