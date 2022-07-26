package io.fluentlabs.jobs

import org.apache.spark.sql.{Dataset, Encoder, SparkSession}

object SharedSparkSession {
  val spark: SparkSession = {
    SparkSession
      .builder()
      .master("local")
      .appName("Spark test runner")
      .getOrCreate()
  }
}

trait TestWithSpark {
  implicit lazy val spark: SparkSession = SharedSparkSession.spark

  def createDataset[T: Encoder](data: Seq[T]): Dataset[T] = {
    spark.createDataset(data)
  }
}
