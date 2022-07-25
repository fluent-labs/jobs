package io.fluentlabs.jobs.definitions.analyze

import io.fluentlabs.jobs.definitions.source.WiktionaryParser
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

case class WiktionarySection(heading: String, count: BigInt)

// Use this when you want to know what kind of sections a backup has. Good for getting the rough structure of the dump
class WiktionarySectionFinder(source: String)
    extends DefinitionsAnalysisJob(source)
    with WiktionaryParser {

  override def getFilename(source: String, version: String): String =
    s"$source-$version-pages-meta-current.xml"

  override def load(path: String)(implicit spark: SparkSession): DataFrame =
    loadWiktionaryDump(path)

  def analyze(data: DataFrame, outputPath: String)(implicit
      spark: SparkSession
  ): Unit = {
    getHeadings(data, 1).write.csv(s"$outputPath/headings/level_one")
    getHeadings(data, 2).write.csv(s"$outputPath/headings/level_two")
    getHeadings(data, 3).write.csv(s"$outputPath/headings/level_three")
    getHeadings(data, 4).write.csv(s"$outputPath/headings/level_four")
  }

  def getHeadings(
      data: DataFrame,
      level: Integer
  )(implicit spark: SparkSession): Dataset[WiktionarySection] = {
    import spark.implicits._

    data
      .select(
        explode(split(col("text"), "\n")).alias("text")
      )
      .select(
        regexp_extract(col("text"), headingRegex(level), 1).alias("heading")
      )
      .groupBy("heading")
      .agg(count("*").alias("count"))
      .as[WiktionarySection]
      .sort(col("count"))
      .coalesce(1)
  }
}
