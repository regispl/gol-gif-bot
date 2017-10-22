package im.michalski.golgifbot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.EitherT
import com.typesafe.scalalogging.LazyLogging
import im.michalski.golgifbot.clients.{RedditApiClient, RedditApiClientConfig, WykopApiClient, WykopApiClientConfig}
import im.michalski.golgifbot.config.Config
import im.michalski.golgifbot.formatters.WykopBlogFormatter
import im.michalski.golgifbot.models.{FormattedMatchData, Problem, RawMatchThreadData}
import im.michalski.golgifbot.processors._

import scala.concurrent.Future


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

  def notPublishedYet(data: List[RawMatchThreadData]) = config.lastPublishedId match {
    case Some(id) => data.takeWhile(_.id != id)
    case None => data
  }

  def processAndPickValid(data: List[RawMatchThreadData]) = {
    data.map(processor.process).filter(_.isDefined).map(_.get)
  }

  def publish(data: List[FormattedMatchData]) = EitherT[Future, Problem, List[Int]] {
    Future.sequence(data.map { fmd =>
      if (!config.dryRun) {
        wykopClient.publish(fmd.text)
      } else {
        EitherT[Future, Problem, Int](Future.successful(Right(-1)))
      }
    }.map(_.value)).map(_.sequence)
  }

  def run: Future[Either[Problem, List[Int]]] = {
    val result = for {
      data          <- redditClient.getMatchThreadData
      _              = logger.info(s"[IMPORTANT!] Newest entry ID: ${data.headOption.map(_.id)}")
      notPublished   = notPublishedYet(data)
      processed      = processAndPickValid(notPublished)
      formatted      = processed.map(formatter.format)
      _              = formatted.foreach(debugTee)
      response      <- publish(formatted)
    } yield response

    val fut = result.value.recoverWith {
      case e: Exception => Future.successful(Left(Problem(s"Unexpected error: ${e.getMessage}")))
    }

    fut.onComplete(_ => redditClient.shutdown().map(_ => wykopClient.shutdown()).onComplete(_ => system.terminate()))

    fut
  }

  private def debugTee(entry: FormattedMatchData) = {
    logger.info(s"Formatted entry for '${entry.headline}' (ID: ${entry.id})")
    logger.debug(s"\n${entry.text}")
  }
}

object GolGifBot extends App with LazyLogging {
  import im.michalski.golgifbot.config.Parser._
  import scala.concurrent.ExecutionContext.Implicits.global

  parser.parse(args, Config()) match {
    case Some(config)   => new GolGifBot(config).run.foreach(println)
    case None           => parser.showUsageAsError()
  }
}
