package cyclone

import com.raquo.laminar.api.L._

trait Cyclone {

  case class Cycle[E <: Element, I, S, O] private () extends Flows[E, I, S, O] with Implicits {

    def apply(
        initState: S,
        initFlow: Flow[_] = emptyFlow
    )(inHandler: Handler = handleNone): Vortex[E, I, S, O] =
      new Landspout[E, I, S, O] {
        override protected lazy val initialState: State     = initState
        override protected lazy val initialHandler: Handler = inHandler
        override protected val initialFlow: Flow[_]         = initFlow
      }

  }

  case class Apply[E <: Element, I, S, O] private () {
    def build(fn: Cycle[E, I, S, O] => Vortex[E, I, S, O]): Vortex[E, I, S, O] =
      fn(Cycle[E, I, S, O]())
  }

  def apply[E <: Element, I, S, O]: Apply[E, I, S, O] = Apply()

}
