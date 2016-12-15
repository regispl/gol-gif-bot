package im.michalski.golgifbot.formatters

import im.michalski.golgifbot.models.{FormattedMatchData, MatchThreadData}

trait Formatter {
  def format(matchData: MatchThreadData): FormattedMatchData
}
