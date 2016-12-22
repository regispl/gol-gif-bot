package im.michalski.golgifbot.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity, StatusCodes}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import im.michalski.golgifbot.models.Problem

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}


trait ApiClient extends LazyLogging {
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
    val entityShort = s"'${request.entity.toString.substring(0, request.entity.toString.length.min(150))}...'"
    logger.debug(s"Calling ${request.method} ${request.uri} with entity $entityShort and headers ${request.headers}")
    logger.trace(request.entity.toString)
    Source.single(request).via(connectionFlow).runWith(Sink.head)
  }

  private[clients] def failure(err: Problem) = Future.successful(Left(err))

  private [clients] def collectResponseEntity(entity: ResponseEntity, timeout: FiniteDuration = 5 seconds): Future[String] = {
    entity.toStrict(timeout).map(_.data.decodeString("UTF-8"))
  }

  private[clients] def unmarshal[T](entity: ResponseEntity)
                                   (implicit um: Unmarshaller[ResponseEntity, T]): Future[Either[Problem, T]] = {
    Try(Unmarshal(entity).to[T]) match {
      case Success(e) => e.map(Right(_))
      case Failure(exception) => collectResponseEntity(entity).map { data =>
        Left(Problem(s"Failed to unmarshal entity $data with error: ${exception.toString}"))
      }
    }
  }

  private[clients] def parseSimple(entity: ResponseEntity): Future[Either[Problem, io.circe.Json]] = {
    import io.circe.parser._

    collectResponseEntity(entity).map(parse)
      .map(_.fold(
        error => Left(Problem(s"Failed to parse: ${error.message}")),
        success => Right(success)
    ))
  }

  private[clients] def process[T](response: HttpResponse, decode: ResponseEntity => Future[Either[Problem, T]])
                                 (implicit um: Unmarshaller[ResponseEntity, T]): Future[Either[Problem, T]] = {
    response.status match {
      case StatusCodes.OK   => decode(response.entity)
      case _                => collectResponseEntity(response.entity).map { data =>
        Left(Problem(s"Request failed with HTTP status code ${response.status.toString}: $data"))
      }
    }
  }
}
