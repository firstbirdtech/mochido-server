package mochido.server

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.Config

trait Module {

  def start(receiver: ActorRef)(implicit actorSystem: ActorSystem, config: Config)

}
