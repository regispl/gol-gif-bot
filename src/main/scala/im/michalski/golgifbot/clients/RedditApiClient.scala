package im.michalski.golgifbot.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, RawHeader}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import cats.data.EitherT
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import im.michalski.golgifbot.config.Config
import im.michalski.golgifbot.models.{AccessToken, Problem, RawMatchThreadData}
import io.circe.{ACursor, Json}

import scala.concurrent.ExecutionContextExecutor


class RedditApiClient(val config: RedditApiClientConfig)
                     (implicit val as: ActorSystem, val ece: ExecutionContextExecutor, val am: ActorMaterializer)
  extends ApiClient with FailFastCirceSupport with LazyLogging {

  import im.michalski.golgifbot.models.JsonOps._

  private lazy val authConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnectionHttps("www.reddit.com")
  private lazy val requestConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnectionHttps("oauth.reddit.com")

  lazy val authToken: EitherT[IO, Problem, String] = authorize

  private def authorize = EitherT[IO, Problem, String] {
    val authUri = "/api/v1/access_token"
    val authQueryParams = s"?grant_type=password&username=${config.username}&password=${config.password}"

    val authorizationHeader = headers.Authorization(BasicHttpCredentials(config.clientId, config.clientSecret))

    val tokenRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(authUri + authQueryParams),
      headers = List(authorizationHeader)
    )

    request(tokenRequest, authConnectionFlow)
      .flatMap(response => process[AccessToken](response, unmarshall[AccessToken]))
      .map(_.map(_.access_token))
  }

  def getMatchThreadData: EitherT[IO, Problem, List[RawMatchThreadData]] = {
    def getJson(token: String) = EitherT[IO, Problem, Json] {
      val requestUri = "/r/soccer/search"
      val queryString = "?q=flair_css_class%3Apostmatch&sort=new&restrict_sr=on&t=day"

      val authHeader = RawHeader("Authorization", s"bearer $token")

      val dataRequest = HttpRequest(
        method = HttpMethods.GET,
        uri = Uri(requestUri + queryString),
        headers = List(authHeader)
      )

      request(dataRequest, requestConnectionFlow)
        .flatMap(response => process[Json](response, parseSimple))
    }

    def extractMatchInfo(json: Json) = {
      def getFieldValues(cursors: Seq[ACursor], field: String): Seq[String] = {
        cursors.map(_.downField(field).as[String]).flatMap(_.toSeq)
      }

      val data = json.hcursor.downField("data").downField("children").downArray

      val head = data.downField("data")
      val maybeTail = data.rights.map(_.map(_.hcursor.downField("data")))

      val threadData = maybeTail.map(head +: _).toSeq.flatten

      val ids = getFieldValues(threadData, "id")
      val titles = getFieldValues(threadData, "title")
      val selftexts = getFieldValues(threadData, "selftext")

      (ids, titles, selftexts).zipped.toSeq
        .map(row => RawMatchThreadData(row._1, row._2, row._3))
        .toList
    }

    val response = for {
      token <- authToken
      json  <- getJson(token)
      data   = extractMatchInfo(json)
    } yield data

    response
  }
}


case class RedditApiClientConfig(username: String,
                                 password: String,
                                 clientId: String,
                                 clientSecret: String,
                                 userAgent:String)


object RedditApiClientConfig {
  def apply(config: Config, userAgent: String): RedditApiClientConfig = {
    new RedditApiClientConfig(
      username = config.redditUsername,
      password = config.redditPassword,
      clientId = config.redditClientId,
      clientSecret = config.redditClientSecret,
      userAgent = userAgent
    )
  }
}
