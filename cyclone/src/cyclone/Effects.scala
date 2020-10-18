package cyclone

import com.raquo.laminar.api.L._

object Effects {
  import Types._

  val noEffect: Effect[Nothing] = NoEffect

  def emitInput[S, I](fn: S => I): Effect[I] =
    EmitInput[S, I](fn)

  def emitInput[S, I](input: => I): Effect[I] =
    EmitInput[S, I](_ => input)

  def emitOutput[S, O](fn: S => O): Effect[O] =
    EmitOutput[S, O](fn)

  def emitOutput[S, O](output: => O): Effect[O] =
    EmitOutput[S, O](_ => output)

  def current[S]: Effect[S] =
    Pure[S, S](identity)

  def update[S](fn: S => S): Effect[(S, S)] =
    UpdateState[S](fn)

  def updateTo[S](state: => S): Effect[(S, S)] =
    UpdateState[S](_ => state)

  def updateHandler[I](fn: InputHandler[I] => InputHandler[I]): Effect[(InputHandler[I], InputHandler[I])] =
    UpdateInputHandler(fn)

  def updateHandlerTo[I](fn: => InputHandler[I]): Effect[(InputHandler[I], InputHandler[I])] =
    UpdateInputHandler(_ => fn)

  def effect[S, X](fn: S => X): Effect[X] =
    Pure[S, X](fn)

  def pure[S, X](fn: => X): Effect[X] =
    Pure[S, X](_ => fn)

  def stream[S, X](fn: S => EventStream[Effect[X]]): Effect[X] =
    Stream[S, X](fn)

  def stream[S, X](fn: => EventStream[Effect[X]]): Effect[X] =
    Stream[S, X](_ => fn)

  def callback[S, X](fn: S => ((X => Unit) => Unit)): Effect[X] =
    Stream[S, X] { s =>
      val cb  = fn(s)
      val bus = new EventBus[X]
      cb(bus.writer.onNext)
      bus.events.map(pure[S, X](_))
    }

//  def ask[S, O, I, X](fwd: S => O)(bwd: PartialFunction[I, X]): Effect[X] = {
//    for {
//      (prevHandler, _) <- updateHandlerTo(emptyInputHandler)
//      (_, newHandler) <- updateHandlerTo {
//        case i if bwd.isDefinedAt(i) =>
//      }
//      o <- emitOutput(fwd)
//    } yield ()
//  }

}
