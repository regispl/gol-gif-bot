package im.michalski.golgifbot.formatters
import im.michalski.golgifbot.models.MatchEvent.{Goal, OtherWithLinks, Unknown}
import im.michalski.golgifbot.models.{FormattedMatchData, Link, MatchEvent, MatchThreadData}


class WykopBlogFormatter extends Formatter {

  val botName = "@GolGifBot"
  val botOwner = "@Regis86"
  val botTags = Seq("#golgifbottest")
  val botInfo =
    s"""**WPIS TESTOWY!**
       |To jest / będzie bot $botName użytkownika $botOwner wrzucający gole z Reddita.
       |Dodaj do czarnej listy tag ${botTags.mkString(",")}, jeśli nie chcesz oglądać tych wpisów.
       |Docelowo, jeśli bot się sprawdzi i nie będzie robił głupich rzeczy, wpisy będą wrzucane pod tag #golgif automatycznie
       |(a póki co uruchamiam skrypt ręcznie - w miarę możliwości - raz dziennie, wieczorem)""".stripMargin

  private def printLine(maybeTime: Option[String], link: Link) = maybeTime match {
    case Some(time) if !link.description.contains(time) => s"$time ${link.description}: ${link.link}"
    case _ => s"${link.description}: ${link.link}"
  }

  private def printEvent(event: MatchEvent) = event match {
    // FIXME: distinguish between goals and other events with links
    case event: Goal => s"${event.links.map(link => printLine(event.time, link)).mkString(" | ")}\n"
    case event: OtherWithLinks => s"${event.links.map(link => printLine(None, link)).mkString(" | ")} [Nie jestem pewien, czy to bramka, ale jest link, więc wrzucam - dop. GolGifBot]\n"
    case event: Unknown => s"${event.entry} [Wydaje mi się, że tu może coś być, ale nie potrafiłem dopasować tego tekstu do żadnego ze znanych mi formatów - dop. GolGifBot]\n"
  }

  override def format(matchData: MatchThreadData): FormattedMatchData = {
    val text =
      s"""${matchData.headline}
      |
      |${matchData.events.map(printEvent).mkString("")}
      |
      |$botInfo""".stripMargin

    FormattedMatchData(matchData.id, matchData.headline, text)
  }
}