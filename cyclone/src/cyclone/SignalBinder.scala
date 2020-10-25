package cyclone

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

object SignalBinder {

  private def bindWhile[E <: Element](active: Signal[Boolean])(dyn: DynamicOwner => DynamicSubscription): Binder[E] = {
    Binder[E] { el =>
      val dynOwner = new DynamicOwner(() => ())
      def activate(a: Boolean): Unit =
        if (a && !dynOwner.isActive) dynOwner.activate()
        else if (!a && dynOwner.isActive) dynOwner.deactivate()
      val activations = active.composeAll(_.map(activate), _.map(activate))
      val dynSub      = dyn(dynOwner)
      val dynBind     = Binder[E](_ => dynSub)
      ReactiveElement.bindSubscription(el.amend(dynBind, activations --> Observer.empty)) { ctx =>
        new Subscription(ctx.owner, cleanup = () => activate(false))
      }
    }
  }

  class BindOnSignal[X](private val active: Signal[Boolean]) extends AnyVal {
    def bindObserver[E <: Element](observable: Observable[X], observer: Observer[X]): Binder[E] =
      bindWhile(active)(DynamicSubscription.subscribeObserver(_, observable, observer))

    def bindFn[E <: Element](observable: Observable[X], onNext: X => Unit): Binder[E] =
      bindWhile(active)(DynamicSubscription.subscribeFn(_, observable, onNext))

    def bindBus[E <: Element](from: EventStream[X], to: WriteBus[X]): Binder[E] =
      bindWhile(active)(DynamicSubscription.subscribeBus(_, from, to))
  }

}
