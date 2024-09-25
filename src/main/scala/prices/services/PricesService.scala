package prices.services

import cats.Functor
import cats.syntax.functor._
import prices.data._
import prices.routes.protocol.PriceResponse

trait PricesService[F[_]] {
  def get(k: InstanceKind): F[PriceResponse]
}

object PricesService {
  private final class PricesServiceImpl[F[_]: Functor](
      details: InstanceDetailsService[F]
  ) extends PricesService[F] {
    override def get(k: InstanceKind): F[PriceResponse] = details.get(k).map { i =>
      PriceResponse(i.kind, i.price)
    }
  }

  def make[F[_]: Functor](details: InstanceDetailsService[F]): PricesService[F] = new PricesServiceImpl[F](details)
}
