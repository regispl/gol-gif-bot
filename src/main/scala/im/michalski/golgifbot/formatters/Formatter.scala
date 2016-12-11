package im.michalski.golgifbot.formatters

import im.michalski.golgifbot.models.MatchThreadData

trait Formatter {
  def format(matchData: MatchThreadData): String
}
