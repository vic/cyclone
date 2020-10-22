package examples.topic

import com.raquo.laminar.api.L._
import cyclone.Cyclone.IO
import cyclone._
import org.scalajs.dom.experimental.{Fetch, RequestInfo, RequestInit, Response}

trait FetchChannel
object FetchChannel extends FetchChannel {

  type P = Any          // Peer type. The type of components that can ask to Fetch
  type C = FetchChannel // The types of Channels this component supports
  type Q = FetchQ       // Question types
  type R = Response     // Reply types

  case class FetchQ(
      info: RequestInfo,
      init: RequestInit
  )

  val channel: Cyclone[Element, (P, C, IO[(Q, R), Q]), Unit, Nothing] =
    Cyclone.channel[Element, P, C, Q, R, Unit, Nothing] {
      cycle: Cyclone.Spin[Element, (P, C, IO[(Q, R), Q]), Unit, Nothing] =>
        import cycle._

        def topic(oi: IO[(Q, R), Q]): Flow[Unit] =
          for {
            _ <- unit
            io: Cyclone[El, Q, Unit, (Q, R)] = Cyclone.paired(oi)(topicExecutor)
            _ <- bind(io.bind())
          } yield ()

        val handler: Handler = { case (_, FetchChannel, oi) => topic(oi) }
        cycle(handler)
    }

  private val topicExecutor = { cycle: Cyclone.Spin[Element, Q, Unit, (Q, R)] =>
    import cycle._

    val handler: Handler = {
      case q: Q =>
        for {
          future <- value(Fetch.fetch(q.info, q.init).toFuture)
          r: R   <- fromStream(EventStream.fromFuture(future))
          _      <- emitOutput(q -> r)
        } yield ()
    }

    cycle(handler)
  }

}
