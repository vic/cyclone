package examples.hello

import com.raquo.laminar.api.L._
import cyclone._

object Hello {

  case class SayHello(to: String)

  val cycle =
    Cyclone[Div, SayHello, String, Nothing] build { flow =>
      import flow._

      val handler: Handler = {
        case SayHello(name) =>
          updateTo(name.toUpperCase())
      }

      create("World", handler)
    }

  val view: Div =
    div(
      cycle.bind(),
      "Hello ",
      child.text <-- cycle.state,
      br(),
      input(
        placeholder := "Enter your name",
        inContext { input => onKeyUp.mapTo(SayHello(input.ref.value)) --> cycle }
      )
    )

}
