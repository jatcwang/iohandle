import org.typelevel.sbt.tpolecat.DevMode

ThisBuild / tpolecatDefaultOptionsMode := DevMode

lazy val root = Project("root", file("."))
  .settings(commonSettings)
  .settings(
    name := "IOHandle",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-mtl" % "1.5.0",
      "org.typelevel" %% "cats-effect" % "3.6.0",
      "org.scalameta" %% "munit" % "1.1.0" % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.1.0" % Test,
    ),
  )

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "3.6.4",
)
