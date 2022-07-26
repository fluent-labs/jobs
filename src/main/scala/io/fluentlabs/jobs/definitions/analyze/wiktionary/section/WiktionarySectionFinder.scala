package io.fluentlabs.jobs.definitions.analyze.wiktionary.section

import io.fluentlabs.jobs.definitions.analyze.DefinitionsAnalysisJob
import io.fluentlabs.jobs.definitions.helpers.RegexHelper
import io.fluentlabs.jobs.definitions.source.{
  WiktionaryParser,
  WiktionaryRawEntry
}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Dataset, SparkSession}

case class WiktionarySection(heading: String, count: BigInt)

// Use this when you want to know what kind of sections a backup has. Good for getting the rough structure of the dump
class WiktionarySectionFinder(source: String)
    extends DefinitionsAnalysisJob[WiktionaryRawEntry](source)
    with WiktionaryParser {

  override def getFilename(source: String, version: String): String =
    s"$source-$version-pages-meta-current.xml"

  override def load(path: String)(implicit
      spark: SparkSession
  ): Dataset[WiktionaryRawEntry] =
    loadWiktionaryDump(path)

  // $COVERAGE-OFF$
  def analyze(data: Dataset[WiktionaryRawEntry], outputPath: String)(implicit
      spark: SparkSession
  ): Unit = {
    getHeadings(data, 1).write.csv(s"$outputPath/headings/level_one")
    getHeadings(data, 2).write.csv(s"$outputPath/headings/level_two")
    getHeadings(data, 3).write.csv(s"$outputPath/headings/level_three")
    getHeadings(data, 4).write.csv(s"$outputPath/headings/level_four")
  }
  // $COVERAGE-ON$

  def getHeadings(
      data: Dataset[WiktionaryRawEntry],
      level: Integer
  )(implicit spark: SparkSession): Dataset[WiktionarySection] = {
    import spark.implicits._

    val regex = headingRegex(level)
    log.info(s"Searching for sections with regex pattern $regex")

    data
      .select(
        explode(RegexHelper.regexp_extract_all("text", regex, 1)).alias(
          "heading"
        )
      )
      .withColumn("heading", trim(col("heading")))
      .groupBy("heading")
      .agg(count("*").alias("count"))
      .as[WiktionarySection]
      .sort(col("count"))
      .coalesce(1)
  }
}
