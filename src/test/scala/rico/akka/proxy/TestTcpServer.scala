package rico.akka.proxy

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}

/**
 * User: Constantine Solovev
 * Created: 17.07.17  10:23
 */


class TestTcpServer(address: InetSocketAddress, listener: ActorRef) extends Actor with ActorLogging{
  import Tcp._
  import context.system

  override def preStart(): Unit = {
    IO(Tcp) ! Bind(self, new InetSocketAddress(address.getHostName, address.getPort))
  }

  def receive = {
    case b @ Bound(localAddress) =>
      context.parent ! b

    case CommandFailed(_: Bind) =>
      context stop self

    case c @ Connected(remote, local) =>
      val connection = sender()
      connection ! Register(listener)
      listener ! connection
  }
}
object TestTcpServer {
  def props(address: InetSocketAddress, listener: ActorRef): Props = Props(classOf[TestTcpServer], address, listener)
}
