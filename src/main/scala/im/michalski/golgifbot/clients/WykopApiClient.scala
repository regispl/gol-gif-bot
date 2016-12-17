package im.michalski.golgifbot.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{RawHeader, `Content-Type`}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import cats.data.EitherT
import cats.instances.all._
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.CirceSupport
import im.michalski.golgifbot.config.Config
import im.michalski.golgifbot.models.{FormattedMatchData, Problem}
import im.michalski.golgifbot.utils.MD5
import io.circe.Json

import scala.concurrent.{ExecutionContextExecutor, Future}


class WykopApiClient(val config: WykopApiClientConfig)
                    (implicit val as: ActorSystem, val ecc: ExecutionContextExecutor, val am: ActorMaterializer)
  extends ApiClient with CirceSupport with LazyLogging {

  private val host: String = "a.wykop.pl"

  private lazy val connectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnection(host)

  lazy val authToken: EitherT[Future, Problem, String] = authorize

  def apiSignHeader(uri: String, postParams: Map[String, String]) = {
    def postParamsToChecksum(params: Map[String, String]) = {
      params.toSeq.sortBy(_._1).map(_._2).mkString(",")
    }

    val checksum = config.secret + s"http://$host$uri" + postParamsToChecksum(postParams)
    RawHeader("apisign", MD5.hash(checksum))
  }

  private def authorize: EitherT[Future, Problem, String] = {
    val authUri = s"/user/login/appkey/${config.applicationKey}/"

    val postParams = Map[String, String](
      "login"       -> config.login,
      "accountkey"  -> config.accountKey
    )

    val microblogAdd = HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(authUri),
      headers = List(apiSignHeader(authUri, postParams)),
      entity = FormData(postParams).toEntity
    )

    //logger.debug(s"Microblog Add Request: $microblogAdd")

    val responseJson = EitherT[Future, Problem, Json] {
      request(microblogAdd, connectionFlow)
        .flatMap(response => process[Json](response, parseSimple))
    }

    def parseJson(json: Json) = EitherT[Future, Problem, String] {
      Future.successful {
        json.hcursor.downField("userkey").as[String]
          .fold(
            error => Left(Problem(s"Failed to parse 'userkey': $error")),
            success => Right(success)
          )
      }
    }

    val response = for {
      json      <- responseJson
      userkey   <- parseJson(json)
    } yield userkey

    response
  }

  def publish(matchData: FormattedMatchData) = {
    logger.info(s"Trying to add entry for: ${matchData.headline}")

    def call(token: String) = EitherT[Future, Problem, Json] {
      val microblogAddUri = s"/entries/add/appkey,${config.applicationKey},userkey,$token/"

      val postParams = Map[String, String]("body" -> matchData.text)

      val tokenRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = Uri(microblogAddUri),
        headers = List(apiSignHeader(microblogAddUri, postParams)),
        entity = FormData(postParams).toEntity
      )

      logger.debug(s"Token Request: ${tokenRequest.copy(entity = "<REDACTED>")}")

      request(tokenRequest, connectionFlow)
        .flatMap(response => process[Json](response, parseSimple))
    }

    def parse(json: Json) = EitherT[Future, Problem, Int] {
      Future.successful {
        json.hcursor.downField("id").as[String].fold(
          failure => Left(Problem(s"Failed to parse id: $failure")),
          success => Right(success.toInt)
        )
      }
    }

    for {
      token   <- authToken
      json    <- call(token)
      result  <- parse(json)
    } yield result
  }
}

case class WykopApiClientConfig(login: String,
                                applicationKey: String,
                                secret: String,
                                accountKey: String,
                                userAgent: String)


object WykopApiClientConfig {
  def apply(config: Config, userAgent: String): WykopApiClientConfig = {
    new WykopApiClientConfig(
      login = config.wykopLogin,
      applicationKey = config.wykopApplicationKey,
      secret = config.wykopSecret,
      accountKey = config.wykopAccountKey,
      userAgent = userAgent
    )
  }
}
