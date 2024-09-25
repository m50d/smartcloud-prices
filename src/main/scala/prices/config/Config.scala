package prices.config

import cats.effect.kernel.Sync
import org.http4s.Uri
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.http4s._

case class Config(
    app: Config.AppConfig,
    smartcloud: Config.SmartcloudConfig
)

object Config {

  case class AppConfig(
      host: String,
      port: Int
  )

  case class SmartcloudConfig(
      baseUri: Uri,
      token: String
  )

  def load[F[_]: Sync]: F[Config] =
    Sync[F].delay(ConfigSource.default.loadOrThrow[Config])

}
