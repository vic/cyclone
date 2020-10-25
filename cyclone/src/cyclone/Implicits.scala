package cyclone

import com.raquo.laminar.api.L._

trait Implicits {

  implicit def signalBinder[X](active: Signal[Boolean]): SignalBinder.BindOnSignal[X] =
    new SignalBinder.BindOnSignal[X](active)

}
