package cyclone

import com.raquo.laminar.api.L._

// Cyclones are circular Airstreams around an stateful Vortex
trait Cyclone[I, S, O] {
  val input: Observer[I]
  val state: Signal[S]
  val output: Observable[O]
  val mod: Mod[Element]
}

object Cyclone {
  import Types._

  implicit def toSignal[S](cyclone: Cyclone[_, S, _]): Signal[S] =
    cyclone.state

  implicit def toObserver[I](cyclone: Cyclone[I, _, _]): Observer[I] =
    cyclone.input

  implicit def toObservable[O](cyclone: Cyclone[_, _, O]): Observable[O] =
    cyclone.output

  def apply[I, S, O](initState: S, inHandler: InputHandler[I]): Cyclone[I, S, O] =
    new Landspout[I, S, O] {
      override protected val initialState: State                       = initState
      override protected lazy val initialInputHandler: InputHandler[I] = inHandler
    }

}
