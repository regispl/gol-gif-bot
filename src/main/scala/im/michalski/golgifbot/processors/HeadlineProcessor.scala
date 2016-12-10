package im.michalski.golgifbot.processors

import im.michalski.golgifbot.models.Score

trait HeadlineProcessor {
  def process(title: String): (String, Option[Score])
}

class HeadlineProcessorImpl(scoreExtractor: ScoreExtractor) extends HeadlineProcessor {

  override def process(title: String): (String, Option[Score]) = {
    val headline = title
      .replaceAll("""\d\.""", "")                                  // Pre-filtering: special case, e.g. "1. FC Kaiserslautern"
      .replaceAll("""(?i)vs(\.)?""", "-")                          // Pre-filtering: replace "vs" with dash
      .replaceAll("""[^\u00BF-\u1FFF\u2C00-\uD7FF\w\d\s]""", " ")  // Leave only letters (UTF-8), digits and whitespaces
      .replaceAll("""(\d)\s+(\d)""", "$1 - $2")                    // Fix score if possible: 0 0 -> 0 - 0
      .replaceFirst("(?i)post match thread", "")                   // Remove standard post prefix
      .replaceAll("""^\s+|\s+$""", "")                             // Strip whitespaces
      .replaceAll("""\s+""", " ")                                  // Replace multi-whitespaces with single ones

    (headline, scoreExtractor.extract(headline))
  }
}
