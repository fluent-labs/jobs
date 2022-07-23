package io.fluentlabs.jobs.definitions.clean

import com.databricks.spark.xml.XmlDataFrameReader
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

abstract class WiktionaryCleaningJob[T](source: String)
    extends DefinitionsCleaningJob[T](source) {

  override def getFilename(source: String, version: String): String =
    s"$source-$version-pages-meta-current.xml"

  override def load(path: String)(implicit spark: SparkSession): DataFrame = {
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

  // Helper methods here:
  // String.repeat is only implemented on some JVMs
  // Removing this has burned me twice.
  // Run the unit tests in the docker container before trying to remove it again.
  def repeat(token: String, count: Int): String = {
    (0 until count).map(_ => token).mkString
  }

  val caseInsensitiveFlag = "(?i)"
  val periodMatchesNewlineFlag = "(?s)"
  val oneOrMoreEqualsSign = "=+"
  val doubleEqualsSign = "=="
  val tripleEqualsSign = "==="
  val optionalWhiteSpace = " *"
  val anythingButEqualsSign = "[^=]*"
  val lazyMatchAnything = "(.*?)"
  val spaceOrNewline = "[ |\n]+"
  val nextSection = s"(?>== *[A-Za-z0-9]+ *==$spaceOrNewline)"
  val nextSectionOrEndOfFile = s"(?>$nextSection|\\Z)+"

  def headingRegex(equalsCount: Int): String =
    repeat(
      "=",
      equalsCount
    ) + optionalWhiteSpace + anythingButEqualsSign + optionalWhiteSpace + repeat(
      "=",
      equalsCount
    ) + anythingButEqualsSign // Needed or else outer equals will be ignored
  // Subtle but '== Test ==' will match '=== Test ===' at this point: '="== Test =="='

  def sectionRegex(sectionName: String): String =
    periodMatchesNewlineFlag + caseInsensitiveFlag + doubleEqualsSign + optionalWhiteSpace + sectionName + optionalWhiteSpace + doubleEqualsSign + lazyMatchAnything + nextSectionOrEndOfFile

  def subSectionRegex(sectionName: String): String =
    periodMatchesNewlineFlag + caseInsensitiveFlag + tripleEqualsSign + optionalWhiteSpace + sectionName + optionalWhiteSpace + tripleEqualsSign + lazyMatchAnything + nextSectionOrEndOfFile

}
