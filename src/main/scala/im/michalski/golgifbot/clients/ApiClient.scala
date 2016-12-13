package im.michalski.golgifbot.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity, StatusCodes}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import im.michalski.golgifbot.models.Error

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}


trait ApiClient {
  implicit val as: ActorSystem
  implicit val ecc: ExecutionContextExecutor
  implicit val am: ActorMaterializer

  def shutdown() = {
    Http().shutdownAllConnectionPools()
  }

  private[clients] def blocking[T](f: Future[T], patience: FiniteDuration = 5 seconds) = {
    Await.result(f, patience)
  }

  private[clients] def request(request: HttpRequest, connectionFlow: Flow[HttpRequest, HttpResponse, Any]): Future[HttpResponse] = {
    Source.single(request).via(connectionFlow).runWith(Sink.head)
  }

  private[clients] def failure(err: Error) = Future.successful(Left(err))

  private[clients] def unmarshall[T](entity: ResponseEntity)
                                    (implicit um: Unmarshaller[ResponseEntity, T]): Future[Either[Error, T]] = {
    Try(Unmarshal(entity).to[T]) match {
      case Success(e) => e.map(Right(_))
      case Failure(exception) => failure(Error(exception.toString))
    }
  }

  private[clients] def parseSimple(entity: ResponseEntity): Future[Either[Error, io.circe.Json]] = {
    import io.circe.parser._

    val body = entity.toStrict(3 seconds).map(_.data.decodeString("UTF-8"))

    body.map(parse).map(_.fold(
      error => Left(Error(error.message)),
      success => Right(success)
    ))
  }

  private[clients] def process[T](response: HttpResponse, decode: ResponseEntity => Future[Either[Error, T]])
                                 (implicit um: Unmarshaller[ResponseEntity, T]): Future[Either[Error, T]] = {
    response.status match {
      case StatusCodes.OK           => decode(response.entity)
      case StatusCodes.BadRequest   => failure(Error("Bad Request"))
      case StatusCodes.Unauthorized => failure(Error("Unauthorized"))
      case StatusCodes.Forbidden    => failure(Error("Forbidden"))
      case _                        => failure(Error(s"Other problem: ${response.status}"))
    }
  }
}
