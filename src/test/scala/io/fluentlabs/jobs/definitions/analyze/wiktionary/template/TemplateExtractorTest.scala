package io.fluentlabs.jobs.definitions.analyze.wiktionary.template

import io.fluentlabs.jobs.TestWithSpark
import io.fluentlabs.jobs.definitions.source.WiktionaryRawEntry
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.functions.col
import org.scalatest.funspec.AnyFunSpec

class TemplateExtractorTest extends AnyFunSpec with TestWithSpark {
  object TemplateExtractor
      extends WiktionaryTemplateExtractor("simplewiktionary")

  import spark.implicits._

  val text: String =
    """===Pronunciation===
      |* {{a|RP}} {{IPA|en|/ˈdɪkʃ(ə)n(ə)ɹi/}}
      |* {{a|GenAm|Canada}} {{enPR|dĭk'shə-nĕr-ē}}, {{IPA|en|/ˈdɪkʃəˌnɛɹi/}}
      |* {{audio|en|en-us-dictionary.ogg|Audio (US, California)}}
      |* {{audio|en|en-uk-dictionary.ogg|Audio (UK)}}
      |* {{hyphenation|en|dic|tion|ary}}
      |* {{rhymes|en|ɪkʃənɛəɹi}}""".stripMargin

  val textWithNoArgumentsTemplate: String = text + "\n* {{test}}"
  val emptyText = ""

  def getDatasetFromText(input: String): Dataset[WiktionaryRawEntry] =
    Seq(WiktionaryRawEntry(42, "token", input)).toDS()

  describe("it can extract templates with arguments from an entry") {
    val data = getDatasetFromText(text)
    it("can get all instances of templates with their arguments") {
      val instances =
        TemplateExtractor.extractTemplateInstances(data).cache()
      assert(instances.count() == 9L)
    }

    it("can count how many times a template was used") {
      val instances = TemplateExtractor.extractTemplateInstances(data)
      val counts =
        TemplateExtractor.extractTemplateCount(instances).cache()

      assert(counts.count() == 6L)
      assert(counts.filter(col("count") > 1).count() == 3L)
    }

    it("can gracefully handle templates with no arguments") {}
  }

  describe("it can extract templates without arguments from an entry") {
    val data = getDatasetFromText(textWithNoArgumentsTemplate)
    it("can get all instances of templates with their arguments") {
      val instances =
        TemplateExtractor.extractTemplateInstances(data).cache()

      instances.show(10)
      assert(instances.count() == 10L)
    }

    it("can count how many times a template was used") {
      val instances = TemplateExtractor.extractTemplateInstances(data)
      val counts =
        TemplateExtractor.extractTemplateCount(instances).cache()

      counts.show(7)
      assert(counts.count() == 7L)
      assert(counts.filter(col("count") > 1).count() == 3L)
    }
  }

  describe("it gracefully handles empty entries") {
    val data = getDatasetFromText(emptyText)
    it("can get all instances of templates with their arguments") {
      val instances =
        TemplateExtractor.extractTemplateInstances(data).cache()
      assert(instances.count() == 0L)
    }

    it("can count how many times a template was used") {
      val instances = TemplateExtractor.extractTemplateInstances(data)
      val counts =
        TemplateExtractor.extractTemplateCount(instances).cache()

      assert(counts.count() == 0L)
    }
  }
}
