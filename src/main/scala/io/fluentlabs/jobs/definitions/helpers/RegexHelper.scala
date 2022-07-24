package io.fluentlabs.jobs.definitions.helpers

import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.expr

object RegexHelper {
  // Helpful escaped regex control characters
  val backslash = "\\"
  val leftBrace = "\\{"
  val rightBrace = "\\}"
  val openParenthesis = "\\("
  val closeParenthesis = "\\)"
  val pipe = "\\|"

  /** Passing a regex to something that will consume backslash escape
    * characters? Then you need to double escape it.
    * @param regex
    *   The regex to add escapes to.
    * @return
    *   A double escaped regex.
    */
  def doubleEscapeRegex(regex: String): String = {
    // Needed because spark consumes the escape characters before passing to java.util.regex.Pattern
    // And then the pattern is unescaped
    regex
      .replaceAll(leftBrace, backslash + leftBrace)
      .replaceAll(rightBrace, backslash + rightBrace)
      .replaceAll(openParenthesis, backslash + openParenthesis)
      .replaceAll(closeParenthesis, backslash + closeParenthesis)
  }

  /** Get all instances of a regular expression in a column.
    * @param column
    *   The column to search
    * @param regex
    *   The regex pattern to use.
    * @param index
    *   The index of the capture group to pull out.
    * @return
    *   An array of strings: The selected capture group for each match.
    */
  def regexp_extract_all(
      column: String,
      regex: String,
      index: Integer
  ): Column =
    expr(s"regexp_extract_all($column, '${doubleEscapeRegex(regex)}', $index)")

  // String.repeat is only implemented on some JVMs
  // Removing this has burned me twice.
  // Run the unit tests in the docker container before trying to remove it again.
  def repeat(token: String, count: Int): String = {
    (0 until count).map(_ => token).mkString
  }
}
