package cyclone

import com.raquo.laminar.api.L._

import scala.util.{Failure, Success, Try}

trait FlowTypes[I, S, O] {
  type Input  = I
  type State  = S
  type Output = O

  type UpdateState    = cyclone.UpdateState[S]
  type EmitInput      = cyclone.EmitInput[I]
  type EmitOutput     = cyclone.EmitOutput[O]
  type Handler        = cyclone.Handler[I]
  type UpdateHandler  = cyclone.UpdateHandler[Handler]
}

trait Types {

  type Handler[I] = PartialFunction[I, Flow[_]]

  sealed trait Flow[+X] {
    def flatMap[Y](f: X => Flow[Y]): Flow[Y] = this match {
      case FlatMap(a, b) => FlatMap(a, b(_: Any).flatMap(f))
      case self          => FlatMap[X, Y](self, f)
    }

    def tapEffect(f: X => Unit): Flow[X] = tap(x => Value(() => f(x)))
    def tap(f: X => Flow[_]): Flow[X]    = flatMap { x => f(x).mapTo(x) }

    def map[Y](f: X => Y): Flow[Y] = FlatMap[X, Y](this, x => Value[Y](() => f(x)))

    def mapTo[Y](f: => Y): Flow[Y] = map(_ => f)

    def withFilter(p: X => Boolean): Flow[X] = FlatMap[X, X](
      this,
      x => if (p(x)) Value[X](() => x) else EmptyFlow.map[X](_ => ???)
    )

    def compose[Y](f: Flow[X] => Flow[Y]): Flow[Y] = f(this)

    def liftToTry: Flow[Try[X]]
    def lowerFromTry[Y](implicit ev: X <:< Try[Y]): Flow[Y] = map {
      case Failure(exception)           => throw exception
      case Success(value: Y @unchecked) => value
    }
  }

  case object EmptyFlow extends Flow[Nothing] {
    override def liftToTry: Flow[Try[Nothing]] = Value(() => Try(???))
  }

  case class Value[+X] private[cyclone] (fn: () => X) extends Flow[X] {
    override def liftToTry: Flow[Try[X]] = Value(() => Try(fn()))
  }

  case class FlatMap[X, +Y] private[cyclone] (a: Flow[X], b: X => Flow[Y]) extends Flow[Y] {
    override def liftToTry: Flow[Try[Y]] =
      a.liftToTry.flatMap {
        case Failure(exception) => Value(() => Failure(exception))
        case Success(value) =>
          b(value).liftToTry
      }
  }

  case class FromStream[+X] private[cyclone] (fn: () => EventStream[Flow[X]]) extends Flow[X] {
    override def liftToTry: Flow[Try[X]] =
      FromStream(() =>
        fn().recoverToTry.map {
          case Failure(exception) => Value(() => Failure(exception))
          case Success(flow)      => flow.liftToTry
        }
      )
  }

  case class EmitInput[I] private[cyclone] (fn: () => I) extends Flow[I] {
    override def liftToTry: Flow[Try[I]] =
      Value[I](() => fn()).liftToTry.flatMap {
        case Failure(exception) => Value(() => Failure(exception))
        case Success(i)         => EmitInput[I](() => i).mapTo(Success(i))
      }
  }

  case class EmitOutput[O] private[cyclone] (fn: () => O) extends Flow[O] {
    override def liftToTry: Flow[Try[O]] =
      Value[O](() => fn()).liftToTry.flatMap {
        case Failure(exception) => Value(() => Failure(exception))
        case Success(o)         => EmitOutput[O](() => o).mapTo(Success(o))
      }
  }

  case class MountedContext[E <: Element] private[cyclone] () extends Flow[MountContext[E]] {
    override def liftToTry: Flow[Try[MountContext[E]]] = map(Success(_))
  }

  sealed trait Update[+X] extends Flow[X] {
    override def liftToTry: Flow[Try[X]] = TryUpdate[X](this)
  }

  case class TryUpdate[X] private[cyclone] (effect: Update[X]) extends Update[Try[X]]

  case class UpdateState[S] private[cyclone] (fn: S => S) extends Update[(S, S)]

  case class UpdateHandler[H] private[cyclone] (fn: H => H) extends Update[(H, H)]

}
