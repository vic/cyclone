package examples.hello

import com.raquo.laminar.api.L._
import cyclone._

object Hello {

  case class SayHello(to: String)

  val hello: Cyclone[SayHello, String, Nothing] =
    Cyclone[SayHello, String, Nothing] { cycle =>
      import cycle._


      def onInput(input: In) = input match {
        case SayHello(name) =>
          updateStateTo(name.toUpperCase())
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
