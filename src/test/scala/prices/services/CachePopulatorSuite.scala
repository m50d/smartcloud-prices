package prices.services

import cats.Applicative
import cats.effect.{ Clock, IO }
import cats.effect.std.MapRef
import cats.effect.unsafe.implicits.global
import prices.StubTimes
import prices.config.Config.AppConfig
import prices.data.{ InstanceDetails, InstanceKind }
import prices.routes.StubDetailsService

import java.time.{ Duration, Instant }
import java.util.concurrent.ConcurrentHashMap
import scala.annotation.unused
import scala.concurrent.duration._
import scala.jdk.DurationConverters._

object InstanceKindSyntax {
  implicit class RichStringContext(sc: StringContext) {
    def k(@unused args: Any*): InstanceKind = InstanceKind(sc.parts.head)
  }
}
import InstanceKindSyntax._

object StubSmartcloudService extends StubDetailsService with InstanceKindService[IO] {
  override def getAll(): IO[List[InstanceKind]] = IO.pure(List(k"a", k"b"))
}

object StubClock extends Clock[IO] {
  override def applicative: Applicative[IO] = Applicative[IO]

  override def monotonic: IO[FiniteDuration] = realTime

  override val realTime: IO[FiniteDuration] = IO.pure(Duration.between(Instant.EPOCH, StubTimes.start.plusSeconds(5)).toScala)
}

class CachePopulatorSuite extends munit.FunSuite {
  private val config                = AppConfig("", 0, 3.second, 3.second)
  private val map                   = new ConcurrentHashMap[InstanceKind, InstanceDetails]()
  private val mapRef                = MapRef.fromConcurrentHashMap[IO, InstanceKind, InstanceDetails](map)
  private val cachePopulatorService = new CachePopulatorService(mapRef, StubSmartcloudService, config)(implicitly, StubClock)

  test("poll") {
    cachePopulatorService.doPoll
      .map { res =>
        assert(map.containsKey(k"a"))
        assert(map.containsKey(k"b"))
        assert(!map.containsKey(k"c"))
        assertEquals(res, Set(k"a", k"b"))
      }
      .unsafeToFuture()
  }

  /** Note this test will fail if run in isolation, as it relies on the side effects of the previous test
    */
  test("reap") {
    map.put(k"c", InstanceDetails(k"c", BigDecimal(2), StubTimes.start.plusSeconds(1)))
    assert(map.containsKey(k"b"))
    cachePopulatorService.doReap(Set(k"a", k"c")).map { res =>
      assert(map.containsKey("c"))
      assert(map.containsKey("b"))
      assert(!map.containsKey("a"))
      assertEquals(res, Set(k"c"))
    }

  }
}
