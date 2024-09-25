package prices.services

import prices.data._

trait PricesService[F[_]] {
  def get(k: InstanceKind): F[Price]
}
