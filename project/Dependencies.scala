import sbt._

object Dependencies {
  val elasticsearchVersion = "8.0.0-alpha1"
  val hadoopVersion = "3.3.1"
  val jacksonVersion = "2.11.3"
  val sparkVersion = "3.1.2"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.8"
  val content = "io.fluentlabs" %% "content" % "1.0.1"

  val sparkCore = "org.apache.spark" %% "spark-core" % sparkVersion
  val sparkSql = "org.apache.spark" %% "spark-sql" % sparkVersion
  val sparkXml = "com.databricks" %% "spark-xml" % "0.12.0"

  val hadoop = "org.apache.hadoop" % "hadoop-common" % hadoopVersion
  val hadoopClient = "org.apache.hadoop" % "hadoop-client" % hadoopVersion
  val hadoopAWS = "org.apache.hadoop" % "hadoop-aws" % hadoopVersion
  val awsJavaSDK = "com.amazonaws" % "aws-java-sdk" % "1.12.42"

  // Security related dependency upgrades below here
  val jacksonScala =
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
  val jacksonDatabind =
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
  val jacksonCore =
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion
}
