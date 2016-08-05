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

import controllers.{SubmitRatesBill, UploadRatesBill}
import models.{DoesHaveRatesBill, HasRatesBill}
import org.scalatest.{FlatSpec, MustMatchers}
import utils.FormBindingVerification._

class RatesBillUploadForm extends FlatSpec with MustMatchers {
  import TestData._

  behavior of "Upload rates bill form"

  it should "bind to valid data" in {
    mustBindTo(form, validData, SubmitRatesBill(DoesHaveRatesBill))
  }

  it should "mandate response to has rates bill" in {
    verifyMandatoryMultiChoice(form, validData, "hasRatesBill")
  }

  it should "only allow recognised has rates bill values" in {
    verifyMultiChoice(form, validData, "hasRatesBill", HasRatesBill)
  }

  object TestData {
    lazy val form = UploadRatesBill.uploadRatesBillForm
    lazy val validData = Map("hasRatesBill" -> DoesHaveRatesBill.name)
  }
}
