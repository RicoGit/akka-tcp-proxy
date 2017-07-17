package rico.akka.proxy.tcp

import akka.actor.{ActorRef, ActorSystem}
import akka.io.Tcp.Connected
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.testkit.{SocketUtil, TestKit, TestProbe}
import akka.util.ByteString
import org.scalatest.{Matchers, WordSpecLike}
import rico.akka.proxy.CandleStore.{Command, Register}
import rico.akka.proxy.TestTcpClient

/**
 * User: Constantine Solovev
 * Created: 16.07.17  23:09
 */


class TcpServerSpec extends TestKit(ActorSystem("TcpServerSpec")) with WordSpecLike with Matchers{

  val address = SocketUtil.temporaryServerAddress()

  "TcpServer" should {
    "register new connection" in {

      implicit val materializer = ActorMaterializer()

      val serverConProbe = TestProbe()
      val clientConProbe = TestProbe()
      val registrar = TestProbe()

      val source: Source[Command, ActorRef] = Source
        .actorRef[Command](5, OverflowStrategy.dropHead)
        .mapMaterializedValue(_ => serverConProbe.ref)

      val flow = Flow.fromFunction((cmd: Command) => ByteString.empty)

      val sink = Sink.actorRef(registrar.ref, "done")

      TcpServer(address, registrar.ref).start(source, flow, TestSink.probe[ByteString])


      system.actorOf(TestTcpClient.props(address, clientConProbe))

      clientConProbe.expectMsgType[Connected]
      registrar.expectMsg(Register(serverConProbe.ref))

    }

  }

}
