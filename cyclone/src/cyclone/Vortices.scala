package cyclone

import com.raquo.laminar.api.L._

trait Waterspout[E <: Element, I, S, O] { self: Vortex[E, I, S, O] =>
  protected def makeState(updates: EventStream[S]): Signal[S]

  protected def makeHandler(updates: EventStream[Handler]): Signal[Handler]

  override lazy val state: Signal[S] = makeState(stateStream)

  override protected lazy val inputHandler: Signal[Handler] =
    makeHandler(inputHandlerStream)
}

trait Landspout[E <: Element, I, S, O] extends Vortex[E, I, S, O] {
  protected val initialState: State
  protected lazy val initialHandler: Handler = emptyHandler

  override lazy val state: Signal[State] = stateStream.startWith(initialState)
  override protected lazy val inputHandler: Signal[Handler] =
    inputHandlerStream.startWith(initialHandler)
}
