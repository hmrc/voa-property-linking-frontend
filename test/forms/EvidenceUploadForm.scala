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

import controllers.{UploadEvidence, UploadedEvidence}
import models.{DoesHaveEvidence, HasEvidence}
import org.scalatest.{FlatSpec, MustMatchers}
import utils.FormBindingVerification._

class EvidenceUploadForm extends FlatSpec with MustMatchers {
  import TestData._

  behavior of "Evidence upload form"

  it should "bind to valid data" in {
    mustBindTo(form, validData, UploadedEvidence(DoesHaveEvidence))
  }

  it should "mandate a response to has evidence" in {
    verifyMandatoryMultiChoice(form, validData, "hasEvidence")
  }

  it should "only allow recognised hasEvidence values" in {
    verifyMultiChoice(form, validData, "hasEvidence", HasEvidence)
  }

  object TestData {
    val form = UploadEvidence.form
    val validData = Map("hasEvidence" -> DoesHaveEvidence.name)
  }
}
