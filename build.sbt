name := "Forex"

version := "1.0"

lazy val `forex` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

val scalactic =  "org.scalactic" %% "scalactic" % "3.0.3"
val scalatest = "org.scalatest" %% "scalatest" % "3.0.3" % "test"
libraryDependencies ++= Seq(jdbc, ehcache, ws, specs2 % Test, guice, scalactic, scalatest)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

      