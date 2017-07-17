package rico.akka.proxy

import java.net.InetSocketAddress
import java.time.temporal.ChronoUnit

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.util.ByteString
import com.github.kardapoltsev.json4s.javatime.JavaTimeSerializers
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader
import org.json4s.jackson.Serialization
import rico.akka.proxy.CandleStore.{Candles, Command, Stopped}
import rico.akka.proxy.domain.{Candle, Warrant}
import rico.akka.proxy.tcp.{TcpClient, TcpServer}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * User: Constantine Solovev
 * Created: 11.07.17  16:55
 */

/**
 * Application entry point.
 */
object App {

  private val ConfigPath = "akkaTcpProxy"

  def main(args: Array[String]): Unit = {

    import TypesafeConversions._
    import akka.stream._
    import akka.stream.scaladsl._
    import org.json4s._


    val config = ConfigFactory.load()
    val appConfig: AppConfig = config.as[AppConfig](ConfigPath)

    implicit val system = ActorSystem("tcp-proxy-system", config)
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val jsonDefaultFormats = DefaultFormats ++ JavaTimeSerializers.defaults

    val candleStoreActor = system.actorOf(CandleStore.props(appConfig.candleStore), "candleStore")

    val mapToWarrant = Warrant.mapToWarrantFlow(appConfig.input)
    val mapToCandle = Candle.mapToCandleFlow()

    val sink = mapToWarrant
      .via(mapToCandle)
      .to(Sink.actorRef(candleStoreActor, CandleStore.Paused))

    TcpClient(appConfig.network.client).start(sink)

    val source: Source[Command, ActorRef] =
      Source
        .actorRef[Command](5, OverflowStrategy.dropHead)
        .takeWhile(_ != Stopped)

    val flow: Flow[Command, ByteString, NotUsed] =
      Flow[Command]
        .collect { case Candles(cs) => cs.toList }
        .mapConcat(identity)
        .map(candle => Serialization.write(candle) + appConfig.output.separator)
        .map(str => ByteString(str))

    TcpServer(appConfig.network.server, candleStoreActor).start(source, flow, Sink.ignore)

    Await.ready(system.whenTerminated, Duration.Inf)

  }

}

/* CONFIGS */


case class AppConfig(
  network: NetworkConfig,
  output: OutputFormat,
  input: InputFormat,
  candleStore: CandleStoreConfig
)

case class OutputFormat(separator: String)

/** Contains length in bytes of all fields of [[Warrant]]. */
case class InputFormat(len: Int, timestamp: Int, tickerLen: Int, price: Int, size: Int) {
  require(
    len <= 8 && timestamp <= 8 && tickerLen <= 8 && price <= 8 && size <= 8,
    "Any fields length shouldn't be more than 8 bytes")

  val MaxLen = Math.pow(2, 8L * len).toInt
}

case class NetworkConfig(client: InetSocketAddress, server: InetSocketAddress)

case class CandleStoreConfig(aggregateBy: ChronoUnit, storeSize: Int)


object TypesafeConversions {
  implicit val chronoUnitReader: ValueReader[CandleStoreConfig] =
    ValueReader.relative { config => CandleStoreConfig(ChronoUnit.valueOf(config.getString("aggregateBy")), config.getInt("storeSize")) }

  implicit val inetSocketReader: ValueReader[InetSocketAddress] =
    ValueReader.relative { config => new InetSocketAddress(config.getString("host"), config.getInt("port")) }
}


