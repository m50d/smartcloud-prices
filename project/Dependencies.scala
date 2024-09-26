import sbt._

object Dependencies {

  object V { // Versions
    // Scala

    val http4s     = "1.0.0-M29"
    val circe      = "0.15.0-M1"
    val logback    = "1.2.6"
    val pureConfig = "0.16.0"

    // Test
    val munit = "0.7.29"

    // Compiler
    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.13.3"
  }

  object L { // Libraries
    // Scala
    def http4s(module: String): ModuleID = "org.http4s" %% s"http4s-$module" % V.http4s
    def circe(module: String): ModuleID  = "io.circe"   %% s"circe-$module"  % V.circe

    val logback    = "ch.qos.logback"         % "logback-classic" % V.logback
    val pureConfig = "com.github.pureconfig" %% "pureconfig"      % V.pureConfig
    // pureconfig-http4s is built against an older version of http4s
    val pureConfigHttp4s = "com.github.pureconfig" %% "pureconfig-http4s" % V.pureConfig exclude ("org.http4s", "http4s-core_2.13")
  }

  object T { // Test dependencies
    // Scala
    val munit = "org.scalameta" %% "munit" % V.munit % Test
  }

  object C { // Compiler plugins
    val betterMonadicFor = compilerPlugin("com.olegpy" %% "better-monadic-for" % V.betterMonadicFor)
    val kindProjector    = compilerPlugin("org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full)
  }

}
