/*
 * Copyright 2022 HM Revenue & Customs
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

package models.domain

import play.api.libs.json.{Format, Reads, Writes}

case class Nino(nino: String) {
  require(Nino.isValid(nino), s"$nino is not a valid nino.")
  override val toString: String = nino

  private val LengthWithoutSuffix: Int = 8

  def value: String = nino

  val name = "nino"

  def formatted: String = value.grouped(2).mkString(" ")

  def withoutSuffix: String = value.take(LengthWithoutSuffix)
}

object Nino extends (String => Nino) {

  import play.api.libs.functional.syntax._

  implicit val format: Format[Nino] = Format(Reads.of[String].map(Nino), Writes.of[String].contramap(_.nino))

  private val validNinoFormat = "[[A-Z]&&[^DFIQUV]][[A-Z]&&[^DFIQUVO]] ?\\d{2} ?\\d{2} ?\\d{2} ?[A-D]{1}"
  private val invalidPrefixes = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")

  private def hasValidPrefix(nino: String) = !invalidPrefixes.exists(nino.startsWith)

  def isValid(nino: String): Boolean = nino != null && hasValidPrefix(nino) && nino.matches(validNinoFormat)

  private val validFirstCharacters =
    ('A' to 'Z').filterNot(List('D', 'F', 'I', 'Q', 'U', 'V').contains).map(_.toString)
  private val validSecondCharacters =
    ('A' to 'Z').filterNot(List('D', 'F', 'I', 'O', 'Q', 'U', 'V').contains).map(_.toString)
  val validPrefixes =
    validFirstCharacters.flatMap(a => validSecondCharacters.map(a + _)).filterNot(invalidPrefixes.contains(_))
  val validSuffixes = ('A' to 'D').map(_.toString)
}
