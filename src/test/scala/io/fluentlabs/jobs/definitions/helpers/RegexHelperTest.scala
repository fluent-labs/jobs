package io.fluentlabs.jobs.definitions.helpers

import io.fluentlabs.jobs.TestWithSpark
import org.scalatest.funspec.AnyFunSpec

case class RegexMatch(matches: Seq[String])

class RegexHelperTest extends AnyFunSpec with TestWithSpark {
  import spark.implicits._

  def get_matches(t: String, expression: String): RegexMatch = {
    Seq(t)
      .toDF("text")
      .select(
        RegexHelper
          .regexp_extract_all("text", expression, 1)
          .alias("matches")
      )
      .as[RegexMatch]
      .first()
  }

  describe("can extract multiple matches from a document") {
    it("on the happy path") {
      val text: String =
        """
          |== This is a heading ==
          |another document item
          |another garbage thing
          |== This is another heading ==
          |= heading level that doesn't exist =""".stripMargin
      val expression = "== ([^=]+) =="

      val m = get_matches(text, expression)
      assert(
        m.matches sameElements Array(
          "This is a heading",
          "This is another heading"
        )
      )
    }

    it("when there are escape characters") {
      val text: String =
        """* {{a|RP}} {{IPA|en|/ˈdɪkʃ(ə)n(ə)ɹi/}}
          |* {{a|GenAm|Canada}} {{enPR|dĭk'shə-nĕr-ē}}, {{IPA|en|/ˈdɪkʃəˌnɛɹi/}}
          |* {{audio|en|en-us-dictionary.ogg|Audio (US, California)}}
          |* {{audio|en|en-uk-dictionary.ogg|Audio (UK)}}
          |* {{hyphenation|en|dic|tion|ary}}
          |* {{rhymes|en|ɪkʃənɛəɹi}}""".stripMargin

      val expression = "\\{\\{([^\\|]+).*\\}\\}"
      val m = get_matches(text, expression).matches.distinct
      assert(
        m sameElements Array("a", "audio", "hyphenation", "rhymes")
      )
    }
  }
}
