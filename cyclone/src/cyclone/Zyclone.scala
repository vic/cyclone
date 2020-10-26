package cyclone

import com.raquo.laminar.api.L._
import zio.{Ref, _}
import zio.stream._

object Zyclone {

  type ZEventStream[-R, -E, +A] = ZStream[R, E, A]
  object ZEventStream {
    implicit class ZEventStreamOps[R, E, A](private val ev: ZEventStream[R, E, A]) extends AnyVal {
      def withCurrentValueOf[SB](signal: ZSignal[_, _, SB]): ZStream[R, E, (A, SB)] =
        ZSignal.zipStreamWithCurrentValue(signal, ev)

      def sample[B](signal: ZSignal[_, _, B]): ZStream[R, E, B] =
        withCurrentValueOf(signal).map(_._2)

      def startWith(initialValue: A): UIO[ZSignal[R, E, A]] =
        ZSignal(ev, ZIO.succeed(initialValue))
    }
  }

  type ZWriteBus[-R, +E, -A] = ZQueue[R, Any, E, Nothing, A, Nothing]

  class ZEventBus[-RA, -RB, +EA, +EB, -A, +B] private[ZEventBus] (private val queue: ZQueue[RA, RB, EA, EB, A, B]) {
    lazy val events: ZEventStream[RB, EB, B] = ZStream.fromQueueWithShutdown(queue)
    lazy val writer: ZWriteBus[RA, EA, A]    = queue.dimapM[RA, Any, EA, Nothing, A, Nothing](ZIO.succeed, _ => ZIO.never)
  }

  object ZEventBus {
    val DEFAULT_CAPACITY = 16

    def apply[A]: UIO[ZEventBus[Any, Any, Nothing, Nothing, A, A]] = apply(ZQueue.bounded[A](DEFAULT_CAPACITY))

    def apply[RA, RB, EA, EB, A, B](queue: UIO[ZQueue[RA, RB, EA, EB, A, B]]): UIO[ZEventBus[RA, RB, EA, EB, A, B]] =
      queue.map(new ZEventBus(_))
  }

  class ZSignal[R, E, A] private[ZSignal] (private[ZSignal] val mem: Ref[A], val changes: ZEventStream[R, E, A]) {}

  object ZSignal {

    def zipStreamWithCurrentValue[A, RB, EB, B](
        signal: ZSignal[_, _, A],
        ev: ZEventStream[RB, EB, B]
    ): ZStream[RB, EB, (B, A)] =
      ev.mapM(event => signal.mem.get.map(event -> _))

    def apply[R, E, A](fromEvents: ZStream[R, E, A], initial: UIO[A]): UIO[ZSignal[R, E, A]] =
      for {
        init <- initial
        mem  <- Ref.make(init)
        eventsWithMem: ZStream[R, E, (A, A)] = fromEvents.mapM(event => mem.get.map(_ -> event))
        changes: ZStream[R, E, A] = eventsWithMem.collectM {
          case (latest, event) if latest != event =>
            mem.set(event).as(event)
        }
      } yield new ZSignal[R, E, A](mem, changes)
  }

  class Zyclone[E <: Element, I, S, O]() {
    import ZEventStream.ZEventStreamOps

    val initialState: S = ???

    type ISO = Either[I, Either[S, O]]
    val isoBuz: UIO[ZEventBus[Any, Any, Nothing, Nothing, ISO, ISO]] = ZEventBus[ISO]

    val input: UIO[ZWriteBus[Any, Nothing, I]]     = isoBuz.map(_.writer.contramap[I](i => Left(i)))
    val output: UIO[ZEventStream[Any, Nothing, O]] = isoBuz.map(_.events.collect { case Right(Right(value)) => value })
    val state: UIO[ZSignal[Any, Nothing, S]] = isoBuz
      .map(_.events.collect { case Right(Left(value)) => value })
      .flatMap(_.startWith(initialState))

    def bind(active: ZSignal[Any, Nothing, Boolean]) = {
      for {
        x <- ZIO.runtime[Any]
      } yield ()
    }

  }

  class ZVortex[E <: Element, I, S, O]() extends Cyclone[E, I, S, O] {

    override val input: WriteBus[Input]      = ???
    override val state: Signal[State]        = ???
    override val output: EventStream[Output] = ???

    override def bind(active: Signal[Boolean]): Mod[El] = ???
  }

}
