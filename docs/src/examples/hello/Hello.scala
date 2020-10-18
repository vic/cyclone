package examples.hello

import com.raquo.laminar.api.L._
import cyclone._

object Hello {

  case class SayHello(to: String)

  val cycle: Cyclone[SayHello, Option[String], Nothing] =
    new Landspout[SayHello, Option[String], Nothing] {
      override protected val initialState: State = None
      override lazy val initialInputHandler: InputHandler = {
        case SayHello(name) => update(_ => Some(name))
      }
    }

  val view: Div =
    div(
      cycle.mod,
      div(
        "Hello ",
        child.text <-- cycle.state.map(_.getOrElse(""))
      ),
      input(
        placeholder := "Enter your name",
        inContext { input =>
          input.events(onKeyUp).mapTo(SayHello(input.ref.value)) --> cycle.input
        }
      )
    )

}
