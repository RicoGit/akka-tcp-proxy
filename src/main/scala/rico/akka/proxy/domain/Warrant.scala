package rico.akka.proxy.domain

import java.lang.{ Double => JDouble }
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Framing}
import akka.util.{ByteIterator, ByteString}
import rico.akka.proxy.InputFormat

/**
 * User: Constantine Solovev
 * Created: 11.07.17  16:09
 */


/**
 * Scala implementation of Warrant Exchange of following format:
 * {{{
 *  [ len:LEN_LEN ] [ timestamp:TIMESTAMP_LEN ] [ ticker_len:TICKER_LEN_LEN ] [ ticker:ticker_len ] [ price:PRICE_LEN ] [ size:SIZE_LEN ]
 * }}}
 * All lengths of fields (MESSAGE_LEN, TIMESTAMP_LEN, etc.) contains in [[InputFormat]].
 */
case class Warrant(timestamp: Long, ticker: String, price: Double, size: Int)

object Warrant {

  def apply(datum: ByteString, warrantFormat: InputFormat): Warrant = {

    require(datum.nonEmpty, "Shouldn't create warrant cause input data is empty.")

    val iter: ByteIterator = datum.iterator

    val len = getLong(iter, warrantFormat.len)
    val timestamp = getLong(iter, warrantFormat.timestamp)
    val tickerLen = getLong(iter, warrantFormat.tickerLen)
    val ticker = new String(iter.getBytes(tickerLen.toInt), StandardCharsets.US_ASCII)
    val price = getDouble(iter, warrantFormat.price)
    val size = getLong(iter, warrantFormat.size)

    Warrant(timestamp, ticker, price, size.toInt)
  }

  /**
   * Get specified number of bytes from iterator and convert its as long.
   */
  private def getLong(byteIter: ByteIterator, numberOfBytes: Int): Long =
    BigInt(byteIter.getBytes(numberOfBytes)).toLong
  /**
   * Get specified number of bytes from iterator and convert its as double.
   */
  private def getDouble(byteIter: ByteIterator, numberOfBytes: Int): Double =
    JDouble.longBitsToDouble(BigInt(byteIter.getBytes(numberOfBytes)).toLong)


  def mapToWarrantFlow(warrantFormat: InputFormat): Flow[ByteString, Warrant, NotUsed] = {
      Framing
        .lengthField(
          fieldLength = warrantFormat.len,
          fieldOffset = 0,
          maximumFrameLength = warrantFormat.MaxLen,
          byteOrder = ByteOrder.BIG_ENDIAN
        )
        .map(x => Warrant(x, warrantFormat))
  }

}


