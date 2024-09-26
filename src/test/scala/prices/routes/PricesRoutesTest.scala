package prices.routes

import cats.effect.{IO, SyncIO}
import org.http4s.client.Client
import org.junit.Test
import prices.data.{InstanceDetails, InstanceKind}
import prices.services.Exception.APICallFailure
import prices.services.{InstanceDetailsService, PricesService}

import java.time.Instant

class PricesRoutesTest {
  object StubDetailsService extends InstanceDetailsService[SyncIO] {
    override def get(k: InstanceKind): SyncIO[InstanceDetails] = k.getString match {
      case "apicallfailure" => SyncIO.raiseError(APICallFailure("failed"))
      case "miscfailure"    => SyncIO.raiseError(new RuntimeException("failed"))
      case _                => SyncIO.pure(InstanceDetails(k, 0.12, Instant.parse("2020-04-29T03:15:02Z")))
    }
  }

  private val pricesService = PricesService.make(StubDetailsService)
  private val pricesRoutes  = PricesRoutes(pricesService)
//  private val client = Client.fromHttpApp(pricesRoutes.routes.orNotFound.mapF(_.to[IO]))


  @Test def basicFunctionality(): Unit = {

  }
}
