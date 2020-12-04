package cyclone

import com.raquo.domtypes.generic.builders.Tag
import com.raquo.laminar.api.L._
import com.raquo.laminar.emitter.EventPropTransformation
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom

import scala.util.Try

trait ElementFlows[I, S, O] { self: Flows[I, S, O] =>

  def context[E <: Element]: Flow[MountContext[E]] =
    MountedContext[E]()

  def element[E <: Element]: Flow[E] =
    context[E].map(_.thisNode)

  def bind[E <: Element](binder: => Binder[E]): Flow[E] =
    element[E].map(_.amend(binder))

  def bindBusOn[E <: Element, X](active: Signal[Boolean])(in: EventStream[X], out: WriteBus[X]): Flow[E] =
    element[E].map(_.amend(active.bindBus(in, out)))

  def bindObserverOn[E <: Element, X](active: Signal[Boolean])(in: Observable[X], out: Observer[X]): Flow[E] =
    element[E].map(_.amend(active.bindObserver(in, out)))

  def bindFnOn[E <: Element, X](active: Signal[Boolean])(in: Observable[X], onNext: X => Unit): Flow[E] =
    element[E].map(_.amend(active.bindFn(in, onNext)))

  private def asReactiveElement[Ref <: dom.Element](el: Ref): ReactiveElement[Ref] = {
    new ReactiveElement[Ref] {
      override val tag: Tag[ReactiveElement[Ref]] = new Tag(el.tagName, false)
      override val ref: Ref                       = el
    }
  }

  def elementById[Ref <: dom.Element](id: String): Flow[ReactiveElement[Ref]] =
    value {
      val el = dom.document.getElementById(id)
      asReactiveElement[Ref](el.asInstanceOf[Ref])
    }

  def elementBySelector[Ref <: dom.Element](selector: String): Flow[ReactiveElement[Ref]] =
    value {
      val el = dom.document.querySelector(selector)
      asReactiveElement[Ref](el.asInstanceOf[Ref])
    }

  def elementChildBySelector[E <: Element, Ref <: dom.Element](selector: String): Flow[ReactiveElement[Ref]] =
    element[E].map { parent =>
      val el = parent.ref.querySelector(selector)
      asReactiveElement[Ref](el.asInstanceOf[Ref])
    }

  type EventInContext[EL <: Element, X, Y] = EL => EventStream[X] => EventStream[Y]
  object EventInContext {
    def identity[El <: Element, X]: EventInContext[El, X, X] = { el: El => ev => ev }
  }

  def events[E <: Element, Ev <: dom.Event, V](t: EventPropTransformation[Ev, V]): Flow[EventStream[V]] =
    eventsOf(element[E], t, EventInContext.identity[E, V])

  def events[E <: Element, Ev <: dom.Event, V, R](
      t: EventPropTransformation[Ev, V],
      inContext: EventInContext[E, V, R]
  ): Flow[EventStream[R]] =
    eventsOf(element[E], t, inContext)

  def eventsOf[E <: Element, Ev <: dom.Event, V](
      element: Flow[E],
      t: EventPropTransformation[Ev, V]
  ): Flow[EventStream[V]] =
    eventsOf(element, t, EventInContext.identity[E, V])

  def eventsOf[E <: Element, Ev <: dom.Event, V, R](
      element: Flow[E],
      t: EventPropTransformation[Ev, V],
      inContext: EventInContext[E, V, R]
  ): Flow[EventStream[R]] =
    element.map(e => e.events(t).compose(inContext(e)))

  def fromEvents[E <: Element, Ev <: dom.Event, V](t: EventPropTransformation[Ev, V]): Flow[V] =
    eventsOf(element[E], t, EventInContext.identity[E, V]).flatMap(fromStream(_))

  def fromEvents[E <: Element, Ev <: dom.Event, V, R](
      t: EventPropTransformation[Ev, V],
      inContext: EventInContext[E, V, R]
  ): Flow[R] =
    eventsOf(element[E], t, inContext).flatMap(fromStream(_))

  def fromEventsOf[E <: Element, Ev <: dom.Event, V](
      element: Flow[E],
      t: EventPropTransformation[Ev, V]
  ): Flow[V] =
    eventsOf(element, t, EventInContext.identity[E, V]).flatMap(fromStream(_))

  def fromEventsOf[E <: Element, Ev <: dom.Event, V, R](
      element: Flow[E],
      t: EventPropTransformation[Ev, V],
      inContext: EventInContext[E, V, R]
  ): Flow[R] =
    eventsOf(element, t, inContext).flatMap(fromStream(_))

}
