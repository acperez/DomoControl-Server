name := """DomoControlHub"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.12.0",
  "org.reactivemongo" %% "reactivemongo-play-json" % "0.12.0"
)

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
