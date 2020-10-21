package cyclone

import com.raquo.laminar.api.L._

import scala.util.Try

private[cyclone] object AirFlowing {

  sealed trait Effect[X]
  trait EmitInput[I]      extends Effect[I] with (() => I)
  trait EmitOutput[O]     extends Effect[O] with (() => O)
  trait UpdateState[S]    extends Effect[(S, S)] with (S => S)
  trait UpdateHandler[H]  extends Effect[(H, H)] with (H => H)
  trait MountedContext[E] extends Effect[MountContext[E]]

  /////////////////
//
//  trait Flow[+X]
//
//  implicit class FlowOps[+F[_] <: Flow[_], X](private val self: F[X]) {
//    def flatMap[Y](f: X => F[Y]): F[Y] = FlowAPI.flatMap(self, f)
//
//    def map[Y](f: X => Y): F[Y] = FlowAPI.flatMap(self, (x: X) => FlowAPI.lift(f(x)))
//
//    def mapTo[Y](y: => Y): F[Y] = map(_ => y)
//
//    def withFilter(p: X => Boolean): F[X] = FlowAPI.flatMap(
//      self,
//      (x: X) => if (p(x)) FlowAPI.lift(x) else FlowAPI.empty.flatMap(_ => ???)
//    )
//  }
//
//  object FlowAPI {
//    def lift[F[_] <: Flow[_]: FlowAPI, X](x: => X): F[X] =
//      implicitly[FlowAPI[F]].lift(x)
//
//    def flatMap[F[_] <: Flow[_]: FlowAPI, X, Y](a: F[X], b: X => F[Y]): F[Y] =
//      implicitly[FlowAPI[F]].flatMap(a, b)
//
//    def empty[F[_] <: Flow[_]: FlowAPI]: F[Nothing] =
//      implicitly[FlowAPI[F]].empty
//  }
//
//  trait FlowAPI[F[_] <: Flow[_]] {
//    def lift[X](x: => X): F[X]
//    def flatMap[X, Y](a: F[X], b: X => F[Y]): F[Y]
//    def empty: F[Nothing]
//  }
//
//  implicit final class AirFlow[X](val stream: EventStream[X]) extends Flow[X]
//
//  implicit object AirFlow extends FlowAPI[AirFlow] {
//    implicit def asStream[X](airFlow: AirFlow[X]) = airFlow.stream
//
//    override def lift[X](x: => X): AirFlow[X] =
//      EventStream.fromTry(Try(x), emitOnce = true)
//
//    override def flatMap[X, Y](a: AirFlow[X], b: X => AirFlow[Y]): AirFlow[Y] =
//      a.stream.flatMap(b(_).stream)
//
//    override def empty: AirFlow[Nothing] = EventStream.empty
//  }
//
//
//  /////////////////////////
//
//  val x: Flow[Int]        = Lift(99)
//  val y: EventStream[Int] = x.map(_ + 1)
//
//  val m: EventStream[Int] = for {
//    a <- Lift("hola")
//    b <- Lift(a + "jaja")
//  } yield 22
//
}
