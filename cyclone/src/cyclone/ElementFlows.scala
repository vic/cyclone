package cyclone

import com.raquo.domtypes.generic.builders.Tag
import com.raquo.laminar.api.L._
import com.raquo.laminar.emitter.EventPropTransformation
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom

import scala.util.Try

trait ElementFlows[E <: Element, I, S, O] { self: Flows[E, I, S, O] =>

  def context: Flow[MountContext[E]] =
    MountedContext[E]()

  def element: Flow[E] =
    context.map(_.thisNode)

  def bind(binder: => Binder[E]): Flow[E] =
    element.map(_.amend(binder))

  def bindBusOn[X](active: Signal[Boolean])(in: EventStream[X], out: WriteBus[X]): Flow[El] =
    element.map(_.amend(active.bindBus(in, out)))

  def bindObserverOn[X](active: Signal[Boolean])(in: Observable[X], out: Observer[X]): Flow[El] =
    element.map(_.amend(active.bindObserver(in, out)))

  def bindFnOn[X](active: Signal[Boolean])(in: Observable[X], onNext: X => Unit): Flow[El] =
    element.map(_.amend(active.bindFn(in, onNext)))

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

  def elementChildBySelector[Ref <: dom.Element](selector: String): Flow[ReactiveElement[Ref]] =
    element.map { parent =>
      val el = parent.ref.querySelector(selector)
      asReactiveElement[Ref](el.asInstanceOf[Ref])
    }

  type EventInContext[EL <: Element, X, Y] = EL => EventStream[X] => EventStream[Y]
  object EventInContext {
    def identity[El <: Element, X]: EventInContext[El, X, X] = { el: El => ev => ev }
  }

  def events[Ev <: dom.Event, V](t: EventPropTransformation[Ev, V]): Flow[EventStream[V]] =
    eventsOf(element, t, EventInContext.identity[E, V])

  def events[Ev <: dom.Event, V, R](
      t: EventPropTransformation[Ev, V],
      inContext: EventInContext[E, V, R]
  ): Flow[EventStream[R]] =
    eventsOf(element, t, inContext)

  def eventsOf[EL <: Element, Ev <: dom.Event, V](
      element: Flow[EL],
      t: EventPropTransformation[Ev, V]
  ): Flow[EventStream[V]] =
    eventsOf(element, t, EventInContext.identity[EL, V])

  def eventsOf[EL <: Element, Ev <: dom.Event, V, R](
      element: Flow[EL],
      t: EventPropTransformation[Ev, V],
      inContext: EventInContext[EL, V, R]
  ): Flow[EventStream[R]] =
    element.map(e => e.events(t).compose(inContext(e)))

  def fromEvents[Ev <: dom.Event, V](t: EventPropTransformation[Ev, V]): Flow[V] =
    eventsOf(element, t, EventInContext.identity[E, V]).flatMap(fromStream(_))

  def fromEvents[Ev <: dom.Event, V, R](
      t: EventPropTransformation[Ev, V],
      inContext: EventInContext[E, V, R]
  ): Flow[R] =
    eventsOf(element, t, inContext).flatMap(fromStream(_))

  def fromEventsOf[EL <: Element, Ev <: dom.Event, V](
      element: Flow[EL],
      t: EventPropTransformation[Ev, V]
  ): Flow[V] =
    eventsOf(element, t, EventInContext.identity[EL, V]).flatMap(fromStream(_))

  def fromEventsOf[EL <: Element, Ev <: dom.Event, V, R](
      element: Flow[EL],
      t: EventPropTransformation[Ev, V],
      inContext: EventInContext[EL, V, R]
  ): Flow[R] =
    eventsOf(element, t, inContext).flatMap(fromStream(_))

}
