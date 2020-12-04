package cyclone

import com.raquo.laminar.api.L._

trait Channel {

  object Channel {
    // A request from Peer to talk about Topic using an IO of query and response, each response labeled with query
    type Req[P, C, Q, R] = (P, C, Cyclone.IO[(Q, R), Q])
  }

  import Channel._

  def channel[P, C, Q, R, S, O](
      fn: Cyclone.Spin[Req[P, C, Q, R], S, O] => Cyclone[Req[P, C, Q, R], S, O]
  ): Cyclone[Req[P, C, Q, R], S, O] =
    fn(Cyclone.Spin())

}
