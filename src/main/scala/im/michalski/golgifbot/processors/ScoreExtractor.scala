package im.michalski.golgifbot.processors

import im.michalski.golgifbot.models.Score

trait ScoreExtractor {
  def extract(headline: String): Option[Score]
}

class ScoreExtractorImpl extends ScoreExtractor {

  private def toInt(s: String) = {
    try {
      Some(s.toInt)
    } catch {
      case e: Exception => None
    }
  }

  private def isSingleDigit(item: String) = item.length == 1 && toInt(item).isDefined

  override def extract(headline: String): Option[Score] = {
    headline.split("""\s""").map { item =>                       // Split - create a list of words / numbers
      if (isSingleDigit(item)) toInt(item) else None          // If part of a score (max 9) return number
    }.toList.filter(_.isDefined) match {
      case Some(a) :: Some(b) :: Nil => Some(Score(a, b))     // If at most two numbers - it's a Score
      case x => None
    }
  }
}
