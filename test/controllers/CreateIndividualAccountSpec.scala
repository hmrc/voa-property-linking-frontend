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

package controllers

import models.{DetailedIndividualAccount, PersonalDetails}
import org.scalacheck.Arbitrary.arbitrary
import play.api.Logger
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderNames
import utils._

class CreateIndividualAccountSpec extends ControllerSpec {

  private object TestCreateIndividualAccount extends CreateIndividualAccount {
    override lazy val ggAction = StubGGAction
    override lazy val auth = StubAuthConnector
    override lazy val individuals = StubIndividualAccountConnector
    override lazy val keystore = StubKeystore
  }

  "Going to the create individual account page, when logged in with an account that has not registered" should "display the create individual account form" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)

    val res = TestCreateIndividualAccount.show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    mustContainIndividualAccountForm(html)
  }

  "Submitting an invalid form" should "return a bad request response" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)

    val res = TestCreateIndividualAccount.submit()(FakeRequest())
    status(res) mustBe BAD_REQUEST
  }

  "Submitting a valid form" should "redirect to SIV" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)

    val individualAccount: PersonalDetails = arbitrary[PersonalDetails]

    val validFormData: Seq[(String, String)] = Seq(
      "fname" -> individualAccount.firstName,
      "lname" -> individualAccount.lastName,
      "email" -> individualAccount.email,
      "confirmedEmail" -> individualAccount.email,
      "phone1" -> individualAccount.phone1,
      "phone2" -> "",
      "nino.nino" -> arbitrary[Nino].value,
      "dob.day" -> individualAccount.dateOfBirth.getDayOfMonth.toString,
      "dob.month" -> individualAccount.dateOfBirth.getMonthOfYear.toString,
      "dob.year" -> individualAccount.dateOfBirth.getYear.toString,
      "address.line1" -> shortString,
      "address.postcode" -> "AA11 1AA"
    )

    Logger.info(individualAccount.toString)

    val res = TestCreateIndividualAccount.submit()(FakeRequest().withFormUrlEncodedBody(validFormData:_*).withHeaders(HeaderNames.xSessionId -> shortString))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.IdentityVerification.startIv().url)
  }

  "Going to the individual account page, when logged in with an account that has already registered" should "redirect to the dashboard page" in {
    val individualAccount: DetailedIndividualAccount = arbitrary[DetailedIndividualAccount]

    StubAuthConnector.stubGroupId(shortString)
    StubAuthConnector.stubExternalId(individualAccount.externalId)
    StubIndividualAccountConnector.stubAccount(individualAccount)

    val res = TestCreateIndividualAccount.show()(FakeRequest())
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.Dashboard.home.url)
  }

  private def mustContainIndividualAccountForm(html: HtmlPage) = {
    html.mustContainTextInput("#fname")
    html.mustContainTextInput("#lname")
    html.mustContainTextInput("#email")
    html.mustContainTextInput("#confirmedEmail")
    html.mustContainTextInput("#phone1")
    html.mustContainTextInput("#phone2")
    html.mustContainDateSelect("dob")
    html.mustContainTextInput("#ninonino")
    html.mustContainTextInput("#addressline1")
    html.mustContainTextInput("#addressline2")
    html.mustContainTextInput("#addressline3")
    html.mustContainTextInput("#addressline4")
    html.mustContainTextInput("#addresspostcode")
  }
}
