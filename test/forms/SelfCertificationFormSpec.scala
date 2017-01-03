/*
 * Copyright 2017 HM Revenue & Customs
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

import controllers.{ConfirmSelfCertification, SelfCertification}
import org.scalatest.{FlatSpec, MustMatchers}
import utils.FormBindingVerification._
import views.helpers.Errors

class SelfCertificationFormSpec extends FlatSpec with MustMatchers {
  val form = SelfCertification.selfCertifyForm

  behavior of "Capacity declaration form"

  it should "bind when the inputs are all valid" in {
    mustBindTo(form, Map("iAgree" -> "true"), ConfirmSelfCertification(true))
  }

  it should "only allow true for iAgree" in {
    verifyTrue(form, "iAgree", Errors.mustAgreeToSelfCert)
  }

  it should "mandate a response to iAgree" in {
    verifyMandatory(form, Map("iAgree" -> "true"), "iAgree")
  }
}
