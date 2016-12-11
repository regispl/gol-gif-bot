package im.michalski.golgifbot.formatters
import im.michalski.golgifbot.models.MatchEvent.Goal
import im.michalski.golgifbot.models.{Link, MatchEvent, MatchThreadData}


class WykopBlogFormatter extends Formatter {

  val tags = Seq("#golgifbottest")
  val botOwner = "@Regis86"
  val botInfo =
    s"""**WPIS TESTOWY!**
       |To jest / będzie bot użytkownika $botOwner wrzucający gole z Reddita
       |Dodaj do czarnej listy tag ${tags.mkString(",")}, jeśli nie chcesz oglądać tych wpisów
       |Docelowo, jeśli bot się sprawdzi i nie będzie robił głupich rzeczy, wpisy będą wrzucane pod tag #golgif automatycznie
       |(a póki co, zanim zintegruję się z API Wykopu, wrzucam ręcznie to co pobiorę :D )""".stripMargin

  private def printLine(maybeTime: Option[String], link: Link) = maybeTime match {
    case Some(time) if !link.description.contains(time) => s"$time ${link.description}: ${link.link}"
    case _ => s"${link.description}: ${link.link}"
  }

  private def printEvent(event: MatchEvent) = event match {
    // FIXME: distinguish between goals and other events with links
    case event: Goal => s"${event.links.map(link => printLine(event.time, link)).mkString(" | ")}\n"
    case _ => ""
  }

  override def format(matchData: MatchThreadData): String = {
    s"""${matchData.headline}
      |
      |${matchData.events.map(printEvent).mkString("")}
      |
      |$botInfo""".stripMargin
  }
}