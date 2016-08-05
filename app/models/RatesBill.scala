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

package models

import java.util.Base64

object RatesBill {
  def apply(s: String): RatesBill = Base64EncodedStringByteArrayRatesBill(s)
}

sealed trait RatesBill {
  def toB64String: String = this match {
    case ByteArrayRatesBill(data) => Base64EncodedStringByteArrayRatesBill(data).b64String
    case Base64EncodedStringByteArrayRatesBill(b64String) => b64String
  }
}

case class ByteArrayRatesBill(data: Array[Byte]) extends RatesBill

object Base64EncodedStringByteArrayRatesBill {
  def apply(data: Array[Byte]): Base64EncodedStringByteArrayRatesBill =
    Base64EncodedStringByteArrayRatesBill(Base64.getEncoder.encodeToString(data))
}

case class Base64EncodedStringByteArrayRatesBill(b64String: String) extends RatesBill
