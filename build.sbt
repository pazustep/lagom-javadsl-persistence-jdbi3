name in ThisBuild := "lagom-javadsl-persistence-jdbi3"

organization in ThisBuild := "to.bri"

version in ThisBuild := "1.0.0"

scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.8", "2.12.4")

libraryDependencies ++= Seq(
  "com.lightbend.lagom" %% "lagom-javadsl-persistence-jdbc" % "1.4.0",
  "org.jdbi" % "jdbi3-core" % "3.0.0"
)

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))
