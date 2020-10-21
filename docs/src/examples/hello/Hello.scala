package examples.hello

import com.raquo.laminar.api.L._
import cyclone._

object Hello {

  case class SayHello(to: String)

  val hello: Cyclone[Div, SayHello, String, Nothing] =
    Cyclone[Div, SayHello, String, Nothing] build { cycle =>
      cycle(state = "World", handler = {
        case SayHello(name) =>
          cycle.updateTo(name.toUpperCase())
      })
    }

  val view: Div =
    div(
      hello.bind(),
      "Hello ",
      child.text <-- hello.state,
      br(),
      input(
        placeholder := "Enter your name",
        inContext { input => onKeyUp.mapTo(SayHello(input.ref.value)) --> hello }
      )
    )

}
