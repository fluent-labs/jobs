package io.fluentlabs.jobs.definitions

import io.fluentlabs.jobs.SparkSessionBuilder
import io.fluentlabs.jobs.definitions.DefinitionSource.DefinitionSource
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.reflect.runtime.universe.TypeTag

// Typetag needed to tell spark how to encode as a dataset
abstract class DefinitionsParsingJob[T](
    s3BasePath: String,
    defaultBackupFileName: String,
    source: DefinitionSource
) {
  @transient lazy val log: Logger =
    LogManager.getLogger("Definitions parsing job")

  def main(args: Array[String]): Unit = {
    val sourceName = source.toString.replace("_", "-").toLowerCase
    val backupFileName =
      sys.env.getOrElse("backup_file_name", defaultBackupFileName)
    val rawPath = s"$s3BasePath/raw/$backupFileName"
    log.info(s"Getting file from $rawPath")

    implicit val spark: SparkSession = SparkSessionBuilder
      .build(s"${sourceName.replace("-", " ")} parse")
    import spark.implicits._
    log.info("Created spark session")

    log.info("Loading data")
    val data = loadFromPath(rawPath)
    log.info("Loaded data")

    val cleanPath = s"$s3BasePath/clean/${source.toString}"
    log.info(s"Saving to path $cleanPath")
    data.write.parquet(cleanPath)
    log.info("Finished saving to S3")
  }

  def loadFromPath(path: String)(implicit spark: SparkSession): Dataset[T]
}
