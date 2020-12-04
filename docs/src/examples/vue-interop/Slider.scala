package examples.vue_interop

import com.raquo.laminar.api.L._
import com.raquo.laminar.builders.{HtmlBuilders, HtmlTag}
import cyclone._
import org.scalajs.dom
import org.scalajs.dom.html

import scala.scalajs.js

// Example using https://nightcatsama.github.io/vue-slider-component/#/
object Slider {

  // Laminar helpers for using vue tags and attributes
  object vueHtml extends HtmlBuilders {
    def vAttr(name: String): HtmlAttr[String] = stringHtmlAttr(s"v-${name}")
    val vModel: HtmlAttr[String]              = vAttr("model")

    val vueSlider: HtmlTag[html.Element] = htmlTag[dom.html.Element](tagName = "vue-slider", void = false)
  }

  import vueHtml._

  // In this example we use js.Dynamic to interact with the js world.
  import js.Dynamic._

  type Vue = js.Dynamic
  val Vue: Vue = global.Vue

  val slider: Cyclone[Nothing, Int, Nothing] =
    Cyclone[Nothing, Int, Nothing] { cycle =>
      import cycle._

      val initialState = 50

      val vueView =
        vueSlider(vModel := "value", vAttr("on:change") := "changed")

      def vueInit(el: dom.Element, onChange: Int => Unit): Vue =
        newInstance(Vue)(
          literal(
            el = el,
            data = () => literal(value = initialState, min = 0, max = 100),
            methods = literal(
              changed = onChange
            ),
            components = literal(
              VueSlider = global.window.selectDynamic("vue-slider-component")
            )
          )
        )

      val mainFlow: Flow[Unit] =
        for {
          (onChange, changes: Flow[Int]) <- makeCallback[Int]()
          vue <- element[Element]
            .map(_.amend(vueView))
            .map(el => vueInit(el.ref, onChange))
          _ <- changes.flatMap(updateStateTo(_))
        } yield ()

      cycle(initialState, mainFlow)
    }

  def view: Div =
    div(
      "Vue slider value: ",
      code(child.text <-- slider.state.map(_.toString)),
      div(slider.bind())
    )
}
