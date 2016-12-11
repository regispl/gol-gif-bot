import im.michalski.golgifbot.formatters.WykopBlogFormatter
import im.michalski.golgifbot.processors._
import im.michalski.golgifbot.{RedditApiClient, RedditApiClientConfig}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

object GolGifBot extends App {
  // TODO: use parser (https://github.com/scopt/scopt
  val Username = args(0)
  val Password = args(1)
  val ClientId = args(2)
  val ClientSecret = args(3)

  val UserAgent = "GolGifBot/0.1 by RegisPL"

  val clientConfig = RedditApiClientConfig(Username, Password, ClientId, ClientSecret, UserAgent)
  val client = new RedditApiClient(clientConfig)

  val scoreExtractor = new ScoreExtractorImpl()
  val headlineProcessor = new HeadlineProcessorImpl(scoreExtractor)
  val contentProcessor = new ContentProcessorImpl()
  val processor = new MatchThreadProcessorImpl(headlineProcessor, contentProcessor)

  val formatter = new WykopBlogFormatter()

  val result = for {
    data <- client.getMatchThreadData
    processed = data.map(processor.process).filter(_.isDefined).map(_.get)
    formatted = processed.map(formatter.format)
  } yield formatted

  val output = Await.result(result, 5 seconds)

  output.foreach(println)

  client.shutdown()
}
