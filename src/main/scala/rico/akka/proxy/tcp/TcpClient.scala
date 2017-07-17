

package rico.akka.proxy.tcp

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.{DelayOverflowStrategy, Materializer}
import akka.util.ByteString
import akka.Done
import org.slf4j.LoggerFactory


/**
 * User: Constantine Solovev
 * Created: 13.07.17  11:46
 */

/**
 * Manages tcp connection with external tcp server.
 */
class TcpClient(clientConfig: InetSocketAddress)(implicit as: ActorSystem, m: Materializer) {


  import TcpClient._
  import scala.concurrent.duration._
  import akka.stream.scaladsl._

  implicit val executionContext = as.dispatcher


  def start(consumerSink: Sink[ByteString, AnyRef]): Unit = {

    val connection = Tcp().outgoingConnection(clientConfig.getHostName, clientConfig.getPort)

    Source
      .repeat(connection)
      .mapAsync(1)(flow => {

        Source.single(ByteString.empty)
          .via(flow)
          .watchTermination()(Keep.right)
          .to(consumerSink)
          .run()
          .recover { case error => Done }

      })
      .delay(5.seconds, DelayOverflowStrategy.dropNew)
      .runWith(Sink.ignore)

  }

}

object TcpClient {
  val log = LoggerFactory.getLogger(classOf[TcpClient])

  def apply(clientConfig: InetSocketAddress)(implicit as: ActorSystem, m: Materializer): TcpClient =
    new TcpClient(clientConfig)
}
