package examples.clock

import cyclone._
import com.raquo.laminar.api.L._

import scala.scalajs.js.Date
import org.scalajs.dom

object Clock {

  val clock: Cyclone[Nothing, Unit, Date] =
    Cyclone[Nothing, Unit, Date] { cycle =>
      import cycle._

      val times: EventStream[Date] =
        EventStream.periodic(intervalMs = 1000).mapTo(new Date())

      val mainFlow: Flow[Unit] = for {
        time: Date <- fromStream(times)
        _          <- emitOutput(time)
      } yield ()

      cycle(state = (), mainFlow = mainFlow)
    }

  val view: Div =
    div(
      clock.bind(),
      "Current time is: ",
      child.text <-- clock.output.map(_.toString)
    )

}
