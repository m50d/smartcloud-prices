package prices.services

import cats.effect.IO
import cats.effect.std.MapRef
import cats.effect.unsafe.implicits.global
import prices.config.Config.AppConfig
import prices.data.{ InstanceDetails, InstanceKind }
import prices.routes.StubDetailsService

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._

object StubSmartcloudService extends StubDetailsService with InstanceKindService[IO] {
  override def getAll(): IO[List[InstanceKind]] = IO.pure(List(InstanceKind("a"), InstanceKind("b")))
}

class CachePopulatorSuite extends munit.FunSuite {
  private val config                = AppConfig("", 0, 1.second, 1.second)
  private val map                   = new ConcurrentHashMap[InstanceKind, InstanceDetails]()
  private val mapRef                = MapRef.fromConcurrentHashMap[IO, InstanceKind, InstanceDetails](map)
  private val cachePopulatorService = new CachePopulatorService(mapRef, StubSmartcloudService, config)

  test("poll") {
    cachePopulatorService.doPoll
      .map { res =>
        assert(map.containsKey(InstanceKind("a")))
        assert(map.containsKey(InstanceKind("b")))
        assert(!map.containsKey(InstanceKind("c")))
        assertEquals(res, Set(InstanceKind("a"), InstanceKind("b")))
      }
      .unsafeToFuture()
  }
}
