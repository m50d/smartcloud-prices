package prices.data

import cats.syntax.bifunctor._
import io.circe._
import io.circe.generic.semiauto._

import java.time.Instant
import scala.util.control.Exception.nonFatalCatch

case class InstanceDetails(kind: InstanceKind, price: BigDecimal, timestamp: Instant)
object InstanceDetails {
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeString.emap { str =>
    (nonFatalCatch either Instant.parse(str)).leftMap(_ => "Instant")
  }
  implicit val instanceDetailsDecoder: Decoder[InstanceDetails] = deriveDecoder[InstanceDetails]
}
