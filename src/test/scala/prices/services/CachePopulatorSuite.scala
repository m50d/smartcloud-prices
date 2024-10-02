package prices.services

import cats.Applicative
import cats.effect.std.MapRef
import cats.effect.{ Clock, SyncIO }
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
import prices.services.InstanceKindSyntax._

object StubSmartcloudService extends StubDetailsService[SyncIO] with InstanceKindService[SyncIO] {
  override def getAll(): SyncIO[List[InstanceKind]] = SyncIO.pure(List(k"a", k"b"))
}

object StubClock extends Clock[SyncIO] {
  override def applicative: Applicative[SyncIO] = Applicative[SyncIO]

  override def monotonic: SyncIO[FiniteDuration] = realTime

  override val realTime: SyncIO[FiniteDuration] = SyncIO.pure(Duration.between(Instant.EPOCH, StubTimes.start.plusSeconds(5)).toScala)
}

class CachePopulatorSuite extends munit.FunSuite {
  private val config                = AppConfig("", 0, 3.second, 3.second)
  private val map                   = new ConcurrentHashMap[InstanceKind, InstanceDetails]()
  private val mapRef                = MapRef.fromConcurrentHashMap[SyncIO, InstanceKind, InstanceDetails](map)
  private val cachePopulatorService = new CachePopulatorService(mapRef, StubSmartcloudService, config)(implicitly, StubClock)

  test("poll") {
    val res = cachePopulatorService.doPoll.unsafeRunSync()
    assert(map.containsKey(k"a"))
    assert(map.containsKey(k"b"))
    assert(!map.containsKey(k"c"))
    assertEquals(res, Set(k"a", k"b"))
  }

  /** Note this test will fail if run in isolation, as it relies on the side effects of the previous test
    */
  test("reap") {
    map.put(k"c", InstanceDetails(k"c", BigDecimal(2), StubTimes.start.plusSeconds(4)))
    assert(map.containsKey(k"b"))
    val res = cachePopulatorService.doReap(Set(k"a", k"c")).unsafeRunSync()
    assert(map.containsKey(k"c"))
    assert(map.containsKey(k"b"))
    assert(!map.containsKey(k"a"))
    assertEquals(res, Set(k"c"))
  }
}
