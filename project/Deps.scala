import sbt._

object ZIO {
  private val version       = "2.0.0"
  private val configVersion = "3.0.1"
  private val jsonVersion   = "0.3.0-RC10"

  val core           = "dev.zio" %% "zio"                 % version
  val streams        = "dev.zio" %% "zio-streams"         % version
  val logging        = "dev.zio" %% "zio-logging"         % version
  val json           = "dev.zio" %% "zio-json"            % jsonVersion
  val config         = "dev.zio" %% "zio-config"          % configVersion
  val configMagnolia = "dev.zio" %% "zio-config-magnolia" % configVersion
}

object Sttp {
  private val version = "3.7.0"

  val core    = "com.softwaremill.sttp.client3" %% "core"     % version
  val zio     = "com.softwaremill.sttp.client3" %% "zio"      % version
  val zioJson = "com.softwaremill.sttp.client3" %% "zio-json" % version
}

object Quill {
  val zio    = "io.getquill" %% "quill-jdbc-zio" % "4.1.0-V2"
  val sqlite = "org.xerial"   % "sqlite-jdbc"    % "3.36.0.3"
}

object Deps {
  val kiteConnect = "com.zerodhatech.kiteconnect" % "kiteconnect" % "3.2.1"
  val totp        = "dev.samstevens.totp"         % "totp"        % "1.7.1"
}
