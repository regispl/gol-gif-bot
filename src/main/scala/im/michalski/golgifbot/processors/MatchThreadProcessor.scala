package im.michalski.golgifbot.processors

import im.michalski.golgifbot.models.{MatchThreadData, RawMatchThreadData}

trait MatchThreadProcessor {
  def process(data: RawMatchThreadData): Option[MatchThreadData]
}

class MatchThreadProcessorImpl(headlineProcessor: HeadlineProcessor,
                               contentProcessor: ContentProcessor) extends MatchThreadProcessor {
  override def process(data: RawMatchThreadData): Option[MatchThreadData] = {
    val events = contentProcessor.process(data.selftext)
    val (headline, score) = headlineProcessor.process(data.title)

    if(events.nonEmpty) Some(MatchThreadData(headline, score, events)) else None
  }
}