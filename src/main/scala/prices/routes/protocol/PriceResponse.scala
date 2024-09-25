package prices.routes.protocol

import io.circe.syntax._
import io.circe.{Encoder, Json}
import prices.data.InstanceKind

final case class PriceResponse(kind: InstanceKind, amount: BigDecimal)
object PriceResponse {
  implicit val encoder: Encoder[PriceResponse] =
    Encoder.instance[PriceResponse] { p =>
      Json.obj("kind" -> p.kind.asJson, "amount" -> p.amount.asJson)
    }
}
