package prices.services

import cats.effect.{ Clock, Temporal }
import cats.effect.std.MapRef
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import cats.syntax.option._
import fs2.Stream
import org.log4s._
import prices.config.Config.AppConfig
import prices.data.{ InstanceDetails, InstanceKind }

import scala.util.control.NonFatal

class CachePopulatorService[F[_]: Temporal](
    cache: MapRef[F, InstanceKind, Option[InstanceDetails]],
    underlying: InstanceKindService[F] with InstanceDetailsService[F],
    config: AppConfig
)(
    implicit
    clock: Clock[F]
) {
  private val logger = getLogger

  /** Polls all available instance types, putting them in the cache, and returns those instances that were polled
    */
  private[services] val doPoll = underlying
    .getAll()
    .flatMap {
      _.foldMapM { k =>
        underlying
          .get(k)
          .flatMap(details => cache(k).set(details.some))
          .as(Set(k))
          // Early recover so that one failed call to underlying doesn't fail the whole poll
          .recover {
            case NonFatal(e) =>
              logger.warn(e)(s"Error polling for kind $k")
              Set.empty
          }
      }
    }
    .recover {
      case NonFatal(e) =>
        logger.warn(e)(s"Error polling")
        Set.empty
    }

  /** Reaps stale instances from the cache
    * @return
    *   those instanceKinds that have not been reaped
    */
  private[services] def doReap(potentiallyStale: Set[InstanceKind]) =
    clock.realTimeInstant
      .flatMap { now =>
        // should be potentiallyStale.unorderedFoldMapM but that doesn't exist
        potentiallyStale.toVector.foldMapM { k =>
          cache(k).modify {
            case Some(d) =>
              if (d.timestamp plus config.systemMaxStaleness isAfter now)
                (None, Vector.empty)
              else
                (Some(d), Vector(k))
            case None =>
              logger.error(s"InstanceKind ${k.getString} mysteriously disappeared from cache")
              (None, Vector.empty) // should never happen, but should be safe to continue if so
          }
        }
      }
      .map(_.toSet)

  private def doPollAndReap(current: Set[InstanceKind]) = for {
    fetched <- doPoll
    potentiallyStale = current -- fetched
    notReaped <- doReap(potentiallyStale)
  } yield fetched ++ notReaped

  val cachePopulatorFiber: Stream[F, Set[InstanceKind]] =
    (Stream.unit ++ Stream.awakeEvery(config.pollInterval)).evalScan(Set.empty[InstanceKind]) {
      case (current, _) => doPollAndReap(current)
    }
}
