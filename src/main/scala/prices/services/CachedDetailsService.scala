package prices.services

import cats.MonadError
import cats.effect.std.MapRef
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import prices.data.{ InstanceDetails, InstanceKind }
import prices.services.Exception.NoDetailsAvailable

class CachedDetailsService[F[_]: MonadError[*[_], Throwable]](cache: MapRef[F, InstanceKind, Option[InstanceDetails]]) extends InstanceDetailsService[F] {
  override def get(k: InstanceKind): F[InstanceDetails] = cache(k).get flatMap {
    case Some(value) => value.pure
    case None        => NoDetailsAvailable(k).raiseError
  }
}
