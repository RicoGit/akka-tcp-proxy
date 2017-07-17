package rico.akka.proxy.domain

import java.time.Instant

import akka.NotUsed
import akka.stream.scaladsl.Flow

/**
 * User: Constantine Solovev
 * Created: 15.07.17  16:42
 */

case class Candle(ticker: String, timestamp: Instant, open: Double, close: Double, high: Double, low: Double, volume: Long)

object Candle {

  def apply(w: Warrant): Candle =
    Candle(w.ticker, Instant.ofEpochMilli(w.timestamp), w.price, w.price, w.price, w.price, w.size)

  def mapToCandleFlow(): Flow[Warrant, Candle, NotUsed] = Flow.fromFunction(Candle(_))

}