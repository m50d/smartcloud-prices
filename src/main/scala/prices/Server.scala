package prices

import cats.effect._
import cats.effect.std.MapRef
import cats.syntax.semigroupk._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import prices.config.Config
import prices.data.{ InstanceDetails, InstanceKind }
import prices.routes.{ ErrorHandling, InstanceKindRoutes, PricesRoutes }
import prices.services.{ CachePopulatorService, CachedDetailsService, PricesService, SmartcloudService }

object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {
    val resource = for {
      client <- EmberClientBuilder.default[IO].build
      smartcloudService = SmartcloudService.make[IO](
                            client,
                            SmartcloudService.Config(
                              config.smartcloud.baseUri,
                              config.smartcloud.token
                            )
                          )
      smartcloudCache <- MapRef.inConcurrentHashMap[ResourceIO, IO, InstanceKind, InstanceDetails]()
      cachedDetails      = new CachedDetailsService(smartcloudCache)
      cachePopulator     = new CachePopulatorService(smartcloudCache, smartcloudService, config.app)
      pricesService      = PricesService.make[IO](cachedDetails)
      instanceKindRoutes = InstanceKindRoutes[IO](smartcloudService)
      pricesRoutes       = PricesRoutes[IO](pricesService)
      errorHandling      = new ErrorHandling[IO]
      httpApp            = errorHandling.errorHandling(instanceKindRoutes.routes combineK pricesRoutes.routes).orNotFound
      _ <- EmberServerBuilder
             .default[IO]
             .withHost(Host.fromString(config.app.host).get)
             .withPort(Port.fromInt(config.app.port).get)
             .withHttpApp(Logger.httpApp(logHeaders = true, logBody = true)(httpApp))
             .build
    } yield cachePopulator

    Stream.resource(resource).flatMap { cachePopulator =>
      Stream.never[IO] concurrently cachePopulator.cachePopulatorFiber
    }
  }

}
