package cyclone

import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

trait Flows {

  val noEffect: Flow[Nothing] = EmptyFlow

  def emptyInputHandler[I]: InputHandler[I] = { case _ => EmptyFlow }

  def emitInput[S, I](fn: S => I): Flow[I] =
    EmitInput[S, I](fn)

  def emitInput[S, I](input: => I): Flow[I] =
    EmitInput[S, I](_ => input)

  def emitOutput[S, O](fn: S => O): Flow[O] =
    EmitOutput[S, O](fn)

  def emitOutput[S, O](output: => O): Flow[O] =
    EmitOutput[S, O](_ => output)

  def current[S]: Flow[S] =
    Pure[S, S](identity)

  def update[S](fn: S => S): Flow[(S, S)] =
    UpdateState[S](fn)

  def updateTo[S](state: => S): Flow[(S, S)] =
    UpdateState[S](_ => state)

  def updateHandler[I](fn: InputHandler[I] => InputHandler[I]): Flow[(InputHandler[I], InputHandler[I])] =
    UpdateInputHandler[I](fn)

  def updateHandlerTo[I](fn: => InputHandler[I]): Flow[(InputHandler[I], InputHandler[I])] =
    UpdateInputHandler[I](_ => fn)

  def effect[S, X](fn: S => X): Flow[X] =
    Pure[S, X](fn)

  def pure[S, X](fn: => X): Flow[X] =
    Pure[S, X](_ => fn)

  def stream[X](xs: Flow[X]): Flow[EventStream[X]] =
    for {
      _ <- pure(())
      bus = new EventBus[X]
      x <- xs
      _ = bus.writer.onNext(x)
    } yield bus.events

  def stream[S, X](fn: S => EventStream[Flow[X]]): Flow[X] =
    Stream[S, X](fn)

  def stream[S, X](fn: => EventStream[Flow[X]]): Flow[X] =
    Stream[S, X](_ => fn)

  def callback[S, X](fn: => ((X => Unit) => Unit)): Flow[X] =
    callback[S, X]((_: S) => fn)

  def callback[S, X](fn: S => ((X => Unit) => Unit)): Flow[X] =
    Stream[S, X] { s =>
      val cb  = fn(s)
      val bus = new EventBus[X]
      cb(bus.writer.onNext)
      bus.events.map(pure[S, X](_))
    }

  def context[E <: Element, X](fn: E => X): Flow[X] =
    InContext[E, X](fn)

  def element[E <: Element]: Flow[E] = context[E, E](identity)

  def bind[E <: Element](binder: Binder[E]): Flow[Unit] =
    element[E].map[Unit](_.amend(binder))

  private def onlyFirst[X](ev: EventStream[X]): EventStream[X] = {
    var first = true
    ev.collect {
      case x if first =>
        first = false
        x
    }
  }

  def send[S, X](events: EventStream[X], to: WriteBus[X])(
      stopAt: EventStream[X] => EventStream[Any] = identity[EventStream[X]](_)
  ): Flow[Unit] =
    for {
      sub <- context[Element, DynamicSubscription](ReactiveElement.bindBus(_, events)(to))
      _   <- stream(events.compose(stopAt).compose(onlyFirst).map(pure(_)))
    } yield sub.kill()

//  def subscribe[I, O](out: WriteBus[O], in: EventStream[I])(active: Signal[Boolean]): Effect[I] = {
//    for {
//      el <- element[Element]
//      dynSub = DynamicSubscription.subscribeBus(dynOwner, )
//    } yield ()
//  }
//
//
//  def ask[S, X, I, O](cyclone: Cyclone[I, _, O])(rq: S => I)(rs: PartialFunction[(I, O), X]): Effect[X] = {
//    for {
//      q <- effect(rq)
//      _ <- pure[S, Unit](cyclone.addSource())
//      r <- stream(cyclone.output.map(q -> _).collect(rs).map(pure[S, X](_)))
//    } yield r
//  }

//  def ask[S, O, I, X](fwd: S => O)(bwd: PartialFunction[I, X]): Effect[X] = {
//    for {
//      (prevHandler, _) <- updateHandlerTo(emptyInputHandler)
//      (_, newHandler) <- updateHandlerTo {
//        case i if bwd.isDefinedAt(i) =>
//      }
//      o <- emitOutput(fwd)
//    } yield ()
//  }

}
