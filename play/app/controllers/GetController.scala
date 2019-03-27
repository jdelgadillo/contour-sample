package controllers

import javax.inject.Inject
import play.api.http.ContentTypes
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

class GetController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  private def echoRequest(request: Request[AnyContent]): String = {
    val buffer = new StringBuilder()
    buffer.append("<tr><th>Header Key</th><th>Header Value</th></tr>")
    request.headers.toSimpleMap.foreach( entry =>
        buffer.append(s"   <tr><td>${entry._1}</td><td>${entry._2}</td></tr>\n")
    )
    buffer.toString()
  }

  def index = Action{ request =>
    Ok(
      s"""
        |<html>
        | <head><title>Hello World!</title></head>
        | <body>
        | <h1>Request:</h1>
        | <table border="1">
        | ${echoRequest(request)}
        | </table>
        | </body>
        |</html>
      """.stripMargin).as(ContentTypes.HTML)
  }

}