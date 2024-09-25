package prices.services

import cats.implicits._
import cats.effect._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.headers.Authorization
import prices.data._
import prices.services.Exception.APICallFailure

import scala.util.control.NonFatal

object SmartcloudService {

  final case class Config(
      baseUri: Uri,
      token: String
  )

  def make[F[_]: Concurrent](client: Client[F], config: Config): InstanceKindService[F] = new SmartcloudInstanceKindService(client, config)

  private final class SmartcloudInstanceKindService[F[_]: Concurrent](
      client: Client[F],
      config: Config
  ) extends InstanceKindService[F] {

    implicit val instanceKindsEntityDecoder: EntityDecoder[F, List[String]] = jsonOf[F, List[String]]

    private val getAllUri = config.baseUri / "instances"

    private val getAllRequest = Request[F](
      method = Method.GET,
      uri = getAllUri,
      headers = Headers(
        Authorization(Credentials.Token(AuthScheme.Bearer, config.token))
      )
    )

    override def getAll(): F[List[InstanceKind]] =
      client
        .expect[List[String]](getAllRequest)
        .map(_.map(InstanceKind(_)))
        .recoverWith {
          case NonFatal(e) => APICallFailure(e.getMessage).raiseError[F, List[InstanceKind]]
        }

  }

}
