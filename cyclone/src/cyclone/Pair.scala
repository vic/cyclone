package cyclone

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

trait Pair {

  def paired[E <: Element, I, S, O](
      other: Cyclone[_ <: Element, O, _, I]
  )(fn: Cyclone.Spin[E, I, S, O] => Cyclone[E, I, S, O]): Cyclone[E, I, S, O] =
    bindPair(fn(Cyclone.Spin()), other)

  private def bindPair[E <: Element, I, S, O](
      self: Cyclone[E, I, S, O],
      other: Cyclone[_ <: Element, O, _, I]
  ): Cyclone[E, I, S, O] =
    new Cyclone[E, I, S, O] {
      override val input: WriteBus[Input]      = self.input
      override val state: Signal[State]        = self.state
      override val output: EventStream[Output] = self.output

      override def bind(): Binder[El] =
        ReactiveElement.bindCallback(_) { ctx =>
          ctx.thisNode.amend(
            self.bind(),
            other.output --> self.input,
            self.output --> other.input
          )
        }
    }

}
