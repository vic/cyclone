package examples.hello

import com.raquo.laminar.api.L._
import cyclone.Cyclone
import cyclone.Effects._

object Hello {

  case class SayHello(to: String)

  val cycle: Cyclone[SayHello, Option[String], Nothing] =
    Cyclone[SayHello, Option[String], Nothing](
      initState = None,
      inHandler = {
        case SayHello(name) if name.trim.isEmpty =>
          update(None)

        case SayHello(name) =>
          update(Some(name.toUpperCase()))
      }
    )

  val view: Div =
    div(
      cycle.mod,
      "Hello ",
      child.text <-- cycle.state.map(_.getOrElse("World")),
      br(),
      input(
        placeholder := "Enter your name",
        inContext { input => onKeyUp.mapTo(SayHello(input.ref.value)) --> cycle.input }
      )
    )

}
