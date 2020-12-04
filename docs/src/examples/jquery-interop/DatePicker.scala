package examples.jquery_interop

import cyclone._

import com.raquo.laminar.api.L
import L._

import scalajs.js
import org.scalajs.dom

// An example using https://www.daterangepicker.com/
object DatePicker {

  // For this example we use js.Dynamic for interacting with the jQuery world
  import js.Dynamic
  import Dynamic.literal

  private val jQuery = Dynamic.global.jQuery
  private val Moment = Dynamic.global.moment

  private type Moment      = Dynamic
  private type MomentRange = (Moment, Moment)

  def momentFormat(moment: Moment): String = moment.format("YYYY-MM-DD").toString

  val datePicker: Cyclone[Nothing, MomentRange, Nothing] =
    Cyclone[Nothing, MomentRange, Nothing] { cycle =>
      import cycle._

      val startDate: Moment         = Moment()
      val endDate: Moment           = Moment().add(5, "days")
      val initialState: MomentRange = startDate -> endDate

      val mainFlow: Flow[Unit] = for {
        jq <- element[L.Input].map(el => jQuery(el.ref))
        range <- fromCallback[MomentRange] { cb =>
          val pluginOptions = literal(opens = "left", startDate = startDate, endDate = endDate)
          jq.daterangepicker(pluginOptions, { (start: Moment, end: Moment) => cb(start -> end) })
        }
        _ <- updateStateTo(range)
      } yield ()

      cycle(initialState, mainFlow)
    }

  val view: Div =
    div(
      input(datePicker.bind()),
      div("From: ", child.text <-- datePicker.state.map(_._1).map(momentFormat)),
      div("To: ", child.text <-- datePicker.state.map(_._2).map(momentFormat))
    )

}
