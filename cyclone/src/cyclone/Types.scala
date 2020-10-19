package cyclone

import com.raquo.laminar.api.L._

trait Types[E <: Element, I, S, O] {
  type El     = E
  type Input  = I
  type State  = S
  type Output = O

  type UpdateState   = Types.UpdateState[S]
  type EmitInput     = Types.EmitInput[I]
  type EmitOutput    = Types.EmitOutput[O]
  type InContext[+X] = Types.InContext[E, X]
  type Handler       = Types.Handler[I]
  type UpdateHandler = Types.UpdateHandler[Handler]
}

object Types {

  def apply[E <: Element, I, S, O]: Types[E, I, S, O] =
    new Types[E, I, S, O] {}

  sealed trait Flow[+X] {
    def flatMap[Y](f: X => Flow[Y]): Flow[Y] = FlatMap[X, Y](this, f)

    def map[Y](f: X => Y): Flow[Y] = FlatMap[X, Y](this, x => Pure[Y](() => f(x)))

    def mapTo[Y](f: => Y): Flow[Y] = map(_ => f)

    def withFilter(p: X => Boolean): Flow[X] = FlatMap[X, X](
      this,
      x => if (p(x)) Pure[X](() => x) else EmptyFlow.map[X](_ => ???)
    )
  }

  case object EmptyFlow extends Flow[Nothing]

  case class Pure[+X] private[cyclone] (fn: () => X) extends Flow[X]

  case class FlatMap[X, +Y] private[cyclone] (a: Flow[X], b: X => Flow[Y]) extends Flow[Y]

  case class FromStream[+X] private[cyclone] (fn: () => EventStream[Flow[X]]) extends Flow[X]

  case class IntoStream[+X] private[cyclone] (fn: () => Flow[X]) extends Flow[EventStream[X]]

  case class UpdateState[S] private[cyclone] (fn: S => S) extends Flow[(S, S)]

  case class EmitInput[I] private[cyclone] (fn: () => I) extends Flow[I]

  case class EmitOutput[O] private[cyclone] (fn: () => O) extends Flow[O]

  case class InContext[E <: Element, +X] private[cyclone] (fn: E => X) extends Flow[X]

  case class UpdateHandler[H] private[cyclone] (fn: H => H) extends Flow[(H, H)]

  type Handler[I] = PartialFunction[I, Flow[_]]

}
