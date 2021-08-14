import sbt._

object Dependencies {
  val hadoopVersion = "3.3.1"
  val sparkVersion = "3.1.2"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.8"
  val content = "io.fluentlabs" %% "content" % "1.0.1"
  val sparkCore =
    "org.apache.spark" %% "spark-core" % sparkVersion
  val sparkSql = "org.apache.spark" %% "spark-sql" % sparkVersion
  val sparkXml = "com.databricks" %% "spark-xml" % "0.12.0"

  val hadoop = "org.apache.hadoop" % "hadoop-common" % hadoopVersion
  val hadoopClient = "org.apache.hadoop" % "hadoop-client" % hadoopVersion
  val hadoopAWS = "org.apache.hadoop" % "hadoop-aws" % hadoopVersion
  val awsJavaSDK = "com.amazonaws" % "aws-java-sdk" % "1.12.42"
  // Enable this when it is build for scala 2.12 in main
  // And remove our local version
  //    val elasticsearchHadoop =
  //      "org.elasticsearch" % "elasticsearch-hadoop" % elasticsearchVersion
}
