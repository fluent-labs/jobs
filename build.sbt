import Dependencies._

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "io.fluentlabs"
ThisBuild / organizationName := "fluentlabs"

lazy val root = (project in file("."))
  .enablePlugins(AssemblyPlugin)
  .settings(
    name := "jobs",
    scalacOptions ++= compilerOptions,
    settings,
    assemblySettings,
    libraryDependencies ++= dependencies,
    dependencyOverrides ++= forcedDependencies
  )

/*
 * Build
 */

lazy val compilerOptions = Seq(
  "-encoding",
  "utf8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Wdead-code",
  "-Wvalue-discard"
)

lazy val settings = Seq(
  releaseVersionBump := sbtrelease.Version.Bump.Bugfix,
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(
    true
  ),
  Compile / unmanagedJars += file(
    "lib/elasticsearch-spark-30_2.12-8.0.0-SNAPSHOT.jar"
  )
)

lazy val dependencies = Seq(
  Dependencies.scalaTest % Test,
  Dependencies.sparkCore % "provided",
  Dependencies.sparkSql % "provided",
  Dependencies.sparkXml,
  // S3 support
  Dependencies.hadoop,
  Dependencies.hadoopClient,
  Dependencies.hadoopAWS,
  Dependencies.awsJavaSDK
)

lazy val forcedDependencies = Seq(
  Dependencies.jacksonScala,
  Dependencies.jacksonDatabind,
  Dependencies.jacksonCore,
  Dependencies.paranamer,
  Dependencies.paranamer % "runtime"
)

/*
 * Release
 */

lazy val assemblySettings = Seq(
  organization := "io.fluentlabs",
  // Used for building jobs fat jars
  assembly / assemblyJarName := name.value + ".jar",
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
    case _                                   => MergeStrategy.first
  }
)

/*
 * Quality
 */

// Code coverage settings
coverageMinimumStmtTotal := 70
coverageFailOnMinimum := false
