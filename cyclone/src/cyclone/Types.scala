package cyclone

import com.raquo.laminar.api.L._

object Types {

  type InputHandler[Input] = PartialFunction[Input, Effect[_]]

  def emptyInputHandler[I]: InputHandler[I] = { case _ => NoEffect }

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

  final case object NoEffect                                                                extends Effect[Nothing]
  final case class Pure[State, X] private[cyclone] (fn: State => X)                         extends Effect[X]
  final case class Stream[State, +X] private[cyclone] (fn: State => EventStream[Effect[X]]) extends Effect[X]
  final case class FlatMap[A, B] private[cyclone] (a: Effect[A], b: A => Effect[B])         extends Effect[B]

  final case class UpdateState[State] private[cyclone] (fn: State => State)         extends Effect[(State, State)]
  final case class EmitInput[State, Input] private[cyclone] (fn: State => Input)    extends Effect[Input]
  final case class EmitOutput[State, Output] private[cyclone] (fn: State => Output) extends Effect[Output]

  final case class UpdateInputHandler[I] private[cyclone] (
      fn: InputHandler[I] => InputHandler[I]
  ) extends Effect[(InputHandler[I], InputHandler[I])]

}
