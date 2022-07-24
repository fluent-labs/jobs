package io.fluentlabs.jobs.definitions.source

import com.databricks.spark.xml.XmlDataFrameReader
import io.fluentlabs.jobs.definitions.helpers.RegexHelper
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.functions.{col, explode, regexp_extract}
import org.apache.spark.sql.{Column, DataFrame, Row, SparkSession}

trait WiktionaryParser {
  @transient lazy val logger: Logger =
    LogManager.getLogger(this.getClass.getName)

  def loadWiktionaryDump(
      path: String
  )(implicit spark: SparkSession): DataFrame = {
    spark.read
      .option("rowTag", "page")
      .xml(path)
      .select("revision.text._VALUE", "title", "id")
      .withColumnRenamed("title", "token")
      .withColumnRenamed("_VALUE", "text")
      .filter(row => filterMetaArticles(row))
  }

  val metaArticleTitles: Set[String] =
    Set(
      "MediaWiki:",
      "MediaWiki talk:",
      "Template:",
      "Template talk:",
      "Wiktionary:",
      "Wiktionary talk:",
      "User:",
      "User talk:",
      "Category:",
      "Category talk:",
      "Help:",
      "File:",
      "File talk:",
      "Appendix:",
      "Module:",
      "Module talk:",
      "Talk:"
    )

  def filterMetaArticles(row: Row): Boolean = {
    val title = row.getAs[String]("token")
    metaArticleTitles.forall(prefix => !title.startsWith(prefix))
  }

  /*
   * Handles sections
   * == Name ==
   * contents
   */

  val caseInsensitiveFlag = "(?i)"
  val periodMatchesNewlineFlag = "(?s)"
  val oneOrMoreEqualsSign = "=+"
  val doubleEqualsSign = "=="
  val tripleEqualsSign = "==="
  val optionalWhiteSpace = " *"
  val anythingButEqualsSign = "([^=]*)"
  val lazyMatchAnything = "(.*?)"
  val spaceOrNewline = "[ |\n]+"
  val nextSection = s"(?>== *[A-Za-z0-9]+ *==$spaceOrNewline)"
  val nextSectionOrEndOfFile = s"(?>$nextSection|\\Z)+"

  /** A regex letting you find headings of a given size. Use this when you don't
    * know what you're looking for, but instead want to get a sense of how the
    * dump is structured.
    *
    * The first capture group is the name of the section.
    * @param level
    *   The heading level to look for.
    * @return
    *   A regular expression letting you find all heading.
    */
  def headingRegex(level: Int): String =
    RegexHelper.repeat(
      "=",
      level
    ) + optionalWhiteSpace + anythingButEqualsSign + optionalWhiteSpace + RegexHelper
      .repeat(
        "=",
        level
      ) + anythingButEqualsSign // Needed or else outer equals will be ignored
  // Subtle but '== Test ==' will match '=== Test ===' at this point: '="== Test =="='

  /** Matches everything within a wikimedia section, both the heading and the
    * content. For example: '''
    * ==Section==
    * (text inside) '''
    *
    * Use this when you know exactly what you're looking for and just want to
    * pull out the content.
    *
    * @param sectionName
    *   The name of the section to pull out
    * @param level
    *   The heading level, with 2 meaning ==, 3 meaning ===, etc.
    * @return
    *   A regex you can use to pull this out.
    */
  def nLevelSectionRegex(sectionName: String, level: Integer): String =
    periodMatchesNewlineFlag + caseInsensitiveFlag + RegexHelper.repeat(
      "=",
      level
    ) + optionalWhiteSpace + sectionName + optionalWhiteSpace + RegexHelper
      .repeat(
        "=",
        level
      ) + lazyMatchAnything + nextSectionOrEndOfFile

  // == name ==
  def sectionRegex(sectionName: String): String =
    nLevelSectionRegex(sectionName, 2)
  // === name ===
  def subSectionRegex(sectionName: String): String =
    nLevelSectionRegex(sectionName, 3)

  // Section extraction down here

  def extractSection(name: String, level: Integer): Column =
    regexp_extract(col("text"), nLevelSectionRegex(name, level), 1)
  def extractSection(name: String): Column =
    regexp_extract(col("text"), sectionRegex(name), 1)
  def extractSubSection(name: String): Column =
    regexp_extract(col("text"), subSectionRegex(name), 1)

  def extractSection(data: DataFrame, name: String): DataFrame =
    data.withColumn(name.toLowerCase(), extractSection(name))
  def extractSubsection(data: DataFrame, name: String): DataFrame =
    data.withColumn(name.toLowerCase(), extractSubSection(name))

  def extractSections(
      data: DataFrame,
      sections: Array[String]
  ): DataFrame = {
    sections
      .foldLeft(data.toDF())((data, section) => extractSection(data, section))
  }

  def extractSubsections(
      data: DataFrame,
      sections: Array[String]
  ): DataFrame = {
    sections.foldLeft(data)((data, section) => extractSection(data, section))
  }

  /*
   * Handles templates which are basically macros
   * eg. {{templateName|args}}
   */

  val notPipeCaptureGroup: String =
    "([^" + RegexHelper.pipe + RegexHelper.rightBrace + "]+)"
  val notRightBraceCaptureGroup: String =
    "(" + RegexHelper.pipe + "[^" + RegexHelper.rightBrace + "]*)?"

  // {{a|b}}
  val templateRegex: String =
    RegexHelper.leftBrace + RegexHelper.leftBrace + notPipeCaptureGroup + notRightBraceCaptureGroup + RegexHelper.rightBrace + RegexHelper.rightBrace
}
