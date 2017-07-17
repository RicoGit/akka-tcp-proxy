package rico.akka.proxy

import java.time.Instant

import rico.akka.proxy.domain.Candle

import scala.util.Random

/**
 * User: Constantine Solovev
 * Created: 16.07.17  12:56
 */

object TestUtils {

  val DefaultInstant: Instant = Instant.parse("2016-12-03T10:15:30Z")

  def createCandle(
    ticker: String = Random.nextString(4),
    timestamp: Instant = DefaultInstant,
    open: Double = Random.nextDouble() * 100,
    close: Double = Random.nextDouble() * 100,
    high: Double = Random.nextDouble() * 100,
    low: Double = Random.nextDouble() * 100,
    vol: Int = Random.nextInt(500)
  ): Candle =
    Candle(ticker, timestamp, open, close, high, low, vol)

}


