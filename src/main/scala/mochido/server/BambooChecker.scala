package mochido.server

import akka.actor.{Actor, ActorRef, Props}
import mochido.server.Bamboo.{BambooResult, BambooResults, BambooResultsPage}
import mochido.server.BambooChecker.{BuildBroke, BuildFixed, CheckBamboo}
import spray.json._

import scala.concurrent.duration._
import scalaj.http.Http

object BambooChecker {
  def props(eventManager: ActorRef): Props = Props(classOf[BambooChecker], eventManager)

  case object CheckBamboo

  case class BuildBroke(buildName: String, user: String = "Daniel Winter", messageType: String = "BuildBroke")

  case class BuildFixed(buildName: String, user: String = "Chuck Norris", messageType: String = "BuildFixed")

  object JsonProtocol extends DefaultJsonProtocol {
    implicit val buildBrokeFormat = jsonFormat3(BuildBroke)
    implicit val buildFixedFormat = jsonFormat3(BuildFixed)
  }

}

class BambooChecker(eventManager: ActorRef) extends Actor {
  implicit val ctx = context.dispatcher
  var oldResult: Option[BambooResults] = None

  def callBamboo() = {
    import Bamboo.JsonProtocol._

    val result: String = Http("http://bamboo.firstbird.eu:8085/rest/api/latest/result.json")
      .param("os_authType", "basic")
      .param("max-result", "100")
      .header("Authorization", "Basic ZGFuaWVsLnBmZWlmZmVyQGZpcnN0YmlyZC5ldTp5MGcxYjQzcg==")
      .timeout(5000, 5000)
      .asString
      .body

    result.parseJson.convertTo[BambooResults]
  }

  override def receive: Receive = {
    case CheckBamboo =>
      import BambooChecker.JsonProtocol._

      var result = callBamboo()

      val oldAndNew: Seq[(BambooResult, BambooResult)] = oldResult match {
        case Some(BambooResults(BambooResultsPage(oldResults: List[BambooResult]))) =>
          oldResults.flatMap { o =>
            val newResults = result.results.result
            newResults.find(_.plan.name == o.plan.name).map(n => (o, n))
          }
        case None => Seq()
      }

      oldAndNew
        .filter { case (o, n) => o.state == "Successful" & n.state == "Failed" }
        .map { case (o, n) => BuildBroke(n.plan.name) }
        .map(event => event.toJson.compactPrint)
        .foreach(eventManager ! EventManager.Event(_))

      oldAndNew
        .filter { case (o, n) => o.state == "Failed" & n.state == "Successful" }
        .map { case (o, n) => BuildFixed(n.plan.name) }
        .map(event => event.toJson.compactPrint)
        .foreach(eventManager ! EventManager.Event(_))

      oldResult = Some(result)
    case _ =>
  }

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = context.system.scheduler.schedule(5.seconds, 5.seconds, self, CheckBamboo)
}
