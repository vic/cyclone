package cyclone

import com.raquo.laminar.api.L._

trait Flows[E <: Element, I, S, O] extends FlowTypes[E, I, S, O] with ElementFlows[E, I, S, O] {

  final val emptyFlow: Flow[Nothing] = EmptyFlow

  def staticSignal[X](x: X): Signal[X]  = EventStream.empty.startWith(x)
  final val trueSignal: Signal[Boolean] = staticSignal(true)

  final val handleNone: Handler                     = { case _ => EmptyFlow }
  def handleAll(fn: I => Flow[_]): Handler          = { case i => fn(i) }
  def handleSome(h: Handler, hs: Handler*): Handler = hs.foldLeft(h)(_ orElse _)

  def emitInput(input: => I): Flow[I] =
    EmitInput(() => input)

  def emitOutput(output: => O): Flow[O] =
    EmitOutput(() => output)

  def pure[X](fn: => X): Flow[X] =
    Pure(() => fn)

  final val unit: Flow[Unit] = pure(())

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

  def intoStream[X](xs: => Flow[X]): Flow[EventStream[X]] =
    for {
      child <- spawn[X, Unit, X] { cycle =>
        cycle((), flow = xs.map(cycle.emitInput(_)), handler = cycle.emitOutput(_))
      }
    } yield child.output

  def fromStream[X](fn: => EventStream[X]): Flow[X] =
    fromFlowStream(fn.map(pure(_)))

  def fromFlowStream[X](fn: => EventStream[Flow[X]]): Flow[X] =
    FromStream[X](() => fn)

  def fromCallback[X](cb: => ((X => Unit) => Unit)): Flow[X] =
    FromStream[X] { () =>
      val bus = new EventBus[X]
      cb(bus.writer.onNext)
      bus.events.map(pure[X](_))
    }

  def spin[WE <: Element, WI, WS, WO](
      w: Cyclone.Spin[WE, WI, WS, WO] => Cyclone[WE, WI, WS, WO]
  ): Flow[Cyclone[WE, WI, WS, WO]] =
    pure(Cyclone[WE, WI, WS, WO].spin(w))

  def spawn[CI, CS, CO](
      c: Cyclone.Spin[E, CI, CS, CO] => Cyclone[E, CI, CS, CO]
  ): Flow[Cyclone[E, CI, CS, CO]] =
    for {
      cyclone <- spin(c)
      _       <- bind(cyclone.bind())
    } yield cyclone

  private def onlyFirst[X](ev: EventStream[X]): EventStream[X] = {
    var first = true
    ev.collect {
      case x if first =>
        first = false
        x
    }
  }

  def sendAll[X](events: => EventStream[X], to: => WriteBus[X], active: => Signal[Boolean] = trueSignal): Flow[Unit] =
    bind(events.withCurrentValueOf(active).collect { case (x, true) => x } --> to).mapTo(())

  def sendOne[X](events: => EventStream[X], to: => WriteBus[X]): Flow[Unit] =
    sendAll(events.compose(onlyFirst), to)

  def tell(to: Cyclone[_, _, _, _])(i: => to.Input): Flow[Unit] =
    sendOne(EventStream.fromValue(i, emitOnce = true), to.input)

  // TODO: Ask, Subscribe

}
