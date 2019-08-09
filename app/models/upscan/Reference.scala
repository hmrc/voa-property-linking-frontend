package models.upscan

import play.api.libs.json._

case class Reference(value: String) extends AnyVal

object Reference {
  implicit val referenceFormat: Format[Reference] =
    Format(Reads.of[String].map(Reference(_)), Writes(ref => JsString(ref.value)))
}
