package examples.hello

import com.raquo.laminar.api.L._
import cyclone._

object Hello {

  case class SayHello(to: String)

  val hello: Cyclone[Div, SayHello, String, Nothing] =
    Cyclone.spin[Div, SayHello, String, Nothing] { cycle =>
      import cycle._

      def onInput(input: Input) = input match {
        case SayHello(name) =>
          updateTo(name.toUpperCase())
      }

      cycle(state = "World", onInput(_))
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
