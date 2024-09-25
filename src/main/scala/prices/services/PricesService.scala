package prices.services

import prices.data._
import prices.routes.protocol.PriceResponse

trait PricesService[F[_]] {
  def get(k: InstanceKind): F[PriceResponse]
}
