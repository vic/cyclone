package cyclone

import com.raquo.laminar.api.L._

// Cyclones are circular Airstreams around an stateful Vortex
trait Cyclone[I, S, O] {
  val input: WriteBus[I]
  val state: Signal[S]
  val output: EventStream[O]

  def bind[E <: Element](initialEffect: Flow[_] = noEffect): Mod[E]
}

object Cyclone {

  implicit def toWriteBus[I](cyclone: Cyclone[I, _, _]): WriteBus[I] =
    cyclone.input

  implicit def toEventStream[O](cyclone: Cyclone[_, _, O]): EventStream[O] =
    cyclone.output

  def apply[I, S, O](initState: S, inHandler: InputHandler[I]): Cyclone[I, S, O] =
    new Landspout[I, S, O] {
      override protected val initialState: S                           = initState
      override protected lazy val initialInputHandler: InputHandler[I] = inHandler
    }

  def between[LI, LO, RI, RO, S, A](left: Cyclone[LI, _, LO], right: Cyclone[RI, _, RO])(
      initState: S,
      inHandler: InputHandler[Either[LO, RO]] = emptyInputHandler[Either[LO, RO]]
  ): Cyclone[Either[LO, RO], S, Either[LI, RI]] =
    new Landspout[Either[LO, RO], S, Either[LI, RI]] {
      override protected val initialState: S                                        = initState
      override protected lazy val initialInputHandler: InputHandler[Either[LO, RO]] = inHandler

      override def bind[E <: Element](initialEffect: Flow[_]): Mod[E] =
        inContext { el =>
          el.amend(
            super.bind(initialEffect),
            left.output.map(Left(_)) --> input,
            right.output.map(Right(_)) --> input,
            output.collect { case Left(v)  => v } --> left.input,
            output.collect { case Right(v) => v } --> right.input
          )
        }
    }

}
