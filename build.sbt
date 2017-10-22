name := "gol-gif-bot"

version := "0.1.2"

scalaVersion := "2.12.3"

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-deprecation",           // Emit warning and location for usages of deprecated APIs.
  "-feature",               // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",             // Enable additional warnings where generated code depends on assumptions.
  "-Xfuture",
  "-Xfatal-warnings",       // Fail the compilation if there are any warnings.
  "-Xlint",                 // Enable recommended additional warnings.
  "-Xcheckinit",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",       // Warn when dead code is identified.
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused",
  "-Ypartial-unification"   //
)

val akkaV       = "2.5.3"
val akkaHttpV   = "10.0.9"
val akkaCirceV  = "1.17.0"
val catsV       = "1.0.0-MF"
val circeV      = "0.8.0"
val logbackV    = "1.1.7"
val scalaLogV   = "3.5.0"
val scalaTestV  = "3.0.1"
val scoptV      = "3.5.0"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % logbackV,
  "com.github.scopt" %% "scopt" % scoptV,
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "com.typesafe.akka" %% "akka-stream" % akkaV,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLogV,
  "de.heikoseeberger" %% "akka-http-circe" % akkaCirceV,
  "io.circe" %% "circe-core" % circeV,
  "io.circe" %% "circe-generic" % circeV,
  "io.circe" %% "circe-parser" % circeV,
  "io.circe" %% "circe-optics" % circeV,
  "org.typelevel" %% "cats-core" % catsV,
  "org.scalactic" %% "scalactic" % scalaTestV,
  "org.scalatest" %% "scalatest" % scalaTestV % "test"
)

mainClass in assembly := Some("im.michalski.golgifbot.GolGifBot")
assemblyJarName in assembly := "GolGifBot.jar"
