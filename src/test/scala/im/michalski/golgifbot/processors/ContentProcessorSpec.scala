package im.michalski.golgifbot.processors

import im.michalski.golgifbot.models.Link
import im.michalski.golgifbot.models.MatchEvent.Goal
import org.scalatest._

class ContentProcessorSpec extends FlatSpec with Matchers {
  import ContentProcessorSpec._

  val processor = new ContentProcessorImpl()

  "ContentProcessor" should "correctly parse all the known selftext formats" in {
    val output = input.map(processor.process).toList

    output.zip(expected).foreach { pair =>
      pair._1 should === (pair._2)
    }
  }

  it should "correctly handle early line breaks" in {
    val output = processor.process(withEarlyLineBreak)

    output should === (expectedWithEarlyLineBreak)
  }
}

object ContentProcessorSpec {
  val input = Seq(
    "#FT' [](#sprite1-p9) Real Madrid 3 - 2 Deportivo La Coruña [](#sprite1-p256)\n\n______________\n**Venue**: Estadio Santiago Bernabéu (Madrid)\n\n**TV**:[Find your channel here] (http://www.livesoccertv.com/match/2010501/real-madrid-vs-deportivo-la-coruna/)\n\n**Referee**: Santiago Jaime Latre\n\n_____\n\n[r/soccerstreams] (https://www.reddit.com/r/soccerstreams/)\n__________\n\n" +
      "[](#icon-notes-big) **Line Up**\n\n[](#sprite1-p9) **Real Madrid** Starting XI (4-3-3): \n\nNavas; Danilo, Pepe, Sergio Ramos, Nacho; Kroos, Casemiro, Isco; James, Morata, Asensio.\n\n**Subs**: Carvajal, Varane, Marcelo, Casilla, Lucas Vazquez, Mariano Diaz, Kovacic.\n\n---------\n\n[](#sprite1-p256)\n **Deportivo La Coruna** Starting XI (4-2-3-1):\n\nTyton; Juanfran, Albentosa, Sidnei, Fernando Navarro; Guilherme, Broges; Carles Gil, Emre Colak, Babel; Andone.\n\n**Subs**: Lux, Mosquera, Joselu, Moreno, Arribas, Luisinho, Fajr.\n_____________________________________\n" +
      "[](#icon-net-big) **Match Events:**\n\n**25'**  [](#icon-yellow) Yellow Card Celso Borges Mora \n\n**45 + 1'** [](#icon-yellow) Yellow Card Ryan Babel \n\n**50'** [](#icon-ball) Goal Álvaro Borja Morata Martín [Watch] (https://streamable.com/80fe) Credit to /u/TomasRoncero\n\n**58'** [](#icon-sub) Substitution [](#icon-down) Emre Çolak José [](#icon-up) Luis Sanmartín Mato \n\n**59'** [](#icon-yellow) Yellow Card Raúl Albentosa Redal \n\n**63'** [](#icon-ball) Goal José Luis Sanmartín Mato [Watch] (https://streamable.com/ows9) Credit to /u/rodrigoelcrack\n\n**65'** [](#icon-ball) Goal José Luis Sanmartín Mato [Watch] (https://streamable.com/m3b1) Credit to /u/rodrigoelcrack\n\n**68'** [](#icon-sub) Substitution [](#icon-down) Marco Asensio Willemsen [](#icon-up) Lucas Vázquez Iglesias \n\n**70'** [](#icon-yellow) Yellow Card Álvaro Borja Morata Martín\n\n**72'** [](#icon-sub) Substitution [](#icon-down) Francisco Román Alarcón Suárez [](#icon-up) Mariano Díaz Mejía \n\n**75'** [](#icon-sub) Substitution [](#icon-down) Florin Andone [](#icon-up) Pedro Mosquera Parada \n\n**81'** [](#icon-sub) Substitution [](#icon-down) Danilo Luiz da Silva [](#icon-up) Marcelo Vieira da Silva Júnior \n\n**82'** [](#icon-sub) Substitution [](#icon-down) Ryan Babel [](#icon-up) Fayçal Fajr \n\n**84'** [](#icon-ball) Goal Mariano Díaz Mejía [Watch] (https://streamable.com/d0xf) Credit to /u/rodrigoelcrack\n\n**88'** [](#icon-yellow)\n Yellow Card Sergio Ramos García \n\n**88'** [](#icon-yellow)\nYellow Card Keilor Navas Gamboa \n\n**90+2'** RAMOSSSSSSSSSS GOALLLLL [Watch] (https://streamable.com/17fx) Credit to /u/rodrigoelcrack\n\n**105'** [Late Scorer] (https://streamable.com/alefuks) Credit to someone\nYellow Card Keilor Navas Gamboa ",
    "Rudnevs 28' (no gif yet)\n\n[Reus 90' + 1](https://streamable.com/auts) (gif by /u/Bernd_S)",
    "[18' Robben](https://streamable.com/iil6) (gif by /u/Thelawgiver4)\n\n[21' Lewandowski](https://streamable.com/kbmq) (gif by /u/Moujahideen)\n\n[58' Lewandowski](https://streamable.com/lioc) (gif by /u/Bernd_S)\n\n[76' Müller](https://streamable.com/7rde) (gif by /u/Bernd_S)\n\n[86' Costa](https://streamable.com/81fs) (gif by /u/Bernd_S)\n",
    "[Lukaku 1-0](https://streamable.com/mftk)\n\n[Okaka 1-1](https://streamable.com/i24g)\n\n[Prödl 2-1](https://streamable.com/zbh6)\n\n[Okaka 3-1](https://streamable.com/t2hv)\n\n[Lukaku 2-3](https://streamable.com/cxpj)",
    "0-1 Suarez https://streamable.com/h4v7\n\n0-2 Messi https://streamable.com/oool\n\n0-3 Messi https://streamable.com/58i1",
    "#**FT: Partick Thistle [](#sprite1-p327) [1-4](#bar-3-white) [](#sprite1-p18) Celtic**\n\n--------\n\n**Kick-off time:**  7:45pm\n\n**Venue:** Firhill Stadium, Glasgow\n\n" +
      "**Referee:** C. Thomson ^^^^^^^*wanker*\n\n--------\n\n[](#icon-notes-big) **LINE-UPS**\n\n**[](#sprite1-p327) Partick Thistle**\n\nT. Černý, \n\nZ. Gordon [](#icon-yellow), A. Barton, C. Booth, L. Lindsay [](#icon-ball), \n\nD. Amoo [](#icon-down), C. Erskine [](#icon-down),\n\nS. Welsh, C. Elliot, A. Osman, \n\nK. Doolan [](#icon-down).\n\n**Subs:** T. Stuckmann, D. Devine, R. Edwards [](#icon-up), D. McDaid, D. Wilson, S. Lawless [](#icon-up), A. Azeez [](#icon-up)[](#icon-yellow).\n\n^____________________________\n\n**[](#sprite1-p18) Celtic**\n\nC. Gordon,\n\nC. Gamboa, M. Lustig, E. Sviatchenko, E. Izaguirre,\n\nS. Brown, S. Armstrong [](#icon-ball)[](#icon-ball), \n\nP. Roberts [](#icon-down), T. Rogić [](#icon-down), G. Mackay-Steven [](#icon-down), \n\nL. Griffiths [](#icon-ball).\n\n**Subs:** D. de Vries, D. Boyata, K. Touré, C. McGregor [](#icon-up)[](#icon-ball), N. Bitton, R. Christie [](#icon-up), M. Dembélé [](#icon-up).\n\n------------\n\n" +
      "[](#icon-net-big) **MATCH HIGHLIGHTS:**\n\n19’ [John Kennedy reacts to Griffiths’ free kick](https://streamable.com/dv9q)\n\n23’ [Griffiths chance](https://streamable.com/glnn)\n\n**39’ [](#icon-ball) [0-1 Armstrong](https://streamable.com/jleo) | [AA (ft. Bertie Auld)](https://streamable.com/825o)**\n\n^____________________________\n\n**49’ [](#icon-ball) [0-2 Armstrong](https://streamable.com/j0fo) | [AA](https://streamable.com/m980)**\n\n**50’ [](#icon-ball) [0-3 Griffiths](https://streamable.com/1l3u) | [AA](https://streamable.com/u9ne)**\n\n**61’ [](#icon-ball) [1-3 Lindsay](https://streamable.com/i8p1)**\n\n64’ [](#icon-flag) [Thistle disallowed goal](https://streamable.com/jmb5)\n\n67’ [Griffiths chance](https://streamable.com/yvmp)\n\n80’ [](#icon-yellow) [Azeez booked for diving](https://streamable.com/btrs)\n\n**82’ [](#icon-ball) [1-4 McGregor](https://streamable.com/y3pt) | [AA](https://streamable.com/g4gr)**\n\n^____________________________\n\n[Griffiths/Armstrong Post Match Interview](https://streamable.com/h4uz)\n\n[Brendan Rodgers Post Match Interview](https://streamable.com/41eb)\n\n----\n\n[](#icon-notes)  [BBC Report](http://www.bbc.co.uk/sport/football/38178476)\n\n----\n\n[](#icon-alien)  /r/PartickThistleFC/  /r/ScottishFootball  /r/CelticFC",
    "**43'** **GOAL** [https://my.mixtape.moe/uuqroa.mp4] for [](#sprite1-p4) **Chelsea** by **DIEGO COSTA**! [](#icon-ball-big)"
  )

  val expected = Seq(
    List(
      Goal(Some("50'"), List(Link("Watch", "https://streamable.com/80fe"))),
      Goal(Some("63'"), List(Link("Watch", "https://streamable.com/ows9"))),
      Goal(Some("65'"), List(Link("Watch", "https://streamable.com/m3b1"))),
      Goal(Some("84'"), List(Link("Watch", "https://streamable.com/d0xf"))),
      Goal(Some("90+2'"), List(Link("Watch", "https://streamable.com/17fx"))),
      Goal(Some("105'"),List(Link("Late Scorer", "https://streamable.com/alefuks")))
    ),
    List(
      Goal(Some("90'"), List(Link("Reus 90' + 1", "https://streamable.com/auts")))
    ),
    List(
      Goal(Some("18'"), List(Link("18' Robben", "https://streamable.com/iil6"))),
      Goal(Some("21'"), List(Link("21' Lewandowski", "https://streamable.com/kbmq"))),
      Goal(Some("58'"), List(Link("58' Lewandowski", "https://streamable.com/lioc"))),
      Goal(Some("76'"), List(Link("76' Müller", "https://streamable.com/7rde"))),
      Goal(Some("86'"), List(Link("86' Costa", "https://streamable.com/81fs")))
    ),
    List(
      Goal(None, List(Link("Lukaku 1-0", "https://streamable.com/mftk"))),
      Goal(None, List(Link("Okaka 1-1", "https://streamable.com/i24g"))),
      Goal(None, List(Link("Prödl 2-1", "https://streamable.com/zbh6"))),
      Goal(None, List(Link("Okaka 3-1", "https://streamable.com/t2hv"))),
      Goal(None, List(Link("Lukaku 2-3", "https://streamable.com/cxpj")))),
    List(
      Goal(None, List(Link("0-1 Suarez ", "https://streamable.com/h4v7"))),
      Goal(None, List(Link("0-2 Messi ", "https://streamable.com/oool"))),
      Goal(None, List(Link("0-3 Messi ", "https://streamable.com/58i1")))),
    List(
      Goal(None, List(Link("John Kennedy reacts to Griffiths’ free kick", "https://streamable.com/dv9q"))),
      Goal(None, List(Link("Griffiths chance", "https://streamable.com/glnn"))),
      Goal(None, List(Link("0-1 Armstrong", "https://streamable.com/jleo"), Link("AA (ft. Bertie Auld)", "https://streamable.com/825o"))),
      Goal(None, List(Link("0-2 Armstrong", "https://streamable.com/j0fo"), Link("AA", "https://streamable.com/m980"))),
      Goal(None, List(Link("0-3 Griffiths", "https://streamable.com/1l3u"), Link("AA", "https://streamable.com/u9ne"))),
      Goal(None, List(Link("1-3 Lindsay", "https://streamable.com/i8p1"))),
      Goal(None, List(Link("Thistle disallowed goal", "https://streamable.com/jmb5"))),
      Goal(None, List(Link("Griffiths chance", "https://streamable.com/yvmp"))),
      Goal(None, List(Link("Azeez booked for diving", "https://streamable.com/btrs"))),
      Goal(None, List(Link("1-4 McGregor", "https://streamable.com/y3pt"), Link("AA", "https://streamable.com/g4gr"))),
      Goal(None, List(Link("Griffiths/Armstrong Post Match Interview", "https://streamable.com/h4uz"))),
      Goal(None, List(Link("Brendan Rodgers Post Match Interview", "https://streamable.com/41eb")))
    ),
    List(
      Goal(Some("43'"), List(Link("", "https://my.mixtape.moe/uuqroa.mp4")))
    )
  )

  val withEarlyLineBreak = "**79'** [](#icon-ball) [**Goal!  Watford 1, Everton 0. Troy Deeney \\(Watford\\) right footed shot from the centre of the box to the top right corner. Assisted by Stefano Okaka.**\n](https://streamable.com/iyd75)\n**82'** [](#icon-sub) Substitution, Everton. Yannick Bolasie replaces Gylfi Sigurdsson."

  val expectedWithEarlyLineBreak = List(
    Goal(Some("79'"),List(Link("**Goal! Watford 1, Everton 0. Troy Deeney \\(Watford\\) right footed shot from the centre of the box to the top right corner. Assisted by Stefano Okaka.**", "https://streamable.com/iyd75")))
  )
}
