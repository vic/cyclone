package cyclone

import com.raquo.laminar.api.L._

trait FlowTypes[E <: Element, I, S, O] {
  type El     = E
  type Input  = I
  type State  = S
  type Output = O

  type UpdateState    = cyclone.UpdateState[S]
  type EmitInput      = cyclone.EmitInput[I]
  type EmitOutput     = cyclone.EmitOutput[O]
  type MountedContext = cyclone.MountedContext[E]
  type Handler        = cyclone.Handler[I]
  type UpdateHandler  = cyclone.UpdateHandler[Handler]
}

trait Types {

  sealed trait Effect[X]
  trait Value[X]          extends Effect[X] with (() => X)
  trait EmitInput[I]      extends Effect[I] with (() => I)
  trait EmitOutput[O]     extends Effect[O] with (() => O)
  trait UpdateState[S]    extends Effect[(S, S)] with (S => S)
  trait UpdateHandler[H]  extends Effect[(H, H)] with (H => H)
  trait MountedContext[E] extends Effect[MountContext[E]]
  case object NoEffect    extends Effect[Nothing]

  type Flow[X] = EventStream[Effect[X]]
  val EmptyFlow: Flow[Nothing] = EventStream.empty.mapTo(NoEffect)

//  sealed trait Flow[+X] {
//    def flatMap[Y](f: X => Flow[Y]): Flow[Y] = FlatMap[X, Y](this, f)
//
//    def map[Y](f: X => Y): Flow[Y] = FlatMap[X, Y](this, x => Pure[Y](() => f(x)))
//
//    def mapTo[Y](f: => Y): Flow[Y] = map(_ => f)
//
//    def withFilter(p: X => Boolean): Flow[X] = FlatMap[X, X](
//      this,
//      x => if (p(x)) Pure[X](() => x) else EmptyFlow.map[X](_ => ???)
//    )
//  }
//
//  case object EmptyFlow extends Flow[Nothing]
//
//  case class Pure[+X] private[cyclone] (fn: () => X) extends Flow[X]
//
//  case class FlatMap[X, +Y] private[cyclone] (a: Flow[X], b: X => Flow[Y]) extends Flow[Y]
//
//  case class FromStream[+X] private[cyclone] (fn: () => EventStream[Flow[X]]) extends Flow[X]
//
//  case class UpdateState[S] private[cyclone] (fn: S => S) extends Flow[(S, S)]
//
//  case class EmitInput[I] private[cyclone] (fn: () => I) extends Flow[I]
//
//  case class EmitOutput[O] private[cyclone] (fn: () => O) extends Flow[O]
//
//  case class MountedContext[E <: Element] private[cyclone] () extends Flow[MountContext[E]]
//
//  case class UpdateHandler[H] private[cyclone] (fn: H => H) extends Flow[(H, H)]

  type Handler[I] = PartialFunction[I, Flow[_]]

}
