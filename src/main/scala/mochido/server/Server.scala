package mochido.server

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.typesafe.config.ConfigFactory
import mochido.server.bamboo.BambooModule

object Server extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  implicit val config = ConfigFactory.load()

  val eventManager = system.actorOf(EventManager.props)

  BambooModule.start(eventManager)

  val routes: Route = {
    path("greeter") {
      val newSource = Source.actorRef(50, OverflowStrategy.dropBuffer)
        .mapMaterializedValue(ref => eventManager.tell(EventManager.NewSubscriber(ref), ActorRef.noSender))

      handleWebSocketMessages(Flow.fromSinkAndSource(Sink.ignore, newSource))
    } ~ path(PathEnd) {
      get {
        complete("hello")
      }
    }
  }

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
