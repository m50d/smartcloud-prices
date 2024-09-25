package prices.services

import prices.data.{InstanceDetails, InstanceKind}

trait InstanceDetailsService[F[_]] {
  def get(k: InstanceKind): F[InstanceDetails]
}
