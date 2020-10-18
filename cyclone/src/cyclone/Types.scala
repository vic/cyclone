package cyclone

import com.raquo.laminar.api.L._

trait Types {

  type InputHandler[Input] = PartialFunction[Input, Flow[_]]

  sealed trait Flow[+A] {
    def flatMap[B](f: A => Flow[B]): Flow[B] = FlatMap[A, B](this, f)

    def map[B](f: A => B): Flow[B] =
      FlatMap[A, B](this, a => Pure[Any, B](_ => f(a)))

    def mapTo[B](b: => B): Flow[B] = map(_ => b)

    def withFilter(p: A => Boolean): Flow[A] =
      FlatMap[A, A](
        this,
        a => if (p(a)) Pure[Any, A](_ => a) else EmptyFlow.map[A](_ => ???)
      )
  }

  case object EmptyFlow                                                             extends Flow[Nothing]
  case class Pure[State, X] private[cyclone] (fn: State => X)                       extends Flow[X]
  case class Stream[State, +X] private[cyclone] (fn: State => EventStream[Flow[X]]) extends Flow[X]
  case class FlatMap[A, B] private[cyclone] (a: Flow[A], b: A => Flow[B])           extends Flow[B]

  case class UpdateState[State] private[cyclone] (fn: State => State)         extends Flow[(State, State)]
  case class EmitInput[State, Input] private[cyclone] (fn: State => Input)    extends Flow[Input]
  case class EmitOutput[State, Output] private[cyclone] (fn: State => Output) extends Flow[Output]

  case class InContext[E <: Element, X] private[cyclone] (fn: E => X) extends Flow[X]

  case class UpdateInputHandler[I] private[cyclone] (
      fn: InputHandler[I] => InputHandler[I]
  ) extends Flow[(InputHandler[I], InputHandler[I])]

}
