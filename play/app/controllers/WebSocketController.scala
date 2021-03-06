/*
 * Based on example at https://github.com/playframework/play-scala-websocket-example/blob/2.6.x/app/controllers/HomeController.scala
 */
package controllers

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import filters.LoggingFilter
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, ControllerComponents, RequestHeader, WebSocket}

import scala.concurrent.{ExecutionContext, Future}

class WebSocketController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) with SameOriginCheck {
  val logger = play.api.Logger(getClass)

  def ws: WebSocket = {
    WebSocket.acceptOrResult[JsValue, JsValue] {
      case rh if sameOriginCheck(rh) =>
        wsFutureFlow(rh).map { flow =>
          logger.info(s"Accepting WebSocket request for ${rh.path}")
          LoggingFilter.logRequestHeader(None, rh)
          Right(flow)
        }.recover {
          case e: Exception =>
            logger.error("Cannot create websocket", e)
            val jsError = Json.obj("error" -> "Cannot create websocket")
            val result = InternalServerError(jsError)
            Left(result)
        }

      case rejected =>
        logger.error(s"Request ${rejected} failed same origin check")
        Future.successful {
          Left(Forbidden("forbidden"))
        }
    }
  }

  private def wsFutureFlow(header: RequestHeader): Future[Flow[JsValue, JsValue, NotUsed]] = {
    Future(Flow.fromSinkAndSource(Sink.ignore, Source.single[JsValue](Json.obj("success" -> "Successfully connected to websocket"))))
  }

}

trait SameOriginCheck {

  def logger: Logger

  /**
    * Checks that the WebSocket comes from the same origin.  This is necessary to protect
    * against Cross-Site WebSocket Hijacking as WebSocket does not implement Same Origin Policy.
    *
    * See https://tools.ietf.org/html/rfc6455#section-1.3 and
    * http://blog.dewhurstsecurity.com/2013/08/30/security-testing-html5-websockets.html
    */
  def sameOriginCheck(rh: RequestHeader): Boolean = {
    rh.headers.get("Origin") match {
      case Some(originValue) if originMatches(originValue) =>
        logger.debug(s"originCheck: originValue = $originValue")
        true

      case Some(badOrigin) =>
        logger.error(s"originCheck: rejecting request because Origin header value ${badOrigin} is not in the same origin")
        false

      case None =>
        logger.error("originCheck: rejecting request because no Origin header found")
        false
    }
  }

  /**
    * Returns true if the value of the Origin header contains an acceptable value.
    *
    * This is probably better done through configuration same as the allowedhosts filter.
    */
  def originMatches(origin: String): Boolean = {
    //origin.contains("localhost:9000") || origin.contains("localhost:19001")
    true
  }
}
