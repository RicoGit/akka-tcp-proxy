package rico.akka.proxy

import java.time.temporal.ChronoUnit.{MINUTES, SECONDS}

import akka.actor.{Actor, ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{Matchers, WordSpecLike}
import rico.akka.proxy.CandleStore.{Candles, Register}
import rico.akka.proxy.domain.Candle

/**
 * User: Constantine Solovev
 * Created: 16.07.17  12:54
 */


class CandleStoreSpec extends TestKit(ActorSystem("CandleStoreSpec")) with WordSpecLike with Matchers {

  val defaultConfig = CandleStoreConfig(MINUTES, 3)

  "CandleStore" should {
    "register listener and send nothing" when {
      "no one candles in store" in {
        val probe = TestProbe()
        val candleStoreActor = system.actorOf(CandleStore.props(defaultConfig))

        candleStoreActor.tell(Register(probe.testActor), candleStoreActor)

        probe.expectMsg(Candles(Nil))
      }
    }

    "register listener and send 1 candle"  when {

      "1 candle in store" in {
        val probe = TestProbe()
        val candleStoreActor = system.actorOf(CandleStore.props(defaultConfig))
        val candle = TestUtils.createCandle()

        candleStoreActor.tell(candle, candleStoreActor)
        candleStoreActor.tell(Register(probe.testActor), candleStoreActor)

        probe.expectMsg(Candles(Seq(truncateTimestamp(candle))))
      }

      "2 candles in store for one ticker and 1 minute" in {
        val probe = TestProbe()
        val candleStoreActor = system.actorOf(CandleStore.props(defaultConfig))

        val timestamp = TestUtils.DefaultInstant
        val appl1 = TestUtils.createCandle("Appl", timestamp, 10D, 10D, 10D, 10D, 100)
        val appl2 = TestUtils.createCandle("Appl", timestamp.plus(10, SECONDS), 20D, 20D, 20D, 20D, 100)

        candleStoreActor.tell(appl1, candleStoreActor)
        candleStoreActor.tell(appl2, candleStoreActor)
        candleStoreActor.tell(Register(probe.testActor), candleStoreActor)

        probe.expectMsg(Candles(Seq(Candle("Appl", timestamp.truncatedTo(MINUTES), 10D, 20D, 20D, 10D, 200))))
      }
    }

    "register listener and send candle for each ticker" when {

      "3 candles for one minute present" in {
        val probe = TestProbe()
        val candleStoreActor = system.actorOf(CandleStore.props(defaultConfig))

        val timestamp = TestUtils.DefaultInstant
        val appl = TestUtils.createCandle("Appl", timestamp, 10D, 10D, 10D, 10D, 100)
        val yand = TestUtils.createCandle("Yand", timestamp.plus(10, SECONDS), 30D, 30D, 30D, 30D, 100)
        val tsla = TestUtils.createCandle("Tsla", timestamp.plus(20, SECONDS), 20D, 20D, 20D, 20D, 100)

        candleStoreActor.tell(appl, candleStoreActor)
        candleStoreActor.tell(yand, candleStoreActor)
        candleStoreActor.tell(tsla, candleStoreActor)
        candleStoreActor.tell(Register(probe.testActor), candleStoreActor)

        probe.expectMsg(Candles(Seq(
          appl.copy(timestamp = appl.timestamp.truncatedTo(MINUTES)),
          yand.copy(timestamp = yand.timestamp.truncatedTo(MINUTES)),
          tsla.copy(timestamp = yand.timestamp.truncatedTo(MINUTES))
        )))
      }

      "few candles for 20 minutes present" in {
        val probe = TestProbe()
        val candleStoreActor = system.actorOf(CandleStore.props(defaultConfig))

        val timestamp = TestUtils.DefaultInstant

        val candles = (0 to 5).flatMap(index => {
            Seq(
              TestUtils.createCandle("Appl", timestamp.plus(index, MINUTES), 10D, 10D, 10D, 10D, 100),
              TestUtils.createCandle("Appl", timestamp.plus(index, MINUTES).plus(index, SECONDS), 15D, 15D, 15D, 15D, 100),

              TestUtils.createCandle("Yand", timestamp.plus(index, MINUTES), 30D, 30D, 30D, 30D, 100),
              TestUtils.createCandle("Yand", timestamp.plus(index, MINUTES).plus(index, SECONDS), 35D, 35D, 35D, 35D, 100),

              TestUtils.createCandle("Tsla", timestamp.plus(index, MINUTES), 20D, 20D, 20D, 20D, 100),
              TestUtils.createCandle("Tsla", timestamp.plus(index, MINUTES).plus(index, SECONDS), 25D, 25D, 25D, 25D, 100)
            )
        }).foreach(x => candleStoreActor.tell(x, candleStoreActor))

        candleStoreActor.tell(Register(probe.testActor), candleStoreActor)

        probe.expectMsg(Candles(
          (3 to 5).flatMap(index => {
            Seq(
              TestUtils.createCandle("Appl", timestamp.plus(index, MINUTES).truncatedTo(MINUTES), 10D, 15D, 15D, 10D, 200),
              TestUtils.createCandle("Yand", timestamp.plus(index, MINUTES).truncatedTo(MINUTES), 30D, 35D, 35D, 30D, 200),
              TestUtils.createCandle("Tsla", timestamp.plus(index, MINUTES).truncatedTo(MINUTES), 20D, 25D, 25D, 20D, 200)
            )
          })
        ))
      }
    }

    "register 2 listeners and send 1 candle" when {

      "1 candle in store" in {
        val probe1 = TestProbe()
        val probe2 = TestProbe()
        val candleStoreActor = system.actorOf(CandleStore.props(defaultConfig))

        val timestamp = TestUtils.DefaultInstant
        val appl1 = TestUtils.createCandle("Appl", timestamp, 10D, 10D, 10D, 10D, 100)
        val appl2 = TestUtils.createCandle("Appl", timestamp.plus(10, SECONDS), 20D, 20D, 20D, 20D, 100)

        candleStoreActor.tell(appl1, candleStoreActor)
        candleStoreActor.tell(appl2, candleStoreActor)
        candleStoreActor.tell(Register(probe1.testActor), candleStoreActor)
        candleStoreActor.tell(Register(probe2.testActor), candleStoreActor)

        probe1.expectMsg(Candles(Seq(Candle("Appl", timestamp.truncatedTo(MINUTES), 10D, 20D, 20D, 10D, 200))))
        probe2.expectMsg(Candles(Seq(Candle("Appl", timestamp.truncatedTo(MINUTES), 10D, 20D, 20D, 10D, 200))))
      }


      "2 candles in store for one ticker and 1 minute" in {
        val probe1 = TestProbe()
        val probe2 = TestProbe()
        val candleStoreActor = system.actorOf(CandleStore.props(defaultConfig))

        val timestamp = TestUtils.DefaultInstant
        val appl1 = TestUtils.createCandle("Appl", timestamp, 10D, 10D, 10D, 10D, 100)
        val appl2 = TestUtils.createCandle("Appl", timestamp.plus(10, SECONDS), 20D, 20D, 20D, 20D, 100)

        candleStoreActor.tell(appl1, candleStoreActor)
        candleStoreActor.tell(appl2, candleStoreActor)
        candleStoreActor.tell(Register(probe1.testActor), candleStoreActor)
        candleStoreActor.tell(Register(probe2.testActor), candleStoreActor)

        probe1.expectMsg(Candles(Seq(Candle("Appl", timestamp.truncatedTo(MINUTES), 10D, 20D, 20D, 10D, 200))))
        probe2.expectMsg(Candles(Seq(Candle("Appl", timestamp.truncatedTo(MINUTES), 10D, 20D, 20D, 10D, 200))))
      }

      "3 candles for one minute present" in {
        val probe1 = TestProbe()
        val probe2 = TestProbe()
        val candleStoreActor = system.actorOf(CandleStore.props(defaultConfig))

        val timestamp = TestUtils.DefaultInstant
        val appl = TestUtils.createCandle("Appl", timestamp, 10D, 10D, 10D, 10D, 100)
        val yand = TestUtils.createCandle("Yand", timestamp.plus(10, SECONDS), 30D, 30D, 30D, 30D, 100)
        val tsla = TestUtils.createCandle("Tsla", timestamp.plus(20, SECONDS), 20D, 20D, 20D, 20D, 100)

        candleStoreActor.tell(appl, candleStoreActor)
        candleStoreActor.tell(yand, candleStoreActor)
        candleStoreActor.tell(tsla, candleStoreActor)
        candleStoreActor.tell(Register(probe1.testActor), candleStoreActor)
        candleStoreActor.tell(Register(probe2.testActor), candleStoreActor)

        probe1.expectMsg(Candles(Seq(
          appl.copy(timestamp = appl.timestamp.truncatedTo(MINUTES)),
          yand.copy(timestamp = yand.timestamp.truncatedTo(MINUTES)),
          tsla.copy(timestamp = yand.timestamp.truncatedTo(MINUTES))
        )))

        probe2.expectMsg(Candles(Seq(
          appl.copy(timestamp = appl.timestamp.truncatedTo(MINUTES)),
          yand.copy(timestamp = yand.timestamp.truncatedTo(MINUTES)),
          tsla.copy(timestamp = yand.timestamp.truncatedTo(MINUTES))
        )))
      }

      "few candles for 20 minutes present" in {
        val probe1 = TestProbe()
        val probe2 = TestProbe()
        val candleStoreActor = system.actorOf(CandleStore.props(defaultConfig))

        val timestamp = TestUtils.DefaultInstant

        val candles = (0 to 5).flatMap(index => {
          Seq(
            TestUtils.createCandle("Appl", timestamp.plus(index, MINUTES), 10D, 10D, 10D, 10D, 100),
            TestUtils.createCandle("Appl", timestamp.plus(index, MINUTES).plus(index, SECONDS), 15D, 15D, 15D, 15D, 100),

            TestUtils.createCandle("Yand", timestamp.plus(index, MINUTES), 30D, 30D, 30D, 30D, 100),
            TestUtils.createCandle("Yand", timestamp.plus(index, MINUTES).plus(index, SECONDS), 35D, 35D, 35D, 35D, 100),

            TestUtils.createCandle("Tsla", timestamp.plus(index, MINUTES), 20D, 20D, 20D, 20D, 100),
            TestUtils.createCandle("Tsla", timestamp.plus(index, MINUTES).plus(index, SECONDS), 25D, 25D, 25D, 25D, 100)
          )
        }).foreach(x => candleStoreActor.tell(x, candleStoreActor))

        candleStoreActor.tell(Register(probe1.testActor), candleStoreActor)
        candleStoreActor.tell(Register(probe2.testActor), candleStoreActor)

        probe1.expectMsg(Candles(
          (3 to 5).flatMap(index => {
            Seq(
              TestUtils.createCandle("Appl", timestamp.plus(index, MINUTES).truncatedTo(MINUTES), 10D, 15D, 15D, 10D, 200),
              TestUtils.createCandle("Yand", timestamp.plus(index, MINUTES).truncatedTo(MINUTES), 30D, 35D, 35D, 30D, 200),
              TestUtils.createCandle("Tsla", timestamp.plus(index, MINUTES).truncatedTo(MINUTES), 20D, 25D, 25D, 20D, 200)
            )
          })
        ))

        probe2.expectMsg(Candles(
          (3 to 5).flatMap(index => {
            Seq(
              TestUtils.createCandle("Appl", timestamp.plus(index, MINUTES).truncatedTo(MINUTES), 10D, 15D, 15D, 10D, 200),
              TestUtils.createCandle("Yand", timestamp.plus(index, MINUTES).truncatedTo(MINUTES), 30D, 35D, 35D, 30D, 200),
              TestUtils.createCandle("Tsla", timestamp.plus(index, MINUTES).truncatedTo(MINUTES), 20D, 25D, 25D, 20D, 200)
            )
          })
        ))

      }

    }

    "send candles to listener" when {
      "new candles come" in {

        val probe = TestProbe()
        val candleStoreActor = system.actorOf(CandleStore.props(defaultConfig))

        candleStoreActor.tell(Register(probe.testActor), candleStoreActor)

        val timestamp = TestUtils.DefaultInstant
        val appl1 = TestUtils.createCandle("Appl", timestamp, 10D, 10D, 10D, 10D, 100)
        val appl2 = TestUtils.createCandle("Appl", timestamp.plus(1, MINUTES), 20D, 20D, 20D, 20D, 100)

        candleStoreActor.tell(appl1, candleStoreActor)
        candleStoreActor.tell(appl2, candleStoreActor)

        probe.expectMsg(Candles(Nil))
        probe.expectMsg(Candles(List(
          appl1.copy(timestamp = appl1.timestamp.truncatedTo(defaultConfig.aggregateBy))
        )))

      }
    }

    "Not sent to listener" when {
      "it was terminated" in {

        val probe1 = TestProbe()
        val probe2 = TestProbe()
        val candleStoreActor = system.actorOf(CandleStore.props(defaultConfig))

        candleStoreActor.tell(Register(probe1.testActor), candleStoreActor)
        candleStoreActor.tell(Register(probe2.testActor), candleStoreActor)

        probe1.expectMsg(Candles(Nil))
        probe2.expectMsg(Candles(Nil))

        probe2.ref ! PoisonPill

        val timestamp = TestUtils.DefaultInstant
        val appl1 = TestUtils.createCandle("Appl", timestamp, 10D, 10D, 10D, 10D, 100)
        val appl2 = TestUtils.createCandle("Appl", timestamp.plus(1, MINUTES), 20D, 20D, 20D, 20D, 100)

        candleStoreActor.tell(appl1, candleStoreActor)
        candleStoreActor.tell(appl2, candleStoreActor)

        probe1.expectMsg(Candles(List(
          appl1.copy(timestamp = appl1.timestamp.truncatedTo(defaultConfig.aggregateBy))
        )))
      }
    }

  }

  private def truncateTimestamp(candle: Candle): Candle = {
    candle.copy(timestamp = candle.timestamp.truncatedTo(MINUTES))
  }


}
