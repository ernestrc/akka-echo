package build.unstable.echo

import java.net.InetSocketAddress

import akka.actor.{ActorSystem, Props}
import akka.io.{IO, Tcp}

object Server extends App {
  implicit val system = ActorSystem()
  val tcpService = system.actorOf(Props[TcpSupervisor])

  IO(Tcp).tell(Tcp.Bind(tcpService,
    new InetSocketAddress("0.0.0.0", 19998), options = Nil, pullMode = true), tcpService)
}
