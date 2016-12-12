package im.michalski.golgifbot.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, RawHeader}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import de.heikoseeberger.akkahttpcirce.CirceSupport
import im.michalski.golgifbot.models.{AccessToken, RawMatchThreadData}
import io.circe.ACursor

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps


case class RedditApiClientConfig(username: String, password: String, clientId: String, clientSecret: String, userAgent:String)

class RedditApiClient(val config: RedditApiClientConfig)
                     (implicit val as: ActorSystem, val ecc: ExecutionContextExecutor, val am: ActorMaterializer)
  extends ApiClient with CirceSupport {

  import im.michalski.golgifbot.models.JsonOps._

  private lazy val authConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnectionHttps("www.reddit.com")
  private lazy val requestConnectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnectionHttps("oauth.reddit.com")

  val token: String = authorize(config.clientId, config.clientSecret)

  private def authorize(username: String, password: String) = {
    val authUri = "/api/v1/access_token"
    val authQueryParams = s"?grant_type=password&username=${config.username}&password=${config.password}"

    val authorizationHeader = headers.Authorization(BasicHttpCredentials(username, password))

    val tokenRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(authUri + authQueryParams),
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

  def getMatchThreadData: Future[Seq[RawMatchThreadData]] = {
    def getJson: Future[io.circe.Json] = {
      val requestUri = "/r/soccer/search"
      val queryString = "?q=flair%3Apostmatch&sort=new&restrict_sr=on&t=day"

      val authHeader = RawHeader("Authorization", s"bearer $token")
      val userAgentHeader = RawHeader("User-Agent", config.userAgent)

      val dataRequest = HttpRequest(
        method = HttpMethods.GET,
        uri = Uri(requestUri + queryString),
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

    def extractMatchInfo(json: io.circe.Json): Seq[RawMatchThreadData] = {
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
    }

    getJson.map(extractMatchInfo)
  }
}
