import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import im.michalski.golgifbot.formatters.WykopBlogFormatter
import im.michalski.golgifbot.processors._
import im.michalski.golgifbot.clients.{RedditApiClient, RedditApiClientConfig}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

object GolGifBot extends App {
  // TODO: use parser (https://github.com/scopt/scopt
  val RedditUsername = args(0)
  val RedditPassword = args(1)
  val RedditClientId = args(2)
  val RedditClientSecret = args(3)

  /*
  val WykopApplicationKey = args(4)
  val WykopSecret = args(5)
  val WykopAccountKey = args(6)
  */

  val UserAgent = "GolGifBot/0.1 by RegisPL"

  private implicit val system = ActorSystem()
  private implicit val executor = system.dispatcher
  private implicit val materializer = ActorMaterializer()

  val clientConfig = RedditApiClientConfig(RedditUsername, RedditPassword, RedditClientId, RedditClientSecret, UserAgent)
  val client = new RedditApiClient(clientConfig)

  val scoreExtractor = new ScoreExtractorImpl()
  val headlineProcessor = new HeadlineProcessorImpl(scoreExtractor)
  val contentProcessor = new ContentProcessorImpl()
  val processor = new MatchThreadProcessorImpl(headlineProcessor, contentProcessor)

  val formatter = new WykopBlogFormatter()

  val result = for {
    data      <- client.getMatchThreadData
    processed  = data.map(processor.process).filter(_.isDefined).map(_.get)
    formatted  = processed.map(formatter.format)
  } yield formatted

  val output = Await.result(result, 5 seconds)

  output.foreach(println)

  client.shutdown().onComplete(_ => system.terminate())
}
