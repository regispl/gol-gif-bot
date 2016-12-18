package im.michalski.golgifbot.processors

import im.michalski.golgifbot.models.{Link, MatchEvent}
import im.michalski.golgifbot.models.MatchEvent.{Goal, OtherWithLinks, Unknown}

import scala.util.matching.Regex

trait ContentProcessor {
  def process(selftext: String): Seq[MatchEvent]
}

class ContentProcessorImpl extends ContentProcessor {

  private def isMatchEvent(line: String): Boolean = {
    line.contains("streamable") ||
      line.contains("mixtape") ||
      line.contains("mp4")
  }

  def maybeExtractTime(line: String): Option[String] = {
    // e.g. 3', 21`, 103 min, 45 + 1'
    val timeRegex = """(\d\d?\d?(\s?\+\s?\d)?('|`|\s?min))""".r

    def parse(regex: Regex) = {
      regex
        .findFirstMatchIn(line)
        .map(m => m.group(1))
    }

    parse(timeRegex)
  }

  private def extractLinks(line: String): Seq[Link] = {
    // e.g. [0-1 Armstrong](https://streamable.com/jleo) | [AA (ft. Bertie Auld)] (https://streamable.com/825o)"
    val linkRegex = """\[([^\[]+)\]\s*\((http[^(]+)\)""".r
    val fallbackLinkRegex = """([^\[]+)\s*(http[^(]+)""".r

    def parse(regex: Regex) = {
      regex
        .findAllMatchIn(line)
        .map(m => Link(Option(m.group(1)).getOrElse(""), m.group(2)))
        .toList
    }

    def extract() = {
      val rawLinkRegex = """(http[^\]\)]+)""".r
      rawLinkRegex.findFirstMatchIn(line).map(m => m.group(0)).map(url => Link("", url)).toSeq
    }

    val links = List(
      parse(linkRegex),
      parse(fallbackLinkRegex),
      extract()
    )

    links.dropWhile(_.isEmpty).headOption.getOrElse(Seq.empty[Link])
  }

  private def isGoal(line: String, links: Seq[Link]): Boolean = {
    links.nonEmpty
  }

  override def process(selftext: String): Seq[MatchEvent] = {
    selftext
      .split("\n+")
      .filter(isMatchEvent)
      .map { line =>
        val processedLine = line
          .replaceAll("""\[\]\(\#[\w\d\-]+\)""", " ")       // Remove icons
          .replaceAll("""^\s+|\s+$""", "")                  // Strip whitespaces
          .replaceAll("""\s+""", " ")                       // Replace multi-whitespaces with single ones

        val maybeTime = maybeExtractTime(processedLine)
        val links = extractLinks(processedLine)

        if(isGoal(processedLine, links)) {
          Goal(maybeTime, links)
        } else if (links.nonEmpty) {
          OtherWithLinks(links)
        } else {
          Unknown(processedLine)
        }
      }.toList
  }
}
