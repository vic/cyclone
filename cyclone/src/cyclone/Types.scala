package cyclone

import com.raquo.laminar.api.L._

object Types {

  type InputHandler[Input]   = PartialFunction[Input, Effect[_]]
  type ActionHandler[Action] = PartialFunction[Action, Effect[_]]

  sealed trait Effect[+A] {
    def flatMap[B](f: A => Effect[B]): Effect[B] = FlatMap[A, B](this, f)

    def map[B](f: A => B): Effect[B] =
      FlatMap[A, B](this, a => Pure[Any, B](_ => f(a)))

    def withFilter(p: A => Boolean): Effect[A] =
      FlatMap[A, A](
        this,
        a => if (p(a)) Pure[Any, A](_ => a) else NoEffect.map[A](_ => ???)
      )
  }

  case object NoEffect                                                                extends Effect[Nothing]
  case class Pure[State, X] private[cyclone] (fn: State => X)                         extends Effect[X]
  case class Stream[State, +X] private[cyclone] (fn: State => EventStream[Effect[X]]) extends Effect[X]
  case class FlatMap[A, B] private[cyclone] (a: Effect[A], b: A => Effect[B])         extends Effect[B]

  case class UpdateState[State] private[cyclone] (fn: State => State)         extends Effect[(State, State)]
  case class EmitAction[State, Action] private[cyclone] (fn: State => Action) extends Effect[Action]
  case class EmitInput[State, Input] private[cyclone] (fn: State => Input)    extends Effect[Input]
  case class EmitOutput[State, Output] private[cyclone] (fn: State => Output) extends Effect[Output]

  case class UpdateInputHandler[I] private[cyclone] (
      fn: InputHandler[I] => InputHandler[I]
  ) extends Effect[(InputHandler[I], InputHandler[I])]

  case class UpdateActionHandler[A] private[cyclone] (
      fn: ActionHandler[A] => ActionHandler[A]
  ) extends Effect[(ActionHandler[A], ActionHandler[A])]

}

private[cyclone] trait Types[I, S, O] {
  type Input  = I
  type State  = S
  type Output = O

  trait Action
  type A = Action
  case class Mounted(ctx: MountContext[Element]) extends Action
  case class Unmounted(el: Element)              extends Action
}
