package cyclone

import com.raquo.laminar.api.L._
import cyclone.Types._

trait Waterspout[I, S, O] extends Vortex[I, S, O] {
  protected def makeState(updates: EventStream[S]): Signal[S]

  protected def makeInputHandler(updates: EventStream[InputHandler[I]]): Signal[InputHandler[I]]

  override lazy val state: Signal[S] = makeState(stateStream)

  override protected lazy val inputHandler: Signal[InputHandler[I]] =
    makeInputHandler(inputHandlerStream)

}

trait Landspout[I, S, O] extends Vortex[I, S, O] {
  protected val initialState: S
  protected lazy val initialInputHandler: InputHandler[I] = emptyInputHandler

  override lazy val state: Signal[S] = stateStream.startWith(initialState)
  override protected lazy val inputHandler: Signal[InputHandler[I]] =
    inputHandlerStream.startWith(initialInputHandler)
}
