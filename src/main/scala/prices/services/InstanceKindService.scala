package prices.services

import prices.data._

trait InstanceKindService[F[_]] {
  def getAll(): F[List[InstanceKind]]
}
