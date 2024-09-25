package prices

import cats.effect._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import prices.config.Config
import prices.routes.InstanceKindRoutes
import prices.services.SmartcloudInstanceKindService

object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {
    val resource = for {
      client <- EmberClientBuilder.default[IO].build
      instanceKindService = SmartcloudInstanceKindService.make[IO](
                              client,
                              SmartcloudInstanceKindService.Config(
                                config.smartcloud.baseUri,
                                config.smartcloud.token
                              )
                            )
      httpApp = InstanceKindRoutes[IO](instanceKindService).routes.orNotFound
      server <- EmberServerBuilder
                  .default[IO]
                  .withHost(Host.fromString(config.app.host).get)
                  .withPort(Port.fromInt(config.app.port).get)
                  .withHttpApp(Logger.httpApp(logHeaders = true, logBody = true)(httpApp))
                  .build
    } yield ()

    Stream
      .eval(resource.useForever)
  }

}
