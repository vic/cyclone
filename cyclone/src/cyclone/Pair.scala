package cyclone

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

trait Pair {

  def paired[I, S, O](
      other: Cyclone.IO[O, I]
  )(fn: Cyclone.Spin[I, S, O] => Cyclone[I, S, O]): Cyclone[I, S, O] =
    bindPair(fn(Cyclone.Spin()), other)

  private def bindPair[I, S, O](
      self: Cyclone[I, S, O],
      other: Cyclone.IO[O, I]
  ): Cyclone[I, S, O] =
    new Cyclone[I, S, O] {
      override val input: WriteBus[In]      = self.input
      override val state: Signal[State]        = self.state
      override val output: EventStream[Out] = self.output

      override def bind[E <: Element](active: Signal[Boolean]): Binder[E] =
        ReactiveElement.bindCallback(_) { ctx =>
          ctx.thisNode.amend(
            self.bind(),
            active.bindBus(other.output, self.input),
            active.bindBus(self.output, other.input)
          )
        }
    }

}
