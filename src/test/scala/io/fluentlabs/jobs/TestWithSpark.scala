package io.fluentlabs.jobs

import org.apache.spark.sql.{Dataset, Encoder, SparkSession}

trait TestWithSpark {
  implicit lazy val spark: SparkSession = {
    SparkSession
      .builder()
      .master("local")
      .appName("spark test example")
      .getOrCreate()
  }

  def createDataset[T: Encoder](data: Seq[T]): Dataset[T] = {
    spark.createDataset(data)
  }
}
