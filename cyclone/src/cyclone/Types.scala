package cyclone

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveElement

private[cyclone] trait Types[I, S, O] {
  type Input  = I
  type State  = S
  type Output = O

  type Element = ReactiveElement.Base

  trait Action
  case class Mounted(ctx: MountContext[Element]) extends Action
  case class Unmounted(el: Element)              extends Action

  type InputHandler  = PartialFunction[Input, Effect[_]]
  type ActionHandler = PartialFunction[Action, Effect[_]]

  sealed trait Effect[+A] {
    def flatMap[B](f: A => Effect[B]): Effect[B] = FlatMap[A, B](this, f)
    def map[B](f: A => B): Effect[B]             = FlatMap[A, B](this, a => Pure[B](_ => f(a)))
    def withFilter(p: A => Boolean): Effect[A] =
      FlatMap[A, A](this, a => if (p(a)) Pure[A](_ => a) else NoEffect.map[A](_ => ???))
  }

  case object NoEffect                                               extends Effect[Nothing]
  case class Pure[X](fn: State => X)                                 extends Effect[X]
  case class Stream[+X](fn: EventStream[Effect[X]])                  extends Effect[X]
  case class FlatMap[A, B](a: Effect[A], b: A => Effect[B])          extends Effect[B]
  case class UpdateState(fn: State => State)                         extends Effect[(State, State)]
  case class EmitAction(fn: State => Action)                         extends Effect[Action]
  case class EmitInput(fn: State => Input)                           extends Effect[Input]
  case class EmitOutput(fn: State => Output)                         extends Effect[Output]
  case class UpdateInputHandler(fn: InputHandler => InputHandler)    extends Effect[(InputHandler, InputHandler)]
  case class UpdateActionHandler(fn: ActionHandler => ActionHandler) extends Effect[(ActionHandler, ActionHandler)]
}
