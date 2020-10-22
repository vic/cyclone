package cyclone

import com.raquo.laminar.api.L._

trait Channel {

  object Channel {
    // A request from Peer to talk about Topic using an IO of query and response, each response labeled with query
    type Req[P, C, Q, R] = (P, C, Cyclone.IO[(Q, R), Q])
  }

  import Channel._

  def channel[E <: Element, P, C, Q, R, S, O](
      fn: Cyclone.Spin[E, Req[P, C, Q, R], S, O] => Cyclone[E, Req[P, C, Q, R], S, O]
  ): Cyclone[E, Req[P, C, Q, R], S, O] =
    fn(Cyclone.Spin())

}
