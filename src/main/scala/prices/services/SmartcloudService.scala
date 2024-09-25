package prices.services

import cats.effect._
import cats.implicits._
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

  def make[F[_]: Concurrent](client: Client[F], config: Config): InstanceKindService[F] with InstanceDetailsService[F] = new SmartcloudService(client, config)

  private final class SmartcloudService[F[_]: Concurrent](
      client: Client[F],
      config: Config
  ) extends InstanceKindService[F]
      with InstanceDetailsService[F] {

    implicit val instanceKindsEntityDecoder: EntityDecoder[F, List[String]]      = jsonOf[F, List[String]]
    implicit val instanceDetailsEntityDecoder: EntityDecoder[F, InstanceDetails] = jsonOf[F, InstanceDetails]

    private val getAllUri               = config.baseUri / "instances"
    private def getUri(k: InstanceKind) = config.baseUri / "instances" / k.getString
    private def buildRequest(uri: Uri) = Request[F](
      method = Method.GET,
      uri = uri,
      headers = Headers(
        Authorization(Credentials.Token(AuthScheme.Bearer, config.token))
      )
    )
    private val getAllRequest               = buildRequest(getAllUri)
    private def getRequest(k: InstanceKind) = buildRequest(getUri(k))

    override def getAll(): F[List[InstanceKind]] =
      client
        .expect[List[String]](getAllRequest)
        .map(_.map(InstanceKind(_)))
        .recoverWith {
          case NonFatal(e) => APICallFailure(e.getMessage).raiseError[F, List[InstanceKind]]
        }

    override def get(k: InstanceKind): F[InstanceDetails] =
      client
        .expect[InstanceDetails](getRequest(k))
        .recoverWith {
          case NonFatal(e) => APICallFailure(e.getMessage).raiseError[F, InstanceDetails]
        }

  }

}
