package cyclone

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

trait Between {

  def between[E <: Element, S, LI, LO, RI, RO](
      left: Cyclone[_ <: Element, LI, _, LO],
      right: Cyclone[_ <: Element, RI, _, RO]
  )(
      fn: Cyclone.Spin[E, Either[LO, RO], S, Either[LI, RI]] => Cyclone[E, Either[LO, RO], S, Either[LI, RI]]
  ): Cyclone[E, Either[LO, RO], S, Either[LI, RI]] =
    bindBetween(left, fn(Cyclone.Spin()), right)

  private def bindBetween[E <: Element, S, LI, LO, RI, RO](
      left: Cyclone[_ <: Element, LI, _, LO],
      center: Cyclone[E, Either[LO, RO], S, Either[LI, RI]],
      right: Cyclone[_ <: Element, RI, _, RO]
  ): Cyclone[E, Either[LO, RO], S, Either[LI, RI]] =
    new Cyclone[E, Either[LO, RO], S, Either[LI, RI]] {
      override val input: WriteBus[Input]      = center.input
      override val state: Signal[State]        = center.state
      override val output: EventStream[Output] = center.output

      override def bind(): Binder[El] =
        ReactiveElement.bindCallback(_) { ctx =>
          ctx.thisNode.amend(
            center.bind(),
            left.output.map(Left(_)) --> input,
            right.output.map(Right(_)) --> input,
            output.collect { case Left(v)  => v } --> left.input,
            output.collect { case Right(v) => v } --> right.input
          )
        }
    }

}
