package im.michalski.golgifbot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.EitherT
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import im.michalski.golgifbot.clients.{RedditApiClient, RedditApiClientConfig, WykopApiClient, WykopApiClientConfig}
import im.michalski.golgifbot.config.Config
import im.michalski.golgifbot.formatters.WykopBlogFormatter
import im.michalski.golgifbot.models._
import im.michalski.golgifbot.processors._

import scala.concurrent.duration._


class GolGifBot(config: Config) extends LazyLogging {
  import cats.implicits._

  private lazy implicit val system = ActorSystem()
  private lazy implicit val executor = system.dispatcher
  private lazy implicit val materializer = ActorMaterializer()

  val UserAgent = "GolGifBot/0.1.0"

  val redditClientConfig = RedditApiClientConfig(config, UserAgent)
  val wykopClientConfig = WykopApiClientConfig(config, UserAgent)

  val redditClient = new RedditApiClient(redditClientConfig)
  val wykopClient = new WykopApiClient(wykopClientConfig)

  val scoreExtractor = new ScoreExtractorImpl()
  val headlineProcessor = new HeadlineProcessorImpl(scoreExtractor)
  val contentProcessor = new ContentProcessorImpl()
  val processor = new MatchThreadProcessorImpl(headlineProcessor, contentProcessor)

  val formatter = new WykopBlogFormatter()

  def run: Either[Problem, List[PublishingResult]] = {
    val processing = for {
      data          <- redditClient.getMatchThreadData
      notPublished   = notPublishedYet(data)
      processed      = processAndPickValid(notPublished)
      formatted      = processed.map(formatter.format)
      _              = formatted.foreach(debugTee)
      response      <- publish(formatted)
      _              = logger.info(s"[IMPORTANT!] Newest entry ID: ${data.headOption.map(_.id)}")
    } yield response

    val maybeResult = processing.value.recoverWith {
      case e: Exception => IO.pure(Left(Problem(s"Unexpected error: ${e.getMessage}")))
    }.unsafeRunTimed(30 seconds)

    val result = maybeResult match {
      case Some(res) => res
      case None      => Left(Problem("Running IO synchronously timed out!"))
    }

    shutdownAll()
    result
  }

  def notPublishedYet(data: List[RawMatchThreadData]) = config.lastPublishedId match {
    case Some(id) => data.takeWhile(_.id != id)
    case None     => data
  }

  def processAndPickValid(data: List[RawMatchThreadData]): List[MatchThreadData] = {
    data.map(processor.process).filter(_.isDefined).map(_.get)
  }

  def publish(data: List[FormattedMatchData]): EitherT[IO, Problem, List[PublishingResult]] = {
    import im.michalski.golgifbot.utils.Transmogrifiers._

    data.map { fmd =>
      if (!config.dryRun) wykopClient.publish(fmd.text)
      else EitherT[IO, Problem, PublishingResult](IO.pure(Right(DryRun)))
    }.magic
  }


  def shutdownAll() = {
    redditClient.shutdown().map(_ => wykopClient.shutdown()).onComplete(_ => system.terminate())
  }

  private def debugTee(entry: FormattedMatchData) = {
    logger.info(s"Formatted entry for '${entry.headline}' (ID: ${entry.id})")
    logger.debug(s"\n${entry.text}")
  }
}

object GolGifBot extends App with LazyLogging {
  import im.michalski.golgifbot.config.Parser._

  private def handleResult(result: Either[Problem, List[PublishingResult]]): Unit = result match {
    case Right(value) => logger.info(s"Successfully published entries with IDs: ${value.mkString(", ")}")
    case Left(Problem(msg)) => logger.error(msg)
  }

  util.Properties.setProp("scala.time", "on")

  parser.parse(args, Config()) match {
    case Some(config)   => handleResult(new GolGifBot(config).run)
    case None           => parser.showUsageAsError()
  }
}
