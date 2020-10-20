package examples

import com.raquo.laminar.api.L._
import org.scalajs.dom

object Main extends scala.App {

  val view: Div =
    div(
      hello.Hello.view,
      hr(),
      clock.Clock.view
    )

  render(dom.document.body, view)

}
