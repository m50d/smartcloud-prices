package prices.routes

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.{ Method, Request, Status }
import prices.data.{ InstanceDetails, InstanceKind }
import prices.services.Exception.APICallFailure
import prices.services.{ InstanceDetailsService, PricesService }

import java.time.Instant

class PricesRoutesSuite extends munit.FunSuite {
  object StubDetailsService extends InstanceDetailsService[IO] {
    override def get(k: InstanceKind): IO[InstanceDetails] = k.getString match {
      case "apicallfailure" => IO.raiseError(APICallFailure("failed"))
      case "miscfailure"    => IO.raiseError(new RuntimeException("failed"))
      case _                => IO.pure(InstanceDetails(k, 0.12, Instant.parse("2020-04-29T03:15:02Z")))
    }
  }

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

  test("misc failure") {
    val request: Request[IO] = Request(method = Method.GET, uri = uri"/prices?kind=miscfailure")
    client
      .run(request)
      .use { response =>
        IO.delay(assertEquals(Status.InternalServerError, response.status))
      }
      .unsafeToFuture()
  }
}
