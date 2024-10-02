package prices.routes

import cats.ApplicativeError
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import io.circe.Json
import io.circe.parser._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.implicits._
import org.http4s._
import prices.StubTimes
import prices.data.{ InstanceDetails, InstanceKind }
import prices.services.Exception.APICallFailure
import prices.services.{ InstanceDetailsService, PricesService }

abstract class StubDetailsService[F[_]: ApplicativeError[*[_], Throwable]] extends InstanceDetailsService[F] {
  override def get(k: InstanceKind): F[InstanceDetails] = k.getString match {
    case "apicallfailure" => APICallFailure("public").raiseError
    case "miscfailure"    => new RuntimeException("secret").raiseError
    case _                => InstanceDetails(k, 0.12, StubTimes.start).pure
  }
}
object StubDetailsService extends StubDetailsService[IO]

class PricesRoutesSuite extends munit.FunSuite {
  private val pricesService = PricesService.make(StubDetailsService)
  private val pricesRoutes  = PricesRoutes(pricesService)
  private val errorHandling = new ErrorHandling[IO]
  private val client        = Client.fromHttpApp(errorHandling.errorHandling(pricesRoutes.routes).orNotFound)

  test("success") {
    val expected = parse("""{"kind":"sc2-medium","amount":0.12}""".stripMargin).toOption.get

    val request: Request[IO] = Request(method = Method.GET, uri = uri"/prices?kind=sc2-medium")
    client
      .expect[Json](request)
      .map { response =>
        assertEquals(expected, response)
      }
      .unsafeToFuture()
  }

  test("api failure") {
    val request: Request[IO] = Request(method = Method.GET, uri = uri"/prices?kind=apicallfailure")
    client
      .run(request)
      .use { response =>
        response.as[String].map { data =>
          assertEquals(response.status, Status.ServiceUnavailable)
          assert(data.contains("public"), data)
        }
      }
      .unsafeToFuture()
  }

  test("misc failure") {
    val request: Request[IO] = Request(method = Method.GET, uri = uri"/prices?kind=miscfailure")
    client
      .run(request)
      .use { response =>
        response.as[String].map { data =>
          assertEquals(response.status, Status.InternalServerError)
          assert(!data.contains("secret"), data)
        }
      }
      .unsafeToFuture()
  }
}
