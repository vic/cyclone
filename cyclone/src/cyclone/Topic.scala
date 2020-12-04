package cyclone

import com.raquo.laminar.api.L._

trait Topic {

  object Topic {
    // A request from Peer to talk about Topic using an IO of query and response, each response labeled with query
    type Req[P, T, Q, R] = (P, T, Cyclone.IO[(Q, R), Q])
  }

  import Topic._

  def topic[P, T, Q, R, S, O](
      fn: Cyclone.Spin[Req[P, T, Q, R], S, O] => Cyclone[Req[P, T, Q, R], S, O]
  ): Cyclone[Req[P, T, Q, R], S, O] =
    fn(Cyclone.Spin())

}
