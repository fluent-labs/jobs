package io.fluentlabs.jobs.definitions.clean

import io.fluentlabs.jobs.SparkSessionBuilder
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

abstract class DefinitionsCleaningJob[T](source: String) {
  @transient lazy val log: Logger =
    LogManager.getLogger("Definitions parsing job")

  // Config variables from the job
  val definitionsBucketPath: String =
    sys.env.getOrElse("definitionsBucketPath", "s3://definitions/")
  val version: String = sys.env.get("version") match {
    case Some(v) => v
    case None    => throw new IllegalArgumentException("No version provided")
  }

  // Methods for subclasses to implement
  def getFilename(source: String, version: String): String
  def load(path: String)(implicit spark: SparkSession): DataFrame
  def clean(data: DataFrame)(implicit spark: SparkSession): Dataset[T]

  // Values we calculated
  val rawPath: String =
    s"$definitionsBucketPath/$source/$version/raw/${getFilename(source, version)}"
  val cleanPath: String =
    s"$definitionsBucketPath/$source/$version/clean/}"

  def main(args: Array[String]): Unit = {
    log.info(s"Getting file from $rawPath")

    implicit val spark: SparkSession = SparkSessionBuilder
      .build(s"Clean $source")
    log.info("Created spark session")

    log.info("Loading data")
    val raw = load(rawPath)
    log.info("Loaded data")

    log.info("Cleaning data")
    val cleaned = clean(raw)
    log.info("Cleaning data")

    log.info(s"Saving to path $cleanPath")
    cleaned.write.json(cleanPath)
    log.info("Finished saving to S3")
  }
}
