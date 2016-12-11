package im.michalski.golgifbot.models

case class RawMatchThreadData(id: String, title: String, selftext: String)

case class Score(home: Int, away: Int)

case class Link(description: String, link: String)

sealed trait MatchEvent
object MatchEvent {
  trait Links { def links: Seq[Link] }

  case class Goal(time: Option[String], override val links: Seq[Link]) extends MatchEvent with Links
  case class OtherWithLinks(override val links: Seq[Link]) extends MatchEvent with Links
  case class Unknown(entry: String) extends MatchEvent
}

case class MatchThreadData(headline: String, score: Option[Score], events: Seq[MatchEvent])
