package io.fluentlabs.jobs.definitions

import io.fluentlabs.jobs.SparkSessionBuilder
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.SparkSession

abstract class DefinitionsJob(
    source: String,
    previousState: String,
    nextState: String
) {
  @transient lazy val log: Logger =
    LogManager.getLogger("Definitions analysis job")

  // Config variables from the job
  val definitionsBucketPath: String =
    sys.env.getOrElse("definitionsBucketPath", "s3://definitions/")
  val version: String = sys.env.get("version") match {
    case Some(v) => v
    case None    => throw new IllegalArgumentException("No version provided")
  }

  // Methods for subclasses to implement
  def run(inputPath: String, outputPath: String)(implicit
      spark: SparkSession
  ): Unit

  // Values we calculated
  val inputPath: String =
    s"$definitionsBucketPath/$source/$version/$previousState/"
  val outputPath: String =
    s"$definitionsBucketPath/$source/$version/$nextState/}"

  def main(args: Array[String]): Unit = {
    implicit val spark: SparkSession = SparkSessionBuilder
      .build(s"$source $nextState job")
    log.info("Created spark session")

    run(inputPath, outputPath)
  }
}
