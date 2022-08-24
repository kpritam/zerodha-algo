import sbt._

object ZIO {
  private val version       = "2.0.1"
  private val configVersion = "3.0.2"
  private val jsonVersion   = "0.3.0-RC10"

  val core         = "dev.zio" %% "zio"               % version
  val streams      = "dev.zio" %% "zio-streams"       % version
  val logging      = "dev.zio" %% "zio-logging"       % version
  val test         = "dev.zio" %% "zio-test"          % version % Test
  val testSbt      = "dev.zio" %% "zio-test-sbt"      % version % Test
  val testMagnolia = "dev.zio" %% "zio-test-magnolia" % version % Test

  val json           = "dev.zio" %% "zio-json"            % jsonVersion
  val config         = "dev.zio" %% "zio-config"          % configVersion
  val configMagnolia = "dev.zio" %% "zio-config-magnolia" % configVersion
}

object Sttp {
  private val version = "3.7.4"

  val core    = "com.softwaremill.sttp.client3" %% "core"     % version
  val zio     = "com.softwaremill.sttp.client3" %% "zio"      % version
  val zioJson = "com.softwaremill.sttp.client3" %% "zio-json" % version
}

object Quill {
  val zio    = "io.getquill" %% "quill-jdbc-zio" % "4.3.0"
  val sqlite = "org.xerial"   % "sqlite-jdbc"    % "3.39.2.0"
}

object Deps {
  val kiteConnect = "com.zerodhatech.kiteconnect" % "kiteconnect" % "3.2.1"
  val totp        = "dev.samstevens.totp"         % "totp"        % "1.7.1"
  val flyway      = "org.flywaydb"                % "flyway-core" % "9.1.6"
}
