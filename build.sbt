lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.admir",
      scalaVersion := "2.12.4",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "imagemagick",
    libraryDependencies ++= Seq(
      "com.beachape" %% "enumeratum" % "1.5.12",
      "com.typesafe.akka" %% "akka-stream" % "2.5.9",
      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "org.scalamock" %% "scalamock" % "4.0.0" % Test
    )
  )

siteSourceDirectory := target.value / "scala-2.12" / "scoverage-report"

addCommandAlias("showCoverage", "; clean ; coverage ; test ; coverageReport ; previewSite")
