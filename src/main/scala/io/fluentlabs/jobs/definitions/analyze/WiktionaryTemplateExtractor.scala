package io.fluentlabs.jobs.definitions.analyze

import io.fluentlabs.jobs.definitions.helpers.RegexHelper
import io.fluentlabs.jobs.definitions.source.WiktionaryParser
import io.fluentlabs.jobs.definitions.{
  WiktionaryTemplate,
  WiktionaryTemplateInstance
}
import org.apache.spark.sql.functions.{arrays_zip, col, count, explode, first}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

class WiktionaryTemplateExtractor(source: String)
    extends DefinitionsAnalysisJob(source)
    with WiktionaryParser {

  override def getFilename(source: String, version: String): String =
    s"$source-$version-pages-meta-current.xml"

  override def load(path: String)(implicit spark: SparkSession): DataFrame =
    loadWiktionaryDump(path)

  def analyze(data: DataFrame, outputPath: String)(implicit
      spark: SparkSession
  ): Unit =
    extractTemplateCount(extractTemplateInstances(data)).write
      .csv(s"$outputPath/templates")

  case class WiktionaryTemplateInstance(name: String, arguments: String)
  case class WiktionaryTemplate(name: String, count: BigInt, example: String)

  def extractTemplateInstances(
      data: DataFrame
  )(implicit spark: SparkSession): Dataset[WiktionaryTemplateInstance] = {
    import spark.implicits._

    data
      .withColumn(
        "name",
        RegexHelper.regexp_extract_all("text", templateRegex, 1)
      )
      .withColumn(
        "arguments",
        RegexHelper.regexp_extract_all("text", templateRegex, 2)
      )
      .select(
        explode(arrays_zip(col("name"), col("arguments")))
          .alias("template")
      )
      .select(col("template.*"))
      .as[WiktionaryTemplateInstance]
  }

  def extractTemplateCount(
      data: Dataset[WiktionaryTemplateInstance]
  )(implicit spark: SparkSession): Dataset[WiktionaryTemplate] = {
    import spark.implicits._

    data
      .groupBy("name")
      .agg(
        count("*").alias("count"),
        first("arguments").alias("example")
      )
      .sort(col("count").desc)
      .as[WiktionaryTemplate]
  }
}
