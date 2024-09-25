package prices.data

import io.circe.syntax._
import io.circe.{Encoder, Json}

final case class Price(kind: InstanceKind, amount: BigDecimal)
object Price {
  implicit val encoder: Encoder[Price] =
    Encoder.instance[Price] {
      p => Json.obj("kind" -> p.kind.asJson, "amount" -> p.amount.asJson)
    }
}