package mochido.server

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.http.scaladsl.model.ws.TextMessage
import mochido.server.EventManager.{Event, NewSubscriber}

object EventManager {
  def props = Props(classOf[EventManager])

  case class NewSubscriber(ref: ActorRef)

  case class Event(json: String)

}

class EventManager extends Actor {

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
}
