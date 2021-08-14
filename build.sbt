import Dependencies._

ThisBuild / scalaVersion := "2.12.12"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "io.fluentlabs"
ThisBuild / organizationName := "fluentlabs"

lazy val root = (project in file("."))
  .settings(
    name := "jobs",
    libraryDependencies += Seq(
      Dependencies.scalaTest % Test,
      Dependencies.content,
      Dependencies.sparkCore % "provided",
      Dependencies.sparkSql % "provided",
      Dependencies.sparkXml,
      // S3 support
      Dependencies.hadoop,
      Dependencies.hadoopClient,
      Dependencies.hadoopAWS,
      Dependencies.awsJavaSDK
    )
  )
