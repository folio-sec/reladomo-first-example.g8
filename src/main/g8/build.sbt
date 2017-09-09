lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.folio-sec",
      scalaVersion := "2.12.3"
    )),
    name := "reladomo-simple",
    libraryDependencies ++= Seq(
      "com.folio-sec"  %% "reladomo-scala-common" % "16.5.6",
      "com.h2database" %  "h2"                    % "1.4.196",
      "org.scalatest"  %% "scalatest"             % "3.0.4"    % Test
    ),
    resolvers += "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"
  )
  .enablePlugins(ReladomoPlugin)
