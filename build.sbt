name in ThisBuild := "lagom-javadsl-persistence-jdbi3"

organization in ThisBuild := "to.bri"

version in ThisBuild := "0.1.0"

scalaVersion in ThisBuild := "2.11.8"

libraryDependencies ++= Seq(
  "com.lightbend.lagom" %% "lagom-javadsl-persistence-jdbc" % "1.3.8+",
  "org.jdbi" % "jdbi3" % "3.0.0-beta2"
)
