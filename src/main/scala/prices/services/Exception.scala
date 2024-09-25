package prices.services

import scala.util.control.NoStackTrace

sealed trait Exception extends NoStackTrace
object Exception {
  case class APICallFailure(message: String) extends Exception
}
