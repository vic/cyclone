package examples.hello

import com.raquo.laminar.api.L._
import cyclone._

object Hello {

  case class SayHello(to: String)

  val vortex: Vortex[Div, SayHello, String, Nothing] =
    Vortex[Div, SayHello, String, Nothing] build { cycle =>
      cycle(initState = "World") {
        case SayHello(name) =>
          cycle.updateTo(name.toUpperCase())
      }
    }

  val view: Div =
    div(
      vortex.bind(),
      "Hello ",
      child.text <-- vortex.state,
      br(),
      input(
        placeholder := "Enter your name",
        inContext { input => onKeyUp.mapTo(SayHello(input.ref.value)) --> vortex }
      )
    )

}
