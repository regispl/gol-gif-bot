import sbt._

object Dependencies {
  val akkaV       = "2.5.3"
  val akkaHttpV   = "10.0.9"
  val akkaCirceV  = "1.17.0"
  val catsV       = "1.0.1"
  val catsEffV    = "0.10"
  val circeV      = "0.9.1"
  val logbackV    = "1.1.7"
  val scalaLogV   = "3.5.0"
  val scalaTestV  = "3.0.1"
  val scoptV      = "3.5.0"

  lazy val logback        = "ch.qos.logback"              % "logback-classic"   % logbackV
  lazy val scopt          = "com.github.scopt"            %% "scopt"            % scoptV
  lazy val akkaHttp       = "com.typesafe.akka"           %% "akka-http"        % akkaHttpV
  lazy val akkaStream     = "com.typesafe.akka"           %% "akka-stream"      % akkaV
  lazy val logging        = "com.typesafe.scala-logging"  %% "scala-logging"    % scalaLogV
  lazy val akkaHttpCirce  = "de.heikoseeberger"           %% "akka-http-circe"  % akkaCirceV
  lazy val circeCore      = "io.circe"                    %% "circe-core"       % circeV
  lazy val circeGeneric   = "io.circe"                    %% "circe-generic"    % circeV
  lazy val circeParser    = "io.circe"                    %% "circe-parser"     % circeV
  lazy val circeOptics    = "io.circe"                    %% "circe-optics"     % circeV
  lazy val catsCore       = "org.typelevel"               %% "cats-core"        % catsV
  lazy val catsEffects    = "org.typelevel"               %% "cats-effect"      % catsEffV
  lazy val scalactic      = "org.scalactic"               %% "scalactic"        % scalaTestV
  lazy val scalatest      = "org.scalatest"               %% "scalatest"        % scalaTestV
}
