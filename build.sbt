val scala3Version    = "3.1.3"
val zioVersion       = "2.0.0"
val zioConfigVersion = "3.0.1"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "zerodha-algo",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,

    // zio dependencies
    libraryDependencies += "dev.zio" %% "zio"                 % zioVersion,
    libraryDependencies += "dev.zio" %% "zio-streams"         % zioVersion,
    libraryDependencies += "dev.zio" %% "zio-config"          % zioConfigVersion,
    libraryDependencies += "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,

    // kite
    libraryDependencies += "com.zerodhatech.kiteconnect" % "kiteconnect" % "3.2.1",

    // test
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
  )
