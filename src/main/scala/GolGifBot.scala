import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import im.michalski.golgifbot.formatters.WykopBlogFormatter
import im.michalski.golgifbot.processors._
import im.michalski.golgifbot.clients.{RedditApiClient, RedditApiClientConfig, WykopApiClient, WykopApiClientConfig}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object GolGifBot extends App {
  // TODO: use parser (https://github.com/scopt/scopt
  val RedditUsername = args(0)
  val RedditPassword = args(1)
  val RedditClientId = args(2)
  val RedditClientSecret = args(3)

  val WykopLogin = args(4)
  val WykopApplicationKey = args(5)
  val WykopSecret = args(6)
  val WykopAccountKey = args(7)

  val UserAgent = "GolGifBot/0.1 by RegisPL"

  private implicit val system = ActorSystem()
  private implicit val executor = system.dispatcher
  private implicit val materializer = ActorMaterializer()

  val redditClientConfig = RedditApiClientConfig(RedditUsername, RedditPassword, RedditClientId, RedditClientSecret, UserAgent)
  val redditClient = new RedditApiClient(redditClientConfig)

  val scoreExtractor = new ScoreExtractorImpl()
  val headlineProcessor = new HeadlineProcessorImpl(scoreExtractor)
  val contentProcessor = new ContentProcessorImpl()
  val processor = new MatchThreadProcessorImpl(headlineProcessor, contentProcessor)

  val formatter = new WykopBlogFormatter()

  val result = for {
    data      <- redditClient.getMatchThreadData
    processed  = data.map(processor.process).filter(_.isDefined).map(_.get)
    formatted  = processed.map(formatter.format)
  } yield formatted

  val output = Await.result(result, 5 seconds)

  output.foreach(println)

  val wykopClientConfig = WykopApiClientConfig(WykopLogin, WykopApplicationKey, WykopSecret, WykopAccountKey)
  val wykopClient = new WykopApiClient(wykopClientConfig)

  output.headOption.map(wykopClient.publish)

  redditClient.shutdown().map(_ => wykopClient.shutdown()).onComplete(_ => system.terminate())
}
