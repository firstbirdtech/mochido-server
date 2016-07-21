package mochido.server

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.http.scaladsl.model.ws.TextMessage
import mochido.server.EventManager.{Event, Heartbeat, NewSubscriber}
import spray.json.{DefaultJsonProtocol, _}

import scala.concurrent.duration._

object EventManager {
  def props = Props(classOf[EventManager])

  case class NewSubscriber(ref: ActorRef)

  case class Event(json: String)
  case class Heartbeat(messageType: String = "heartbeat")

  object JsonProtocol extends DefaultJsonProtocol {
    implicit val buildBrokeFormat = jsonFormat1(Heartbeat)
  }
}

class EventManager extends Actor {

  import mochido.server.EventManager.JsonProtocol._

  private var subscribers = Set[ActorRef]()
  implicit val ctx = context.dispatcher

  override def receive: Receive = {
    case NewSubscriber(ref) =>
      context.watch(ref)
      subscribers = subscribers + ref
    case Terminated(ref) =>
      subscribers = subscribers - ref
    case Event(json) =>
      subscribers.foreach(_ ! TextMessage(json))
    case _ =>
  }

  override def preStart(): Unit = {
    context.system.scheduler.schedule(15.seconds, 15.seconds, self, Event(Heartbeat().toJson.prettyPrint))
  }
}
