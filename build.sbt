name in ThisBuild := "lagom-javadsl-persistence-jdbi3"

organization in ThisBuild := "to.bri"

version in ThisBuild := "0.1.4"

scalaVersion in ThisBuild := "2.11.8"

libraryDependencies ++= Seq(
  "com.lightbend.lagom" %% "lagom-javadsl-persistence-jdbc" % "1.3.0",
  "org.jdbi" % "jdbi3" % "3.0.0-beta0"
)

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))
