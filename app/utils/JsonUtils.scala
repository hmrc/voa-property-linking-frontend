package utils

import play.api.libs.json.{Format, Reads, Writes}

object JsonUtils {

  def enumFormat[E <: Enumeration](e: E): Format[E#Value] =
    Format(Reads.enumNameReads(e), Writes.enumNameWrites)

}
