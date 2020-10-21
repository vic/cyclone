package cyclone

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

trait Between {

  case class Between[E <: Element, S, LI, LO, RI, RO] private (
      left: Cyclone[_ <: Element, LI, _, LO],
      right: Cyclone[_ <: Element, RI, _, RO]
  ) extends Flows[E, Either[LO, RO], S, Either[LI, RI]]
      with Implicits {

    def apply(
        state: S,
        flow: Flow[_] = emptyFlow,
        handler: Handler = handleNone
    ): Cyclone[E, Either[LO, RO], S, Either[LI, RI]] = {
      val s = state
      val f = flow
      val h = handler
      new Landspout[E, Either[LO, RO], S, Either[LI, RI]] {
        override protected lazy val initialState: State     = s
        override protected lazy val initialHandler: Handler = h
        override protected val mainFlow: Flow[_]            = f

        override def bind(): Binder[E] =
          ReactiveElement.bindCallback(_) { ctx =>
            ctx.thisNode.amend(
              super.bind(),
              left.output.map(Left(_)) --> input,
              right.output.map(Right(_)) --> input,
              output.collect { case Left(v)  => v } --> left.input,
              output.collect { case Right(v) => v } --> right.input
            )
          }
      }
    }

  }

  def between[E <: Element, S, LI, LO, RI, RO](
      left: Cyclone[_ <: Element, LI, _, LO],
      right: Cyclone[_ <: Element, RI, _, RO]
  )(
      fn: Between[E, S, LI, LO, RI, RO] => Cyclone[
        E,
        Either[LO, RO],
        S,
        Either[LI, RI]
      ]
  ): Cyclone[E, Either[LO, RO], S, Either[LI, RI]] =
    fn(Between[E, S, LI, LO, RI, RO](left, right))

}
