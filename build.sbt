name := "gol-gif-bot"

version := "1.0"

scalaVersion := "2.12.0"

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

val akkaV       = "2.4.14"
val akkaHttpV   = "10.0.0"
val akkaCirceV  = "1.11.0"
val catsV       = "0.8.1"
val circeV      = "0.6.1"
val scalaTestV  = "3.0.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "com.typesafe.akka" %% "akka-stream" % akkaV,
  "de.heikoseeberger" %% "akka-http-circe" % akkaCirceV,
  "io.circe" %% "circe-core" % circeV,
  "io.circe" %% "circe-generic" % circeV,
  "io.circe" %% "circe-parser" % circeV,
  "io.circe" %% "circe-optics" % circeV,
  "org.typelevel" %% "cats" % catsV,
  "org.scalactic" %% "scalactic" % scalaTestV,
  "org.scalatest" %% "scalatest" % scalaTestV % "test"
)
