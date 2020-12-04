package cyclone

import com.raquo.laminar.api.L._

// Cyclones are circular Airstreams around an stateful Vortex
trait Cyclone[I, S, O] extends FlowTypes[I, S, O] {
  val input: WriteBus[In]
  val state: Signal[State]
  val output: EventStream[Out]

  def bind[E <: Element](active: Signal[Boolean]): Binder[E]
  final def bind[E <: Element](): Binder[E] = bind[E](EventStream.empty.startWith(true))
}

object Cyclone extends Spin with Pair with Between with Channel {
  type IO[I, O] = Cyclone[I, _, O]

  implicit def toWriteBus[I](cyclone: Cyclone[I, _, _]): WriteBus[I] =
    cyclone.input

  implicit def toSignal[S](cyclone: Cyclone[_, S, _]): Signal[S] =
    cyclone.state

  implicit def toEventStream[O](cyclone: Cyclone[_, _, O]): EventStream[O] =
    cyclone.output
}
