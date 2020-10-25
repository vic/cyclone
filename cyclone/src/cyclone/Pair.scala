package cyclone

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

trait Pair {

  def paired[E <: Element, I, S, O](
      other: Cyclone.IO[O, I]
  )(fn: Cyclone.Spin[E, I, S, O] => Cyclone[E, I, S, O]): Cyclone[E, I, S, O] =
    bindPair(fn(Cyclone.Spin()), other)

  private def bindPair[E <: Element, I, S, O](
      self: Cyclone[E, I, S, O],
      other: Cyclone.IO[O, I]
  ): Cyclone[E, I, S, O] =
    new Cyclone[E, I, S, O] {
      override val input: WriteBus[Input]      = self.input
      override val state: Signal[State]        = self.state
      override val output: EventStream[Output] = self.output

      override def bind(active: Signal[Boolean]): Binder[El] =
        ReactiveElement.bindCallback(_) { ctx =>
          ctx.thisNode.amend(
            self.bind(),
            active.bindBus(other.output, self.input),
            active.bindBus(self.output, other.input)
          )
        }
    }

}
