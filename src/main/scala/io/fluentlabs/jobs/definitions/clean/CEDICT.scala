package io.fluentlabs.jobs.definitions.clean

import org.apache.spark.sql.functions.{col, regexp_extract, split}
import org.apache.spark.sql.{Dataset, SparkSession}

case class CEDICTRawEntry(entry: String)
case class CEDICTDefinitionEntry(
    subdefinitions: List[String],
    pinyin: String,
    simplified: String,
    traditional: String,
    token: String
)

object CEDICT
    extends DefinitionsCleaningJob[CEDICTRawEntry, CEDICTDefinitionEntry](
      "cedict"
    ) {
  val lineRegex: String = "([^ ]+)\\s([^ ]+) \\[(.*)\\] \\/(.*)\\/"

  override def getFilename(source: String, version: String): String =
    "cedict_ts.u8"

  override def load(
      path: String
  )(implicit spark: SparkSession): Dataset[CEDICTRawEntry] = {
    import spark.implicits._
    spark.read
      .textFile(path)
      .withColumnRenamed("value", "entry")
      .as[CEDICTRawEntry]
  }

  override def clean(data: Dataset[CEDICTRawEntry])(implicit
      spark: SparkSession
  ): Dataset[CEDICTDefinitionEntry] = {
    import spark.implicits._

    data
      .where(!col("entry").startsWith("#"))
      .select(
        regexp_extract(col("entry"), lineRegex, 1).alias("traditional"),
        regexp_extract(col("entry"), lineRegex, 2).alias("simplified"),
        regexp_extract(col("entry"), lineRegex, 3).alias("pinyin"),
        regexp_extract(col("entry"), lineRegex, 4).alias("definitions")
      )
      .withColumn("token", col("traditional"))
      .withColumn("subdefinitions", split(col("definitions"), "/"))
      .drop("definitions")
      .as[CEDICTDefinitionEntry]
  }
}
