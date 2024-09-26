package prices.routes

import cats.data.{ Kleisli, OptionT }
import cats.effect.kernel.Concurrent
import cats.syntax.applicativeError._
import cats.syntax.functor._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import prices.services.Exception

import scala.util.control.NonFatal

class ErrorHandling[F[_]: Concurrent] extends Http4sDsl[F] {
  private def knownErrorResponse(error: prices.services.Exception): F[Response[F]] = error match {
    // The syntax wrapper should be applied implicitly, but for some reason it isn't
    case Exception.APICallFailure(message) => http4sServiceUnavailableSyntax(Status.ServiceUnavailable)(message)
  }
  private def unknownErrorResponse(error: Throwable): F[Response[F]] =
    // The syntax wrapper should be applied implicitly, but for some reason it isn't
    http4sInternalServerErrorSyntax(Status.InternalServerError)(error.getMessage)

  def errorHandling(service: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req =>
      OptionT(service(req).value.recoverWith {
        case e: prices.services.Exception => knownErrorResponse(e).map(Some(_))
        case NonFatal(t)                  => unknownErrorResponse(t).map(Some(_))
      })
    }
}
