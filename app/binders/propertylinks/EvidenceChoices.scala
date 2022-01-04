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

package binders.propertylinks

import play.api.libs.json.Format
import play.api.mvc.PathBindable
import utils.{Cats, JsonUtils}

import scala.util.Try

object EvidenceChoices extends Enumeration {
  type EvidenceChoices = Value

  val RATES_BILL = Value("RATES_BILL")
  val OTHER = Value("OTHER")

  implicit val format: Format[EvidenceChoices] = JsonUtils.enumFormat(EvidenceChoices)

  implicit object Binder extends PathBindable[EvidenceChoices] with Cats {

    override def bind(key: String, value: String): Either[String, EvidenceChoices] =
      Try(EvidenceChoices.withName(value)).toOption
        .map(_.asRight)
        .getOrElse("".asLeft)

    override def unbind(key: String, value: EvidenceChoices): String = value.toString

  }
}
