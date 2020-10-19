package cyclone

import com.raquo.laminar.api.L._

// Cyclones are circular Airstreams around an stateful Vortex
trait Cyclone[E <: Element, I, S, O] extends FlowTypes[E, I, S, O] {
  val input: WriteBus[Input]
  val state: Signal[State]
  val output: EventStream[Output]
  def bind(initialFlow: Flow[_] = EmptyFlow): Binder[El]
}

object Cyclone {
  implicit def toWriteBus[I](cyclone: Cyclone[_, I, _, _]): WriteBus[I] =
    cyclone.input

  implicit def toEventStream[O](cyclone: Cyclone[_, _, _, O]): EventStream[O] =
    cyclone.output

  case class Whirl[E <: Element, I, S, O] private[Cyclone] () extends Flows[E, I, S, O] with Implicits {

    def create(
        initState: S,
        inHandler: Handler = emptyHandler,
        startFlow: Flow[_] = emptyFlow
    ): Cyclone[E, I, S, O] =
      new Landspout[E, I, S, O] {
        override protected lazy val initialState: State     = initState
        override protected lazy val initialHandler: Handler = inHandler
        override protected val initialFlow: Flow[_]         = startFlow
      }

  }

  case class Apply[E <: Element, I, S, O] private[Cyclone] () {
    def build(fn: Whirl[E, I, S, O] => Cyclone[E, I, S, O]): Cyclone[E, I, S, O] =
      fn(Whirl[E, I, S, O]())
  }

  def apply[E <: Element, I, S, O]: Apply[E, I, S, O] = Apply()
//  final class Between private[Cyclone] (left: Cyclone[_, _, _, _], right: Cyclone[_, _, _, _]) {
//    class Apply(flows: Flows[_, _, _, _]) {
//      def apply(initState: flows.State, initHandler: flows.Handler): Cyclone[E, I, S, O] =
//        new Landspout {
//          override val flow = flows
//          import flow._
//          override protected val initialState: State     = initState
//          override protected val initialHandler: Handler = initHandler
//          override def bind(initialFlow: Flow[_]): Binder[El] = {
//            ReactiveElement.bindCallback[El](_) { ctx =>
//              ctx.thisNode.amend(
//                super.bind(initialFlow),
//                left.output.map(Left(_)) --> input,
//                right.output.map(Right(_)) --> input,
//                output.collect { case Left(v)  => v } --> left.input,
//                output.collect { case Right(v) => v } --> right.input
//              )
//            }
//          }
//        }
//    }
//
//    def apply[E <: Element, S]: Apply =
//      new Apply(
//        new Flows[E, Either[left.flow.Input, right.flow.Input], S, Either[left.flow.Output, right.flow.Output]] {}
//      )
//  }

}
