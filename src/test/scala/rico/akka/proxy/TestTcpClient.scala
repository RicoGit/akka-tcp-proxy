package rico.akka.proxy

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.{IO, Tcp}
import akka.io.Tcp.Connect
import akka.testkit.TestProbe

/**
 * User: Constantine Solovev
 * Created: 17.07.17  8:39
 */


class TestTcpClient(remoteAddress: InetSocketAddress, testProbe: TestProbe) extends Actor with ActorLogging {
  import scala.concurrent.duration._

  implicit val system = context.system
  IO(Tcp) ! Connect(remoteAddress, timeout = Some(3.seconds))

  override def receive: Receive = {
    case msg =>
      log.info(s"TestTcpServer received $msg")
      testProbe.ref.tell(msg, sender())
  }
}
object TestTcpClient {
  def props(remoteAddress: InetSocketAddress, testProbe: TestProbe): Props =
    Props(classOf[TestTcpClient], remoteAddress, testProbe)
}
