package io.fluentlabs.jobs.definitions.source

import io.fluentlabs.jobs.definitions.helpers.RegexHelper
import org.scalatest.funspec.AnyFunSpec

class WiktionaryTest extends AnyFunSpec {
  object Wiktionary extends WiktionaryParser

  /*
   * Nearly everything in the Wiktionary object is regex, we don't need to bring up an entire spark context to test.
   * Let's keep these tests fast.
   */
  def regex_extract_all(
      data: String,
      pattern: String,
      index: Integer
  ): List[String] = {
    pattern.r.findAllMatchIn(data).map(_.group(index).trim).toList
  }

  describe("can correctly generate regexes") {
    it("can repeat a pattern") {
      assert(RegexHelper.repeat("=", 6) == "======")
    }

    describe("for a heading of any size") {
      val levelThreeHeading = Wiktionary.headingRegex(3)
      it("which match valid headings") {
        assert("=== Title ===".matches(levelThreeHeading))
      }

      // There's some subtle bugs around matching too many and too few
      // This is to prevent regression

      it("does not match larger headings") {
        assert(!"== Title ==".matches(levelThreeHeading))
      }

      it("does not match smaller headings") {
        assert(!"==== Title ====".matches(levelThreeHeading))
      }
    }

    // This is there to cover refactors, feel free to wipe the assertion if the regex materially changes.
    it("for a section") {
      assert(
        "(?s)(?i)== *MyTestSection *==(.*?)(?>(?>== *[A-Za-z0-9]+ *==[ |\n]+)|\\Z)+" == Wiktionary
          .sectionRegex("MyTestSection")
      )
    }

    // This is there to cover refactors, feel free to wipe the assertion if the regex materially changes.
    it("for a subsection") {
      assert(
        "(?s)(?i)=== *MyTestSubsection *===(.*?)(?>(?>== *[A-Za-z0-9]+ *==[ |\n]+)|\\Z)+" == Wiktionary
          .subSectionRegex("MyTestSubsection")
      )
    }
  }

  describe("can generate regex patterns for headings") {
    val text =
      """== This is a heading ==
        |another document item
        |=== subheading that should be ignored ===
        |another garbage thing
        |== This is another heading ==
        |= heading level that doesn't exist =
        |=== uneven in a different way ==
        |""".stripMargin

    it("on the happy path") {
      val regex = Wiktionary.headingRegex(2)
      val items = regex_extract_all(text, regex, 1)
      assert(
        items sameElements Array(
          "This is a heading",
          "This is another heading"
        )
      )
    }
  }
}
