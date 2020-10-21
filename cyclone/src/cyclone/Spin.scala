package cyclone

import com.raquo.laminar.api.L._

trait Spin {

  case class Spin[E <: Element, I, S, O] private () extends Flows[E, I, S, O] with Implicits {

    def apply(
        state: S,
        flow: Flow[_] = emptyFlow,
        handler: Handler = handleNone
    ): Cyclone[E, I, S, O] = {
      val s = state
      val f = flow
      val h = handler
      new Landspout[E, I, S, O] {
        override protected lazy val initialState: State     = s
        override protected lazy val initialHandler: Handler = h
        override protected val mainFlow: Flow[_]            = f
      }
    }

  }

  case class Apply[E <: Element, I, S, O] private () {
    def spin(fn: Spin[E, I, S, O] => Cyclone[E, I, S, O]): Cyclone[E, I, S, O] =
      fn(Spin[E, I, S, O]())
  }

  def apply[E <: Element, I, S, O]: Apply[E, I, S, O] = Apply()

}
