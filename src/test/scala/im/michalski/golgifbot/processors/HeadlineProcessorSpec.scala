package im.michalski.golgifbot.processors

import im.michalski.golgifbot.models.Score
import org.scalatest._

class HeadlineProcessorSpec extends FlatSpec with Matchers {
  import HeadlineProcessorSpec._

  val scoreExtractor = new ScoreExtractorImpl()
  val processor = new HeadlineProcessorImpl(scoreExtractor)

  "HeadlineProcessor" should "correctly parse all the known headline formats" in {
    val output = input.map(processor.process)

    output.zip(expected).foreach { pair =>
      pair._1 should === (pair._2)
    }
  }
}

object HeadlineProcessorSpec {
  val input = Seq(
    "Post Match Thread:Feyenoord 0-1 Fenerbahçe",
    "Post Match Thread: Zorya Luhansk 0-2 Manchester United [Europa League - Group A]",
    "Post Match Thread: Kashima Antlers 2–1 Auckland City [Club World Cup]",
    "Post-Match Thread: Club Brugge 0-2 FC København",
    "Post-Match Thread: Olympique Lyonnais 0-0 Sevilla",
    "Post Match Thread: Bayer Leverkusen 3-0 AS Monaco",
    "Post Match Thread: Legia Warszawa 1-0 Sporting CP [UEFA Champions League]",
    "Post Match Thread: FC Porto 5-0 Leicester City[Uefa Champions League]",
    "Post Match Thread: PSV 0-0 Rostov [UEFA Champions League",
    "Post Match Thread: Bayern München 1-0 Atlético Madrid [UEFA Champions League]",
    "Post Match Thread: AFC Bournemouth 4 - 3 Liverpool",
    "Post Match Thread: Barcelona 1-1 Real Madrid [Primera División]",
    "[POST MATCH THREAD]- CRYSTAL PALACE 3-0 SOUTHAMPTON",
    "[POST MATCH THREAD]WEST BROM 3-1 WATFORD",
    "Post Match Thread: Stoke City 2 Burnley 0",
    "Post Match Thread: Sunderland 2 - 1 Leicester",
    "Post Match Thread Tottenham Hotspur vs Swansea City 5-0",
    "Post Match Thread: Manchester City 1-3 Chelsea [Premier League]",
    "[POST MATCH THREAD] Mainz 1 - Bayern 3 (Bundesliga)",
    "Post match Thread: Toronto FC 5 - Montreal Impact 2",
    "Post Match Thread: Real Madrid 6- Cultural Leonesa 1",
    "Post Match Thread: Shalke 04 Gelsenkirchen 9 Hannover 96 1",
    "Post Match Thread: QPR 2 : 2 1. FC Kaiserslautern",
    "[Post Match Thread] Liverpool 2 - 0 Leeds United (EFL Cup Quarter-Finals)",
    "Post Match Thread Arsenal vs Jakieś Ogony 11-0",
    "[Post-Match Thread] FC Ingolstadt 1-0 RB Leipzig",
    "[Post Match Thread] 1. FC Köln 1 - 1 Borussia Dortmund"
  )

  val expected = Seq(
    ("Feyenoord 0 - 1 Fenerbahçe", Some(Score(0,1))),
    ("Zorya Luhansk 0 - 2 Manchester United Europa League Group A", Some(Score(0,2))),
    ("Kashima Antlers 2 - 1 Auckland City Club World Cup", Some(Score(2,1))),
    ("Club Brugge 0 - 2 FC København", Some(Score(0,2))),
    ("Olympique Lyonnais 0 - 0 Sevilla",  Some(Score(0,0))), 
    ("Bayer Leverkusen 3 - 0 AS Monaco",  Some(Score(3,0))), 
    ("Legia Warszawa 1 - 0 Sporting CP UEFA Champions League", Some(Score(1,0))),
    ("FC Porto 5 - 0 Leicester City Uefa Champions League", Some(Score(5,0))),
    ("PSV 0 - 0 Rostov UEFA Champions League", Some(Score(0,0))),
    ("Bayern München 1 - 0 Atlético Madrid UEFA Champions League", Some(Score(1,0))),
    ("AFC Bournemouth 4 - 3 Liverpool", Some(Score(4,3))),
    ("Barcelona 1 - 1 Real Madrid Primera División", Some(Score(1,1))),
    ("CRYSTAL PALACE 3 - 0 SOUTHAMPTON",  Some(Score(3,0))), 
    ("WEST BROM 3 - 1 WATFORD", Some(Score(3,1))),
    ("Stoke City 2 Burnley 0", Some(Score(2,0))),
    ("Sunderland 2 - 1 Leicester", Some(Score(2,1))),
    ("Tottenham Hotspur Swansea City 5 - 0", Some(Score(5,0))),
    ("Manchester City 1 - 3 Chelsea Premier League", Some(Score(1,3))),
    ("Mainz 1 Bayern 3 Bundesliga", Some(Score(1,3))),
    ("Toronto FC 5 Montreal Impact 2", Some(Score(5,2))),
    ("Real Madrid 6 Cultural Leonesa 1", Some(Score(6,1))),
    ("Shalke 04 Gelsenkirchen 9 Hannover 96 - 1", Some(Score(9,1))),    // FIXME!
    ("QPR 2 - 2 FC Kaiserslautern", Some(Score(2,2))),
    ("Liverpool 2 - 0 Leeds United EFL Cup Quarter Finals", Some(Score(2,0))),
    ("Arsenal Jakieś Ogony 11 - 0", None),
    ("FC Ingolstadt 1 - 0 RB Leipzig", Some(Score(1, 0))),
    ("FC Köln 1 - 1 Borussia Dortmund", Some(Score(1, 1)))
  )
}