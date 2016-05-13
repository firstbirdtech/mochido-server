package mochido.server.bamboo

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import mochido.server.EventManager
import mochido.server.bamboo.BambooChecker.{BuildBroke, BuildFixed, CheckBamboo}
import mochido.server.bamboo.BambooJsonProtocol._
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration._

object BambooChecker {
  def props(bambooSettings: BambooSettings, eventManager: ActorRef): Props =
    Props(classOf[BambooChecker], bambooSettings, eventManager)

  case object CheckBamboo

  sealed trait BambooEvent
  case class BuildBroke(buildName: String, user: String = "Daniel Winter", messageType: String = "BuildBroke") extends BambooEvent
  case class BuildFixed(buildName: String, user: String = "Chuck Norris", messageType: String = "BuildFixed") extends BambooEvent

  object JsonProtocol extends DefaultJsonProtocol {
    implicit val buildBrokeFormat = jsonFormat3(BuildBroke)
    implicit val buildFixedFormat = jsonFormat3(BuildFixed)
  }

}

class BambooChecker(settings: BambooSettings, eventManager: ActorRef) extends Actor {

  import BambooChecker.JsonProtocol._

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  var lastBambooBuildsResult: Option[BambooResults] = None

  def requestPlanResults() = {


    val http = Http(context.system)

    val request = HttpRequest(
      uri = Uri(settings.url).withQuery(Query("os_authType" -> "basic", "max-result" -> "100")),
      headers = List(Authorization(BasicHttpCredentials(settings.username, settings.password)))
    )

    val result = for {
      res <- http.singleRequest(request)
      body <- Unmarshal(res.entity).to[String]
    } yield body.parseJson.convertTo[BambooResults]

    Await.result(result, 5.seconds)

    //    result.parseJson.convertTo[BambooResults]
  }

  override def receive: Receive = {
    case CheckBamboo =>

      val currentBambooResult = requestPlanResults()

      lastBambooBuildsResult = lastBambooBuildsResult match {
        case Some(BambooResults(BambooResultsPage(oldResults: List[BambooResult]))) =>
          val newResults = currentBambooResult.results.result

          val plansWithOldAndNewState = for {
            or <- oldResults
            nr <- newResults if nr.plan.name == or.plan.name
          } yield {
            (or.plan.name, or.state, nr.state)
          }

          plansWithOldAndNewState.flatMap {
            case (name, os, ns) if os == "Successful" & ns == "Failed" => Some(BuildBroke(name).toJson)
            case (name, os, ns) if os == "Failed" & ns == "Successful" => Some(BuildFixed(name).toJson)
            case _ => None
          }.map { json =>
            json.prettyPrint
          }.foreach { jsonString =>
            eventManager ! EventManager.Event(jsonString)
          }

          Some(currentBambooResult)

        case None =>
          Some(currentBambooResult)
      }

    case _ =>
  }

  override def preStart(): Unit = {
    context.system.scheduler.schedule(5.seconds, 5.seconds, self, CheckBamboo)
  }
}
