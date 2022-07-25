package io.fluentlabs.jobs

import org.apache.spark.sql.SparkSession

// $COVERAGE-OFF$
object SparkSessionBuilder {
  def build(name: String): SparkSession = {
    SparkSession
      .builder()
      .appName(name)
      .getOrCreate()
  }
}
// $COVERAGE-ON$
