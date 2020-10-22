package cyclone

import com.raquo.laminar.api.L._

trait Spin {

  case class Spin[E <: Element, I, S, O] private () extends Flows[E, I, S, O] with Implicits {

    def apply()(implicit ev: Unit =:= S): Cyclone[E, I, S, O] = apply(state = ())

    def apply(state: S): Cyclone[E, I, S, O] = apply(state, handleNone, emptyFlow)

    def apply(handler: (I => Flow[_]))(implicit ev: Unit =:= S): Cyclone[E, I, S, O] =
      apply(state = (), handler)

    def apply(state: S, handler: (I => Flow[_])): Cyclone[E, I, S, O] =
      apply(state, handleAll(handler), emptyFlow)

    def apply(handler: Handler)(implicit ev: Unit =:= S): Cyclone[E, I, S, O] =
      apply(state = (), handler)

    def apply(state: S, handler: Handler): Cyclone[E, I, S, O] =
      apply(state, handler, emptyFlow)

    def apply(mainFlow: Flow[_])(implicit ev: Unit =:= S): Cyclone[E, I, S, O] =
      apply(state = (), mainFlow)

    def apply(state: S, mainFlow: Flow[_]): Cyclone[E, I, S, O] =
      apply(state, handleNone, mainFlow)

    def apply(state: S, handler: (I => Flow[_]), mainFlow: Flow[_]): Cyclone[E, I, S, O] =
      apply(state, handleAll(handler), mainFlow)

    def apply(
        state: S,
        handler: Handler,
        mainFlow: Flow[_]
    ): Cyclone[E, I, S, O] = {
      val s = state
      val f = mainFlow
      val h = handler
      new Landspout[E, I, S, O] {
        override protected lazy val initialState: State     = s
        override protected lazy val initialHandler: Handler = h
        override protected val mainFlow: Flow[_]            = f
      }
    }

  }

  case class Apply[E <: Element, I, S, O] private () {
    def apply[X](fn: Spin[E, I, S, O] => X): X =
      fn(Spin[E, I, S, O]())
  }

  def spin[E <: Element, I, S, O]: Apply[E, I, S, O] = Apply()

}
