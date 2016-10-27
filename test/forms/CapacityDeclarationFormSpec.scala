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

package forms

import java.io.IOException

import connectors.CapacityDeclaration
import controllers.Search
import models.{CapacityType, Occupier}
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, MustMatchers}
import play.api.libs.ws.{WSClient, WSRequest}
import utils.FormBindingVerification._

class CapacityDeclarationFormSpec extends FlatSpec with MustMatchers {
  import TestData._

  behavior of "Capacity declaration form"

  it should "bind when the inputs are all valid" in {
    mustBindTo(form, validData, CapacityDeclaration(Occupier, new DateTime(2001, 12, 24, 0, 0, 0), Some(new DateTime(2011, 1, 13, 0, 0, 0))))
  }

  it should "mandate a capacity" in {
    verifyMandatoryMultiChoice(form, validData, "capacity")
  }

  it should "only allow recognised capacity values" in {
    verifyMultiChoice(form, validData, "capacity", CapacityType)
  }

  it should "mandate presence of from date" in {
    verifyMandatoryDate(form, validData, "fromDate")
  }

  it should "only allow dd/MM/yyy from dates before the current date" in {
    verifyPastddmmyy(form, validData, "fromDate")
  }

  it should "allow to date to be optional" in {
    verifyOptionalDate(form, validData, "toDate")
  }

  object TestData {
    val form = Search.declareCapacityForm
    val validData = Map(
      "capacity" -> Occupier.name, "fromDate.day" -> "24", "fromDate.month" -> "12", "fromDate.year" -> "2001",
      "toDate.day" -> "13", "toDate.month" -> "1", "toDate.year" -> "2011"
    )
  }
}
