package rico.akka.proxy.tcp

import akka.actor.{ActorRef, ActorSystem}
import akka.io.Tcp.Write
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.testkit.{SocketUtil, TestKit, TestProbe}
import akka.util.ByteString
import org.scalatest.{Matchers, WordSpecLike}
import rico.akka.proxy.TestTcpServer

/**
 * User: Constantine Solovev
 * Created: 16.07.17  23:09
 */


class TcpClientSpec extends TestKit(ActorSystem("TcpClientSpec")) with WordSpecLike with Matchers{

  val address = SocketUtil.temporaryServerAddress()

  "TcpClient" should {
    "connect to server and send all data to sink" in {

      implicit val materializer = ActorMaterializer()

      val serverConProbe = TestProbe()
      val clientConProbe = TestProbe()

      // start server
      system.actorOf(TestTcpServer.props(address, serverConProbe.ref))

      // start client
      TcpClient(address).start(Sink.actorRef(clientConProbe.ref, "done"))

      val connection = serverConProbe.expectMsgType[ActorRef]

      connection ! Write(ByteString("test1"))
      clientConProbe.expectMsg(ByteString("test1"))

    }

  }

}
