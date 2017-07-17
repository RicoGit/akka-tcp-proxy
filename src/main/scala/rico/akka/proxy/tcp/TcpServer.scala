package rico.akka.proxy.tcp

import java.net.InetSocketAddress

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp}
import akka.util.ByteString
import org.slf4j.LoggerFactory
import rico.akka.proxy.CandleStore.Register

import scala.concurrent.Future

/**
 * User: Constantine Solovev
 * Created: 15.07.17  18:24
 */


/**
 * Tcp server, manages connection.
 */
class TcpServer(address: InetSocketAddress, listener: ActorRef)
               (implicit as: ActorSystem, m: Materializer) {

  import TcpServer._

  val connections: Source[IncomingConnection, Future[ServerBinding]] =
    Tcp().bind(address.getHostName, address.getPort)

  def start[T](source: Source[T, ActorRef], flow: Flow[T, ByteString, NotUsed], sink: Sink[ByteString, AnyRef]): Unit = {
    // handle connections
    connections.runForeach(connection => {
      log.info(s"New connection from: ${connection.remoteAddress}")

      val newClient =
        flow.via(connection.flow)
          .to(sink)
          .runWith(source)

      listener ! Register(newClient)

      log.info(s"Registering new connection.")
    })
  }

}

object TcpServer {
  val log = LoggerFactory.getLogger(classOf[TcpServer])

  def apply(clientConfig: InetSocketAddress, listener: ActorRef)
           (implicit as: ActorSystem, m: Materializer): TcpServer =
    new TcpServer(clientConfig, listener: ActorRef)

}
