package mochido.server

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}

trait Service {

  implicit def system: ActorSystem

  implicit def materializer: ActorMaterializer

  def eventManager: ActorRef



  val routes: Route = path("hello") {
    get {
      complete("Hello World")
    }
  } ~ path("greeter") {
    val newSource = Source.actorRef(50, OverflowStrategy.dropNew)
      .mapMaterializedValue(ref => eventManager.tell(EventManager.NewSubscriber(ref), ActorRef.noSender))

    val newFlow = Flow.fromSinkAndSource(Sink.ignore, newSource)
    handleWebSocketMessages(newFlow)
  }

}

object Server extends App with Service {

  override implicit val system = ActorSystem()
  override implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  override val eventManager = system.actorOf(EventManager.props)
  val bambooChecker = system.actorOf(BambooChecker.props(eventManager))

  Http().bindAndHandle(routes, "localhost", 8080)
}
