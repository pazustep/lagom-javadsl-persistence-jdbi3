name in ThisBuild := "lagom-javadsl-persistence-jdbi3"

organization in ThisBuild := "to.bri"

version in ThisBuild := "0.1.2"

scalaVersion in ThisBuild := "2.11.8"

libraryDependencies ++= Seq(
  "com.lightbend.lagom" %% "lagom-javadsl-persistence-jdbc" % "[1.3.0,4)",
  "org.jdbi" % "jdbi3" % "[3.0.0-beta0,4)"
)

bintrayReleaseOnPublish in ThisBuild := false

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))

useGpg := true

usePgpKeyHex("8eb81df9")
