package im.michalski.golgifbot.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity, StatusCodes}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import im.michalski.golgifbot.models.Problem

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}


trait ApiClient extends LazyLogging {
  implicit val as: ActorSystem
  implicit val ece: ExecutionContextExecutor
  implicit val am: ActorMaterializer

  def shutdown(): Future[Unit] = {
    Http().shutdownAllConnectionPools()
  }

  private[clients] def blocking[T](f: IO[T], patience: FiniteDuration = 5 seconds): T = {
    f.unsafeRunTimed(patience).getOrElse(sys.error("Timed out when running IO!"))
  }

  private[clients] def request(request: HttpRequest, connectionFlow: Flow[HttpRequest, HttpResponse, Any]): IO[HttpResponse] = {
    val entityShort = s"'${request.entity.toString.substring(0, request.entity.toString.length.min(150))}...'"
    logger.debug(s"Calling ${request.method} ${request.uri} with entity $entityShort and headers ${request.headers}")
    logger.trace(request.entity.toString)
    IO.fromFuture(IO {
      Source.single(request).via(connectionFlow).runWith(Sink.head)
    })
  }

  private[clients] def failure(err: Problem): IO[Left[Problem, Nothing]] = IO.pure(Left(err))

  private [clients] def collectResponseEntity(entity: ResponseEntity, timeout: FiniteDuration = 5 seconds): IO[String] = {
    IO.fromFuture(IO {
      entity.toStrict(timeout).map(_.data.decodeString("UTF-8"))
    })
  }

  private[clients] def unmarshall[T](entity: ResponseEntity)
                                    (implicit um: Unmarshaller[ResponseEntity, T]): IO[Either[Problem, T]] = {
    Try(Unmarshal(entity).to[T]).map(t => IO.fromFuture(IO(t))) match {
      case Success(e)         => e.map(Right(_))
      case Failure(exception) => collectResponseEntity(entity).map { data =>
        Left(Problem(s"Failed to unmarshal entity $data with error: ${exception.toString}"))
      }
    }
  }

  private[clients] def parseSimple(entity: ResponseEntity): IO[Either[Problem, io.circe.Json]] = {
    import io.circe.parser._

    collectResponseEntity(entity).map(parse)
      .map(_.fold(
        error => Left(Problem(s"Failed to parse: ${error.message}")),
        success => Right(success)
    ))
  }

  private[clients] def process[T](response: HttpResponse, decode: ResponseEntity => IO[Either[Problem, T]]): IO[Either[Problem, T]] = {
    response.status match {
      case StatusCodes.OK   => decode(response.entity)
      case _                => collectResponseEntity(response.entity).map { data =>
        Left(Problem(s"Request failed with HTTP status code ${response.status.toString}: $data"))
      }
    }
  }
}
