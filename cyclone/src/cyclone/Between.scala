package cyclone

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

trait Between {

  def between[S, LI, LO, RI, RO](
      left: Cyclone.IO[LI, LO],
      right: Cyclone.IO[RI, RO]
  )(
      fn: Cyclone.Spin[Either[LO, RO], S, Either[LI, RI]] => Cyclone[Either[LO, RO], S, Either[LI, RI]]
  ): Cyclone[Either[LO, RO], S, Either[LI, RI]] =
    bindBetween(left, fn(Cyclone.Spin()), right)

  private def bindBetween[S, LI, LO, RI, RO](
      left: Cyclone.IO[LI, LO],
      center: Cyclone[Either[LO, RO], S, Either[LI, RI]],
      right: Cyclone.IO[RI, RO]
  ): Cyclone[Either[LO, RO], S, Either[LI, RI]] =
    new Cyclone[Either[LO, RO], S, Either[LI, RI]] {
      override val input: WriteBus[In]      = center.input
      override val state: Signal[State]        = center.state
      override val output: EventStream[Out] = center.output

      override def bind[E <: Element](active: Signal[Boolean]): Binder[E] =
        ReactiveElement.bindCallback(_) { ctx =>
          ctx.thisNode.amend(
            center.bind(),
            active.bindBus(left.output.map(Left(_)), input),
            active.bindBus(right.output.map(Right(_)), input),
            active.bindBus(output.collect { case Left(v)  => v }, left.input),
            active.bindBus(output.collect { case Right(v) => v }, right.input)
          )
        }
    }

}
