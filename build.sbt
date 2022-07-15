val scala3Version = "3.1.3"

inThisBuild(
  List(
    scalaVersion                                   := scala3Version,
    version                                        := "0.1.0-SNAPSHOT",
    fork                                           := true,
    scalacOptions ++= Seq(
      "-explain",
      "-indent",
      "-new-syntax"
    ),
    semanticdbEnabled                              := true,
    semanticdbVersion                              := scalafixSemanticdb.revision,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
  )
)

lazy val root = project
  .in(file("."))
  .aggregate(kiteconnect, strategies)

lazy val strategies = project
  .settings(
    fork := true,
    libraryDependencies ++= Seq(
      Deps.kiteConnect,
      ZIO.core,
      ZIO.streams,
      ZIO.logging,
      Sttp.core,
      Quill.zio,
      Quill.sqlite
    )
  )
  .dependsOn(kiteconnect)

lazy val kiteconnect = project
  .settings(
    libraryDependencies ++= Seq(
      Deps.kiteConnect,
      Deps.totp,
      ZIO.core,
      ZIO.streams,
      ZIO.json,
      ZIO.config,
      ZIO.configMagnolia,
      Sttp.core,
      Sttp.zioJson
    )
  )
