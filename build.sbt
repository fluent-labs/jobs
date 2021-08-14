import Dependencies._

ThisBuild / scalaVersion := "2.12.12"
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
    libraryDependencies ++= dependencies
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
  "-Ypartial-unification" // Remove me in scala 2.13
)
// Add these back in when we can get to scala 2.13
//  "-Wdead-code",
//  "-Wvalue-discard",

lazy val settings = Seq(
  githubTokenSource := TokenSource.Or(
    TokenSource.Environment("GITHUB_TOKEN"),
    TokenSource.GitConfig("github.token")
  ),
  releaseVersionBump := sbtrelease.Version.Bump.Bugfix,
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(
    true
  )
)

lazy val dependencies = Seq(
  Dependencies.scalaTest % Test,
  Dependencies.content,
//  Dependencies.sparkCore % "provided",
//  Dependencies.sparkSql % "provided",
  Dependencies.sparkXml,
  // S3 support
  Dependencies.hadoop,
  Dependencies.hadoopClient,
  Dependencies.hadoopAWS,
  Dependencies.awsJavaSDK,
  Dependencies.elasticsearchHadoop
)

/*
 * Release
 */

lazy val assemblySettings = Seq(
  organization := "io.fluentlabs",
  githubOwner := "fluent-labs",
  githubRepository := "jobs",
  // Used for building jobs fat jars
  assemblyJarName in assembly := name.value + ".jar",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
    case _                                   => MergeStrategy.first
  }
)

/*
 * Quality
 */

// Code coverage settings
coverageMinimum := 70
coverageFailOnMinimum := false
