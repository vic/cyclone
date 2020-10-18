package examples

import com.raquo.laminar.api.L._
import org.scalajs.dom

object Main extends scala.App {

  render(dom.document.body, hello.Hello.view)

}
