package rico.akka.proxy.domain

import java.nio.ByteBuffer

import akka.util.ByteString
import org.scalatest.{Matchers, WordSpec}
import rico.akka.proxy.InputFormat

/**
 * User: Constantine Solovev
 * Created: 16.07.17  21:36
 */


class WarrantSpec extends WordSpec with Matchers {

  val DefaultFormat = InputFormat(2, 8, 2, 8, 4)

  "apply" should {
    "throw Exception" when {
      "input is blank" in {
        Warrant(ByteString.empty, DefaultFormat)
      }
    }
    "return Warrant" when {
      "Default config" in {

        val buffer = ByteBuffer.allocate(28)
        buffer.putShort(25)
        buffer.putLong(1500227757505L)
        buffer.putShort(4)
        buffer.put("GOOG".getBytes)
        buffer.putDouble(10.25)
        buffer.putInt(100)

        Warrant(ByteString(buffer.array()), DefaultFormat) shouldBe Warrant(1500227757505L,"GOOG",10.25D,100)
      }
    }
  }

}
