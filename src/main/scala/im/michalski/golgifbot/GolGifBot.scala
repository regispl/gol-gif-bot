package im.michalski.golgifbot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import im.michalski.golgifbot.clients.{RedditApiClient, RedditApiClientConfig, WykopApiClient, WykopApiClientConfig}
import im.michalski.golgifbot.formatters.WykopBlogFormatter
import im.michalski.golgifbot.processors._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


object GolGifBot extends App {
  import Parser._

  private lazy implicit val system = ActorSystem()
  private lazy implicit val executor = system.dispatcher
  private lazy implicit val materializer = ActorMaterializer()

  val UserAgent = "GolGifBot/0.1.0"

  parser.parse(args, Config()) match {
    case Some(config) => run(config)
    case None =>
  }

  def run(config: Config) = {
    val redditClientConfig = RedditApiClientConfig(
      config.redditUsername,
      config.redditPassword,
      config.redditClientId,
      config.redditClientSecret,
      UserAgent)

    val redditClient = new RedditApiClient(redditClientConfig)

    val scoreExtractor = new ScoreExtractorImpl()
    val headlineProcessor = new HeadlineProcessorImpl(scoreExtractor)
    val contentProcessor = new ContentProcessorImpl()
    val processor = new MatchThreadProcessorImpl(headlineProcessor, contentProcessor)

    val formatter = new WykopBlogFormatter()

    val result = for {
      data <- redditClient.getMatchThreadData
      _ = println(s"===> NEWEST ENTRY: ${data.headOption.map(_.id)}")
      fresh = data.takeWhile(_.id != config.lastPublishedId)
      processed = fresh.map(processor.process).filter(_.isDefined).map(_.get)
      formatted = processed.map(formatter.format)
    } yield formatted

    val output = Await.result(result, 5 seconds)

    output.foreach(println)

    val wykopClientConfig = WykopApiClientConfig(
      config.wykopLogin,
      config.wykopApplicationKey,
      config.wykopSecret,
      config.wykopAccountKey)

    val wykopClient = new WykopApiClient(wykopClientConfig)

    output.map(wykopClient.publish).map(fut => Await.result(fut, 5 seconds))

    redditClient.shutdown().map(_ => wykopClient.shutdown()).onComplete(_ => system.terminate())
  }
}

object Parser {

  case class Config(redditUsername: String = "",
                    redditPassword: String = "",
                    redditClientId: String = "",
                    redditClientSecret: String = "",
                    wykopLogin: String = "",
                    wykopApplicationKey: String = "",
                    wykopSecret: String = "",
                    wykopAccountKey: String = "",
                    lastPublishedId: String = "")

  val parser = new scopt.OptionParser[Config]("golgifbot") {
    head("GolGifBot", "0.1.0")

    opt[String]("reddit-username").required().action((x, c) =>
      c.copy(redditUsername = x))

    opt[String]("reddit-password").required().action((x, c) =>
      c.copy(redditPassword = x))

    opt[String]("reddit-client-id").required().action((x, c) =>
      c.copy(redditClientId = x))

    opt[String]("reddit-client-secret").required().action((x, c) =>
      c.copy(redditClientSecret = x))

    opt[String]("wykop-login").required().action((x, c) =>
      c.copy(wykopLogin = x))

    opt[String]("wykop-application-key").required().action((x, c) =>
      c.copy(wykopApplicationKey = x))

    opt[String]("wykop-secret").required().action((x, c) =>
      c.copy(wykopSecret = x))

    opt[String]("wykop-account-key").required().action((x, c) =>
      c.copy(wykopAccountKey = x))

    opt[String]("last-published-id").required().action((x, c) =>
      c.copy(lastPublishedId = x))
  }
}