package prices.services

import prices.data.InstanceKind

import scala.util.control.NoStackTrace

sealed trait Exception extends NoStackTrace
object Exception {
  case class APICallFailure(message: String) extends Exception
  case class NoDetailsAvailable(k: InstanceKind) extends Exception
}
