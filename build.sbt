val scala3Version = "3.1.3"

ThisBuild / scalaVersion := scala3Version
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / fork         := true
ThisBuild / scalacOptions ++= Seq(
  "-explain",
  "-indent",
  "-new-syntax"
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
