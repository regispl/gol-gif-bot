package im.michalski.golgifbot

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, RawHeader}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import im.michalski.golgifbot.models.RawMatchThreadData
import io.circe.ACursor

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

case class RedditApiClientConfig(
                                  username: String,
                                  password: String,
                                  clientId: String,
                                  clientSecret: String,
                                  userAgent:String
                                )

class RedditApiClient(val config: RedditApiClientConfig)(implicit ec: ExecutionContext) extends CirceSupport {
  import JsonOps._

  private val apiKeyHeaderName = "Authorize"

  private val acceptHeaderName = "Accept"
  private val acceptHeaderValue = "application/json"

  private val AuthHost = "www.reddit.com"
  private val AuthUri = "/api/v1/access_token"
  private val AuthQueryParams = s"?grant_type=password&username=${config.username}&password=${config.password}"

  private val RequestHost = "oauth.reddit.com"

  private implicit val system = ActorSystem()
  private implicit val executor = system.dispatcher
  private implicit val materializer = ActorMaterializer()

  private lazy val authConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnectionHttps(AuthHost)
  private lazy val requestConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnectionHttps(RequestHost)

  val token = authorize(config.clientId, config.clientSecret)

  def shutdown() = {
    Http().shutdownAllConnectionPools().onComplete{ _ =>
      system.terminate()
    }
  }

  private def blocking[T](f: Future[T], patience: FiniteDuration = 5 seconds) = {
    Await.result(f, patience)
  }

  private def request(request: HttpRequest, connectionFlow: Flow[HttpRequest, HttpResponse, Any]): Future[HttpResponse] = {
    Source.single(request).via(connectionFlow).runWith(Sink.head)
  }

  private def failure(err: Error) = Future.successful(Left(err))

  private def unmarshall[T](entity: ResponseEntity)
                           (implicit um: Unmarshaller[ResponseEntity, T]): Future[Either[Error, T]] = {
    Try(Unmarshal(entity).to[T]) match {
      case Success(e) => e.map(Right(_))
      case Failure(exception) => failure(Error(exception.toString))
    }
  }

  private def parseSimple(entity: ResponseEntity): Future[Either[Error, io.circe.Json]] = {
    import io.circe.parser._

    val body = blocking(entity.toStrict(3 seconds).map(_.data.decodeString("UTF-8")))

    parse(body).fold(
      error => failure(Error(error.message)),
      success => Future.successful(Right(success))
    )
  }

  private def process[T](response: HttpResponse, decode: ResponseEntity => Future[Either[Error, T]])
                        (implicit um: Unmarshaller[ResponseEntity, T]): Future[Either[Error, T]] = {
    response.status match {
      case StatusCodes.OK => decode(response.entity)
      case StatusCodes.BadRequest => failure(Error("Bad Request"))
      case StatusCodes.Unauthorized => failure(Error("Unauthorized"))
      case StatusCodes.Forbidden => failure(Error("Forbidden"))
      case _ => failure(Error(s"Other problem: ${response.status}"))
    }
  }

  private def authorize(username: String, password: String) = {
    val authorizationHeader = headers.Authorization(BasicHttpCredentials(username, password))

    val tokenRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(AuthUri + AuthQueryParams),
      headers = List(authorizationHeader)
    )

    println(s"Token Request: $tokenRequest")

    val response = request(tokenRequest, authConnectionFlow)
      .flatMap(response => process[AccessToken](response, unmarshall[AccessToken]))
      .map(_.map(_.access_token))

    blocking(response).fold(
      err     => throw new RuntimeException(err.message),
      success => success
    )
  }

  def getMatchThreadData: Future[List[RawMatchThreadData]] = {

    def getJson: Future[io.circe.Json] = {
      val RequestUri = "/r/soccer/search"
      val QueryString = "?q=flair%3Apostmatch&sort=new&restrict_sr=on&t=week"
      val authHeader = RawHeader("Authorization", s"bearer $token")
      val userAgentHeader = RawHeader("User-Agent", "GolGifBot/0.1 by RegisPL")

      val dataRequest = HttpRequest(
        method = HttpMethods.GET,
        uri = Uri(RequestUri + QueryString),
        headers = List(authHeader)
      )

      println(s"Data Request: $dataRequest")

      request(dataRequest, requestConnectionFlow)
        .flatMap(response => process[io.circe.Json](response, parseSimple))
        .map(_.fold(
          error => throw new RuntimeException(error.message),
          success => success
        ))
    }

    def extractMatchInfo(json: io.circe.Json): List[RawMatchThreadData] = {
      // FIXME: refactor, this is ugly as fuck and the only difference is the last field I pick...
      val hcursor = json.hcursor
      val data = hcursor.downField("data").downField("children").downArray

      val head = data.downField("data")
      val tail = data.rights

      val titleHead = head.downField("title").as[String]
      val titlesTail = tail.map(_.map(_.hcursor.downField("data").downField("title").as[String]))

      val selftextHead = head.downField("selftext").as[String]
      val selftextsTail = tail.map(_.map(_.hcursor.downField("data").downField("selftext").as[String]))

      val mergedTitles = titleHead +: titlesTail.getOrElse(Nil)
      val mergedSelftexts = selftextHead +: selftextsTail.getOrElse(Nil)

      val titles = mergedTitles.flatMap(_.toSeq)
      val selftexts = mergedSelftexts.flatMap(_.toSeq)

      titles.zip(selftexts).map(pair => RawMatchThreadData(pair._1, pair._2))
    }

    getJson.map(extractMatchInfo)
  }
}
