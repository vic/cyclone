package examples.clock

import cyclone._
import com.raquo.laminar.api.L._

import scala.scalajs.js.Date
import org.scalajs.dom

object Clock {

  val clock = Cyclone[Div, Nothing, Unit, Date] build { cycle =>
    import cycle._

    val times: EventStream[Date] =
      EventStream.periodic(intervalMs = 1000).mapTo(new Date())

    val mainFlow: Flow[Unit] = for {
      time <- fromStream(times)
      _    <- emitOutput(time)
    } yield ()

    cycle(initState = (), mainFlow)()
  }

  val view: Div =
    div(
      clock.bind(),
      "Current time is: ",
      child.text <-- clock.output.map(_.toString)
    )

}
