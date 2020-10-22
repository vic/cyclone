package cyclone

import com.raquo.laminar.api.L._

// Cyclones are circular Airstreams around an stateful Vortex
trait Cyclone[E <: Element, I, S, O] extends FlowTypes[E, I, S, O] {
  val input: WriteBus[Input]
  val state: Signal[State]
  val output: EventStream[Output]
  def bind(): Binder[El]
}

object Cyclone extends Spin with Pair with Between with Channel {
  type IO[I, O] = Cyclone[_ <: Element, I, _, O]

  implicit def toWriteBus[I](cyclone: Cyclone[_, I, _, _]): WriteBus[I] =
    cyclone.input

  implicit def toSignal[S](cyclone: Cyclone[_, _, S, _]): Signal[S] =
    cyclone.state

  implicit def toEventStream[O](cyclone: Cyclone[_, _, _, O]): EventStream[O] =
    cyclone.output
}
