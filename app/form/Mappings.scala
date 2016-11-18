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

package form

import models.{NamedEnum, NamedEnumSupport}
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data.format.{Formats, Formatter}
import play.api.data.validation.Constraints
import play.api.data.{FormError, Forms, Mapping}
import views.helpers.Errors

import scala.util.Try

object Mappings extends DateMappings {

  def trueOnly(error: String): Mapping[Boolean] =
    text.verifying(error, _ == "true").transform[Boolean](_.toBoolean, _.toString)

  def mandatoryBoolean: Mapping[Boolean] = optional(boolean).verifying("error.required", _.isDefined).transform(_.get, Some.apply)
}

trait DateMappings {
  def dmyDate: Mapping[DateTime] = toDateTime(dmyDateTuple)

  def dmyPastDate: Mapping[DateTime] = toDateTime(dmyPastDateTuple)

  private def toDateTime(m: Mapping[(Int, Int, Int)]): Mapping[DateTime] = m.transform[DateTime](
    x => new DateTime(x._3, x._2, x._1, 0, 0, 0, 0),
    x => (x.getDayOfMonth, x.getMonthOfYear, x.getYear)
  )

  private def dmyPastDateTuple = dmyDateTuple.verifying(
    Errors.dateMustBeInPast,
    x => new DateTime(x._3, x._2, x._1, 0, 0, 0, 0).isBefore(DateTime.now.withTimeAtStartOfDay)
  )

  private def dmyDateTuple: Mapping[(Int, Int, Int)] = mapping(
    "day" -> number(1, 31),
    "month" -> number(1, 12),
    "year" -> number(1900, 3000)
  )((d, m, y) => (d, m, y))(Some(_)).verifying(Errors.invalidDate, x => Try(new DateTime(x._3, x._2, x._1, 0, 0, 0, 0)).isSuccess)


  private def number(min: Int, max: Int) = Forms.of[Int](trimmingNumberFormatter).verifying(Constraints.min(min)).verifying(Constraints.max(max))

  implicit lazy val trimmingNumberFormatter = new Formatter[Int] {
    override val format: Option[(String, Seq[Any])] = Formats.intFormat.format

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
      Formats.intFormat.bind(key, data.map(x => (x._1, x._2.trim)))

    override def unbind(key: String, value: Int): Map[String, String] = Formats.intFormat.unbind(key, value)
  }
}

object EnumMapping {
  def apply[T <: NamedEnum](named:NamedEnumSupport[T]) = Forms.of[T](new Formatter[T] {

    override val format = Some((Errors.noValueSelected, Nil))

    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], T] = {
      val resOpt = for {
        keyVal <- data.get(key)
        enumTypeValue <- named.fromName(keyVal)
      } yield {
        Right(enumTypeValue)
      }
      resOpt.getOrElse(Left(Seq(FormError(key, Errors.noValueSelected, Nil))))
    }

    def unbind(key: String, value: T): Map[String, String] = Map(key -> value.name)
  })
}
