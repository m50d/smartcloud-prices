package prices.routes

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.implicits._
import org.http4s._
import prices.StubTimes
import prices.data.{InstanceDetails, InstanceKind}
import prices.services.Exception.APICallFailure
import prices.services.{InstanceDetailsService, PricesService}

trait StubDetailsService extends InstanceDetailsService[IO] {
  override def get(k: InstanceKind): IO[InstanceDetails] = k.getString match {
    case "apicallfailure" => IO.raiseError(APICallFailure("public"))
    case "miscfailure"    => IO.raiseError(new RuntimeException("secret"))
    case _                => IO.pure(InstanceDetails(k, 0.12, StubTimes.start))
  }
}
object StubDetailsService extends StubDetailsService

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
