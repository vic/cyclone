package cyclone

import com.raquo.laminar.api.L._

// Cyclones are circular Airstreams around an stateful Vortex
trait Cyclone[I, S, O] {
  val input: Observer[I]
  val state: Signal[S]
  val output: Observable[O]
  val mod: Mod[Element]
}
