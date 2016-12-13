package im.michalski.golgifbot.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{RawHeader, `Content-Type`}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import de.heikoseeberger.akkahttpcirce.CirceSupport
import im.michalski.golgifbot.utils.MD5

import scala.concurrent.{ExecutionContextExecutor, Future}


case class WykopApiClientConfig(login: String, applicationKey: String, secret: String, accountKey: String)

class WykopApiClient(val config: WykopApiClientConfig)
                    (implicit val as: ActorSystem, val ecc: ExecutionContextExecutor, val am: ActorMaterializer)
  extends ApiClient with CirceSupport {

  private val host: String = "a.wykop.pl"

  private lazy val connectionFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnection(host)

  val token: String = authorize()

  def apiSignHeader(uri: String, postParams: Map[String, String]) = {
    def postParamsToChecksum(params: Map[String, String]) = {
      params.toSeq.sortBy(_._1).map(_._2).mkString(",")
    }

    val checksum = config.secret + s"http://$host$uri" + postParamsToChecksum(postParams)
    RawHeader("apisign", MD5.hash(checksum))
  }

  private def authorize() = {
    val authUri = s"/user/login/appkey/${config.applicationKey}/"

    val postParams = Map[String, String](
      "login" -> config.login,
      "accountkey" -> config.accountKey
    )

    val microblogAdd = HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(authUri),
      headers = List(apiSignHeader(authUri, postParams)),
      entity = FormData(postParams).toEntity
    )

    println(s"Microblog Add Request: $microblogAdd")

    val response = request(microblogAdd, connectionFlow)
      .flatMap(response => process[io.circe.Json](response, parseSimple))
      .map(_.fold(
        error => throw new RuntimeException(error.message),
        success => success
      )).map(_.hcursor.downField("userkey").as[String])

    blocking(response).fold(
      err     => throw new RuntimeException(err.message),
      success => success
    )
  }

  def publish(text: String): Future[Int] = {
    val microblogAddUri = s"/entries/add/appkey,${config.applicationKey},userkey,$token/"

    val postParams = Map[String, String]("body" -> text)

    val tokenRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(microblogAddUri),
      headers = List(apiSignHeader(microblogAddUri, postParams)),
      entity = FormData(postParams).toEntity
    )

    println(s"Token Request: ${tokenRequest.copy(entity = "<REDACTED>")}")

//    val response = request(tokenRequest, connectionFlow)
//      .flatMap(response => process[io.circe.Json](response, parseSimple))
//      .map(_.fold(
//        error => throw new RuntimeException(error.message),
//        success => success
//      )).map(_.hcursor.downField("id").as[String])
//
//    blocking(response).fold(
//      err     => throw new RuntimeException(err.message),
//      success => {
//        println(success)
//        success
//      }
//    )

    Future.successful(1)
  }
}
