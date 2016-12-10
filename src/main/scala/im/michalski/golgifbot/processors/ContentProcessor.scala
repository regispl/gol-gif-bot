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

  private def maybeExtractTime(line: String): Option[Int] = {
    // e.g. 3', 21`, 103 min
    // FIXME: handle optional "+1" etc.
    val timeRegex = """(\d+)('|`|\s?min)""".r

    timeRegex
      .findFirstMatchIn(line)
      .map(m => m.group(1).toInt)
  }

  private def extractLinks(line: String): Seq[Link] = {
    // e.g. [0-1 Armstrong](https://streamable.com/jleo) | [AA (ft. Bertie Auld)] (https://streamable.com/825o)"
    val linkRegex = """\[([^\[]+)\]\s*\((http[^(]+)\)""".r
    val fallbackLinkRegex = """([^\[]+)\s*(http[^(]+)""".r

    def parse(regex: Regex) = {
      regex
        .findAllMatchIn(line)
        .map(m => Link(m.group(1), m.group(2)))
        .toList
    }

    val links = parse(linkRegex)

    if (links.nonEmpty) links else parse(fallbackLinkRegex)
  }

  private def isGoal(line: String, links: Seq[Link]): Boolean = {
    links.nonEmpty
  }

  override def process(selftext: String): Seq[MatchEvent] = {
    selftext
      .split("\n+")
      .filter(isMatchEvent)
      .map { line =>
        val maybeTime = maybeExtractTime(line)
        val links = extractLinks(line)

        if(isGoal(line, links)) {
          Goal(maybeTime, links)
        } else if (links.nonEmpty) {
          OtherWithLinks(links)
        } else {
          Unknown(line)
        }
      }.toList
  }
}
