ThisBuild / tlBaseVersion := "0.1" // your current series x.y

ThisBuild / organization := "io.github.jatcwang"
ThisBuild / organizationName := "IOHandle"
ThisBuild / startYear := Some(2025)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers ++= List(
  tlGitHubDev("jatcwang", "Jacob Wang"),
)
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"))

val Scala2 = "2.13.16"
//val Scala3 = "3.3.6"
val Scala3 = "3.6.4"
ThisBuild / crossScalaVersions := Seq(Scala2, Scala3)
ThisBuild / scalaVersion := Scala2 // the default Scala
//ThisBuild / scalaVersion := Scala3 // the default Scala

lazy val iohandleRoot = Project("root", file("."))
  .aggregate(iohandle, examples)
  .settings(
    name := "IOHandle",
    publish / skip := true,
  )
lazy val iohandle = Project("iohandle", file("modules/iohandle"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.13.0",
      "org.typelevel" %% "cats-mtl" % "1.5.0",
      "org.typelevel" %% "cats-effect" % "3.6.0",
    ),
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.1.0",
      "org.typelevel" %% "munit-cats-effect" % "2.1.0",
    ).map(_ % Test),
  )

lazy val examples = Project("examples", file("modules/examples"))
  .dependsOn(iohandle)
  .settings(publish / skip := true)

lazy val commonSettings = Seq(
)
