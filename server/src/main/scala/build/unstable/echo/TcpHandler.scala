package build.unstable.echo

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.io.Tcp

import scala.util.control.NonFatal

class TcpSupervisor extends Actor with ActorLogging {

  override def supervisorStrategy = OneForOneStrategy(loggingEnabled = true) {
    case NonFatal(_) ⇒ Stop
  }

  def listening(listener: ActorRef): Receive = {

    case c@Tcp.Connected(remote, local) =>
    //  log.info("new connection from {}", remote)
      val connection = sender()
      val handler = context.actorOf(Props(classOf[TcpHandler], connection))
      connection ! Tcp.Register(handler)
      listener ! Tcp.ResumeAccepting(1)
  }

  override def receive: Actor.Receive = {

    case Tcp.CommandFailed(_: Tcp.Bind) ⇒ context stop self

    case b@Tcp.Bound(localAddress) ⇒
      log.info("ready and listening for new connections on {}", localAddress)
      val listener = sender()
      listener ! Tcp.ResumeAccepting(1)
      context.become(listening(listener))
  }
}

object TcpHandler {

  case object Ack extends Tcp.Event

}

class TcpHandler(connection: ActorRef) extends Actor with ActorLogging {

  import TcpHandler._
  import akka.io.Tcp._

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context watch connection
    connection ! ResumeReading
  }

  def receive: Receive = {

    case ConfirmedClosed | Terminated(_) ⇒
      context.stop(self)

    case PeerClosed ⇒ log.debug("peer closed")
      connection ! ConfirmedClose

    case e@Received(data) ⇒
      connection ! Write(data, Ack)

      context.become({
        
        case Ack ⇒
          connection ! ResumeReading
          context.unbecome()

        case WritingResumed ⇒ connection ! Write(data, Ack)

        case Tcp.CommandFailed(Write(_, _)) =>
          connection ! ResumeWriting

      }, discardOld = false)
  }
}
