/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package serialization

import connectors._
import models._
import play.api.libs.json._
import session.LinkingSession

object JsonFormats {
  implicit val dateReads = Reads.jodaDateReads("yyyy-MM-dd")
  implicit val dateWrites = Writes.jodaDateWrites("yyyy-MM-dd")
  implicit val addressFormat = Json.format[Address]
  implicit val propertyDataFormat = Json.format[Property]
  implicit val propertyRepresentationsFormat = Json.format[PropertyRepresentation]
  implicit val capacityTypeFormat = EnumFormat(CapacityType)
  implicit val capacityFormat = Json.format[CapacityDeclaration]
  implicit val requestFlag = EnumFormat(LinkBasis)
  implicit val propertyLinkRequest = Json.format[PropertyLinkRequest]
  implicit val propertyLink = Json.format[PropertyLink]
  implicit val linkedProperties = Json.format[LinkedProperties]
  implicit val sessionFormat = Json.format[LinkingSession]
  implicit val ratesBillCheckFormat = Json.reads[RatesBillCheck]
}

object EnumFormat {
  def apply[T <: NamedEnum](enumObject: NamedEnumSupport[T]): Format[T] = Format[T](generateReads(enumObject), generateWrites(enumObject))

  private def generateWrites[T <: NamedEnum](enumObject: NamedEnumSupport[T]): Writes[T] = new Writes[T] {
    def writes(data: T): JsValue = {
      JsString(data.name)
    }
  }

  private def generateReads[T <: NamedEnum](enumObject: NamedEnumSupport[T]): Reads[T] = new Reads[T] {
    def reads(json: JsValue): JsResult[T] = json match {
      case JsString(value) =>
        enumObject.fromName(value) match {
          case Some(enumValue) => JsSuccess(enumValue)
          case None => JsError(s"Value: $value is not valid type of ${this.getClass}")
        }
      case js =>
        JsError(s"Invalid Json: expected string, got: $js")
    }
  }
}
