package mochido.server

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.typesafe.config.ConfigFactory
import mochido.server.bamboo.{BambooChecker, BambooSettings}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

object Server extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val config = ConfigFactory.load()
  val eventManager = system.actorOf(EventManager.props)

  val bambooSettings = config.as[BambooSettings]("bamboo")
  val bambooChecker = system.actorOf(BambooChecker.props(bambooSettings, eventManager))


  val routes: Route = {
    path("greeter") {
      val newSource = Source.actorRef(50, OverflowStrategy.dropBuffer)
        .mapMaterializedValue(ref => eventManager.tell(EventManager.NewSubscriber(ref), ActorRef.noSender))

      handleWebSocketMessages(Flow.fromSinkAndSource(Sink.ignore, newSource))
    }
  }

  Http().bindAndHandle(routes, "localhost", 8080)
}
