package cyclone

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

trait Between {

  def between[E <: Element, S, LI, LO, RI, RO](
      left: Cyclone.IO[LI, LO],
      right: Cyclone.IO[RI, RO]
  )(
      fn: Cyclone.Spin[E, Either[LO, RO], S, Either[LI, RI]] => Cyclone[E, Either[LO, RO], S, Either[LI, RI]]
  ): Cyclone[E, Either[LO, RO], S, Either[LI, RI]] =
    bindBetween(left, fn(Cyclone.Spin()), right)

  private def bindBetween[E <: Element, S, LI, LO, RI, RO](
      left: Cyclone.IO[LI, LO],
      center: Cyclone[E, Either[LO, RO], S, Either[LI, RI]],
      right: Cyclone.IO[RI, RO]
  ): Cyclone[E, Either[LO, RO], S, Either[LI, RI]] =
    new Cyclone[E, Either[LO, RO], S, Either[LI, RI]] {
      override val input: WriteBus[Input]      = center.input
      override val state: Signal[State]        = center.state
      override val output: EventStream[Output] = center.output

      override def bind(active: Signal[Boolean]): Binder[El] =
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
