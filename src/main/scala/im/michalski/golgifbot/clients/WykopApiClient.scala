package im.michalski.golgifbot.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import cats.data.EitherT
import cats.effect.IO
import cats.instances.all._
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import im.michalski.golgifbot.config.Config
import im.michalski.golgifbot.models.Problem
import im.michalski.golgifbot.utils.MD5
import io.circe.Json

import scala.concurrent.ExecutionContextExecutor


class WykopApiClient(val config: WykopApiClientConfig)
                    (implicit val as: ActorSystem, val ece: ExecutionContextExecutor, val am: ActorMaterializer)
  extends ApiClient with FailFastCirceSupport with LazyLogging {

  private val host: String = "a.wykop.pl"

  private lazy val connectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnection(host)

  lazy val authToken: EitherT[IO, Problem, String] = authorize

  def apiSignHeader(uri: String, postParams: Map[String, String]) = {
    def postParamsToChecksum(params: Map[String, String]) = {
      params.toSeq.sortBy(_._1).map(_._2).mkString(",")
    }

    val checksum = config.secret + s"http://$host$uri" + postParamsToChecksum(postParams)
    RawHeader("apisign", MD5.hash(checksum))
  }

  private def authorize: EitherT[IO, Problem, String] = {
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

    val responseJson = EitherT[IO, Problem, Json] {
      request(microblogAdd, connectionFlow)
        .flatMap(response => process[Json](response, parseSimple))
    }

    def parseJson(json: Json) = EitherT[IO, Problem, String] {
      IO.pure {
        json.hcursor.downField("userkey").as[String]
          .fold(
            error => Left(Problem(s"Failed to parse 'userkey': $error")),
            success => Right(success)
          )
      }
    }

    for {
      json      <- responseJson
      userkey   <- parseJson(json)
    } yield userkey
  }

  def publish(body: String) = {
    def call(token: String) = EitherT[IO, Problem, Json] {
      val microblogAddUri = s"/entries/add/appkey,${config.applicationKey},userkey,$token/"

      val postParams = Map[String, String]("body" -> body)

      val tokenRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = Uri(microblogAddUri),
        headers = List(apiSignHeader(microblogAddUri, postParams)),
        entity = FormData(postParams).toEntity
      )

      request(tokenRequest, connectionFlow)
        .flatMap(response => process[Json](response, parseSimple))
    }

    def parse(json: Json) = EitherT[IO, Problem, Int] {
      IO.pure {
        json.hcursor.downField("id").as[String].fold(
          failure => Left(Problem(s"Failed to parse id: ${failure.message} when parsing JSON response: ${json.toString}; Body provided: ${body}")),
          success => Right(success.toInt)
        )
      }
    }

    for {
      token  <- authToken
      json   <- call(token)
      result <- parse(json)
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
