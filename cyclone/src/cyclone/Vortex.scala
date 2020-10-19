package cyclone

import com.raquo.laminar.api.L._

// Cyclones are circular Airstreams around an stateful Vortex
trait Vortex[E <: Element, I, S, O] extends FlowTypes[E, I, S, O] {
  val input: WriteBus[Input]
  val state: Signal[State]
  val output: EventStream[Output]
  def bind(initialFlow: Flow[_] = EmptyFlow): Binder[El]
}

object Vortex extends Cyclone with Between {
  implicit def toWriteBus[I](cyclone: Vortex[_, I, _, _]): WriteBus[I] =
    cyclone.input

  implicit def toSignal[S](cyclone: Vortex[_, _, S, _]): Signal[S] =
    cyclone.state

  implicit def toEventStream[O](cyclone: Vortex[_, _, _, O]): EventStream[O] =
    cyclone.output
}
