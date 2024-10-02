package prices.config

import cats.effect.kernel.Sync
import org.http4s.Uri
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.http4s._

import java.time.Duration
import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

case class Config(
    app: Config.AppConfig,
    smartcloud: Config.SmartcloudConfig
)

object Config {

  case class AppConfig(
      host: String,
      port: Int,
      pollInterval: FiniteDuration,
      maxStaleness: FiniteDuration
  ) {
    val systemMaxStaleness: Duration = maxStaleness.toJava
  }

  case class SmartcloudConfig(
      baseUri: Uri,
      token: String
  )

  def load[F[_]: Sync]: F[Config] =
    Sync[F].delay(ConfigSource.default.loadOrThrow[Config])

}
