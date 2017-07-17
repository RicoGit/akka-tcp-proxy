package rico.akka.proxy

import java.time.Instant

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import rico.akka.proxy.domain.Candle

import scala.collection.mutable

/**
 * User: Constantine Solovev
 * Created: 14.07.17  9:12
 */

class CandleStore(config: CandleStoreConfig) extends Actor with ActorLogging {

  import CandleStore._

  private val candleStorage = mutable.LinkedHashMap[Instant, Map[String, Candle]]()
  private val listeners = mutable.Set[ActorRef]()
  private var previousTimestamp = Instant.MAX


  override def receive: Receive = {

    case newCandle: Candle =>
      log.debug(s"Receive new candle $newCandle")

      val currentTimestamp = newCandle.timestamp.truncatedTo(config.aggregateBy)

      val currentTimestampTickerMap = candleStorage.getOrElse(currentTimestamp, Map())

      val mergedCandle =
        currentTimestampTickerMap.get(newCandle.ticker)
          .map(storedCandle => merge(storedCandle, newCandle))
          .getOrElse(newCandle)
          .copy(timestamp = currentTimestamp)

      candleStorage += (currentTimestamp -> currentTimestampTickerMap.updated(newCandle.ticker, mergedCandle))

      if (currentTimestamp.isAfter(previousTimestamp)) {

        if (listeners.nonEmpty) {
          val lastMinutesCandles = candleStorage(previousTimestamp).values
          log.info(s"Sending minute candle $lastMinutesCandles")
          listeners.foreach(actor => actor ! Candles(lastMinutesCandles.toSeq))
        }

        cleanup()
      }

      previousTimestamp = currentTimestamp

    case Register(listener) =>
      listeners += listener
      log.info(s"New client registered, total number of client is ${listeners.size}")
      context watch listener

      listener ! getLastCandles(config.storeSize)
      log.info(s"Candles for ${config.storeSize} ${config.aggregateBy} were sent")

    case Paused =>
      log.warning("CandleStore paused ...")

    case Stopped =>
      log.warning("CandleStore stopped ...")
      listeners.foreach(_ ! Paused)
      context stop self


    case Terminated(listener) =>
      listeners -= listener
      log.info(s"Client was terminated, total number of client is ${listeners.size}")
      context unwatch listener

  }

  private def getLastCandles(number: Int): Candles = {
    Candles(candleStorage.takeRight(number).values.flatMap(_.values))
  }

  /**
   * We should cleanup candles storage. Only last [[config.storeSize]] should be in storage.
   */
  private def cleanup(): Unit = {

    if (candleStorage.size > config.storeSize) {
      val oldestCandle = candleStorage.head
      candleStorage.remove(oldestCandle._1)
      log.info(s"Oldest candle was deleted $oldestCandle")
    }
  }

  /**
   * Merge 2 candles for the same ticker and the same aggregation time unit.
   * @param oldest candle which came earlier (tcp guarantees order)
   * @param newest candle which came later (tcp guarantees order)
   */
  private def merge(oldest: Candle, newest: Candle): Candle = {

    oldest.copy(
      close = newest.close,
      high = Math.max(oldest.high, newest.high),
      low  = Math.min(oldest.low, newest.low),
      volume = oldest.volume + newest.volume
    )
  }

}
object CandleStore {

  def props(config: CandleStoreConfig): Props = Props(classOf[CandleStore], config)

  sealed trait Command
  case class Paused(cause: String) extends Command
  case class Stopped(cause: String) extends Command
  case class Candles(candles: Iterable[Candle]) extends Command
  case class Register(actor: ActorRef)  extends Command
}
