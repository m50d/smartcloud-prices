package prices.data

import io.circe.{ Decoder, Encoder }
import io.circe.syntax._

final case class InstanceKind(getString: String) extends AnyVal

object InstanceKind {
  implicit val encoder: Encoder[InstanceKind] = Encoder.instance[InstanceKind] { k =>
    k.getString.asJson
  }
  implicit val decoder: Decoder[InstanceKind] = Decoder.decodeString.map(InstanceKind(_))
}
