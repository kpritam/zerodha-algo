val scala3Version = "3.1.3"

inThisBuild(
  List(
    scalaVersion := scala3Version,
    version      := "0.1.0-SNAPSHOT",
    fork         := true,

    // compiler option
    scalacOptions ++= Seq(
      "-explain",
      "-indent",
      "-new-syntax"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,

    // scalafix
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
  )
)

lazy val root = project
  .in(file("."))
  .aggregate(kiteconnect, strategies)

lazy val strategies = project
  .enablePlugins(JavaAppPackaging)
  .settings(
    fork                   := true,
    libraryDependencies ++= Seq(
      Deps.kiteConnect,
      ZIO.core,
      ZIO.streams,
      ZIO.logging,
      Sttp.core,
      Quill.zio,
      Quill.sqlite,
      ZIO.test,
      ZIO.testSbt
    ),
    Universal / maintainer := "phkadam2008@gmail.com"
  )
  .dependsOn(kiteconnect)

lazy val kiteconnect = project
  .settings(
    libraryDependencies ++= Seq(
      Deps.kiteConnect,
      Deps.totp,
      Deps.flyway,
      ZIO.core,
      ZIO.streams,
      ZIO.json,
      ZIO.config,
      ZIO.configMagnolia,
      Sttp.core,
      Sttp.zio,
      Sttp.zioJson,
      ZIO.test,
      ZIO.testSbt
    )
  )
