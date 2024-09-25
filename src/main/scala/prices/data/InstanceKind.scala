package prices.data

import io.circe.Encoder
import io.circe.syntax._

final case class InstanceKind(getString: String) extends AnyVal

object InstanceKind {
  implicit val encoder: Encoder[InstanceKind] =
    Encoder.instance[InstanceKind] {
      k => k.getString.asJson
    }
}
