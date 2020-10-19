package cyclone

import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L._
import Types._

trait Flows[E <: Element, I, S, O] extends Types[E, I, S, O] {

  final val emptyFlow: Flow[Nothing] = EmptyFlow
  final val trueSignal               = EventStream.empty.startWith(true)
  final val emptyHandler: Handler    = { case _ => EmptyFlow }

  def handleAll(fn: I => Flow[_]): Handler = { case i => fn(i) }

  def emitInput(input: => I): Flow[I] =
    EmitInput(() => input)

  def emitOutput(output: => O): Flow[O] =
    EmitOutput(() => output)

  def pure[X](fn: => X): Flow[X] =
    Pure(() => fn)

  def current: Flow[S] =
    update(identity).map(_._2)

  def update(fn: S => S): Flow[(S, S)] =
    UpdateState(fn)

  def updateTo(state: => S): Flow[(S, S)] =
    UpdateState((_: S) => state)

  def updateHandler(fn: Handler => Handler): Flow[(Handler, Handler)] =
    UpdateHandler(fn)

  def updateHandlerTo(fn: => Handler): Flow[(Handler, Handler)] =
    UpdateHandler((_: Handler) => fn)

  def intoStream[X](xs: Flow[X]): Flow[EventStream[X]] =
    for {
      child <- spawn {
        new Vortex[E, X, Unit, X] {
          override val state: Signal[Unit] = EventStream.empty.startWith(())
          override protected val inputHandler: Signal[Handler] =
            EventStream.empty.startWith { case x => emitOutput(x) }
        }
      }(xs)
    } yield child.output

  def fromStream[X](fn: => EventStream[X]): Flow[X] =
    fromFlowStream(fn.map(pure(_)))

  def fromFlowStream[X](fn: => EventStream[Flow[X]]): Flow[X] =
    FromStream[X](() => fn)

  def callback[X](cb: => ((X => Unit) => Unit)): Flow[X] =
    FromStream[X] { () =>
      val bus = new EventBus[X]
      cb(bus.writer.onNext)
      bus.events.map(pure[X](_))
    }

  def context[X](fn: E => X): Flow[X] =
    InContext[E, X](fn)

  def element: Flow[E] = context[E](identity)

  def bind(binder: Binder[E]): Flow[E] =
    element.map(_.amend(binder))

  def spawn[CI, CS, CO](c: Cyclone[E, CI, CS, CO])(initialFlow: Flow[_] = EmptyFlow): Flow[c.type] =
    bind(c.bind(initialFlow)).mapTo(c)

  private def onlyFirst[X](ev: EventStream[X]): EventStream[X] = {
    var first = true
    ev.collect {
      case x if first =>
        first = false
        x
    }
  }

  def sendAll[X](events: EventStream[X], to: WriteBus[X], active: Signal[Boolean] = trueSignal): Flow[Unit] =
    bind(events.withCurrentValueOf(active).collect { case (x, true) => x } --> to).mapTo(())

  def sendOne[X](events: EventStream[X], to: WriteBus[X]): Flow[Unit] =
    sendAll(events.compose(onlyFirst), to)

  def tell(to: Cyclone[_, _, _, _])(i: to.Input): Flow[Unit] =
    sendOne(EventStream.fromValue(i, emitOnce = true), to.input)

  // TODO: Ask, Subscribe

}
