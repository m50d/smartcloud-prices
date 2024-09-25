package prices.routes

import cats.effect._
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import prices.data.Price
import prices.routes.protocol._
import prices.services.PricesService

final case class PricesRoutes[F[_]: Sync](pricesService: PricesService[F]) extends Http4sDsl[F] {

  val prefix = "/prices"

  implicit val priceResponseEncoder = jsonEncoderOf[F, Price]

  private val get: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root :? InstanceKindQueryParam(k) =>
      pricesService.get(k).map(Ok(_))
  }

  def routes: HttpRoutes[F] =
    Router(
      prefix -> get
    )

}
