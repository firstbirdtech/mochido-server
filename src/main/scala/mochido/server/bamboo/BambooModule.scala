package mochido.server.bamboo

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.Config
import mochido.server.Module
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

object BambooModule extends Module {
  override def start(receiver: ActorRef)(implicit system: ActorSystem, config: Config): Unit = {
    val bambooSettings = config.as[BambooSettings]("bamboo")
    val bambooChecker = system.actorOf(BambooChecker.props(bambooSettings, receiver))
  }
}
