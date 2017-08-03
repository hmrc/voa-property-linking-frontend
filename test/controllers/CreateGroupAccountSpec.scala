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

import auth.GGAction
import connectors.{Authenticated, VPLAuthConnector}
import controllers.CreateGroupAccount.keys
import models.{Accounts, DetailedIndividualAccount, GroupAccount, PersonalDetails}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import utils._

import scala.concurrent.Future

class CreateGroupAccountSpec extends ControllerSpec with MockitoSugar {

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())
    ).thenReturn(Future.successful(()))
    when(f.saveOrUpdate(any())(any(), any())
    ).thenReturn(Future.successful(()))
    when(f.get[PersonalDetails](any(), any())).thenReturn(Future.successful(arbitrary[PersonalDetails].sample))
    f
  }
  private object TestCreateGroupAccount extends CreateGroupAccount(mockSessionRepo) {
    override lazy val auth: VPLAuthConnector = StubAuthConnector
    override lazy val ggAction: GGAction = StubGGAction
    override lazy val individuals = StubIndividualAccountConnector
    override lazy val groups = StubGroupAccountConnector
    override lazy val identityVerification = StubIdentityVerification
    override lazy val addresses = StubAddresses
  }

  val request = FakeRequest().withSession(token)

  "The create group account page" must "return Unauthorized if user has not verified identity" in {
    val res = TestCreateGroupAccount.show()(request)
    status(res) mustBe UNAUTHORIZED
  }

  it must "return Ok if user has verified identity" in {
    StubIdentityVerification.stubSuccessfulJourney("fakeId")
    val res = TestCreateGroupAccount.show()(request.withSession(token, ("journeyId", "fakeId")))
    status(res) mustBe OK
  }

  "The create group account success page" must "return Unauthorized if user has not verified identity" in {
    val res = TestCreateGroupAccount.success()(request)
    status(res) mustBe UNAUTHORIZED
  }

  it must "return Ok if user has verified identity" in {
    StubIdentityVerification.stubSuccessfulJourney("fakeId")
    val res = TestCreateGroupAccount.success()(request.withSession(token, ("journeyId", "fakeId")))
    status(res) mustBe OK
  }

  "The create group account submit" must "return Unauthorized if user has not verified identity" in {
    val res = TestCreateGroupAccount.submit()(request.withFormUrlEncodedBody("companyName" -> "someName"))
    status(res) mustBe UNAUTHORIZED
  }

  it must "redirect to the confirmation page if the user has verified their identity and submitted valid data" in {
    val validData = Map(
      keys.companyName -> "Company Ltd",
      keys.email -> "email@address.com",
      keys.confirmEmail -> "email@address.com",
      "address.line1" -> "Address line 1",
      "address.postcode" -> "AA11 1AA",
      keys.phone -> "01234567890",
      keys.isAgent -> "false"
    )
    StubIdentityVerification.stubSuccessfulJourney("fakeId")

    val group = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get.copy(externalId = "has-account", organisationId = group.id)

    StubAuthConnector.stubExternalId("has-account")
    StubAuthConnector.stubGroupId("has-group-account")
    StubIndividualAccountConnector.stubAccount(person)
    StubGroupAccountConnector.stubAccount(group)
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(group, person)))

    val res = TestCreateGroupAccount.submit()(request.withSession(token, ("journeyId", "fakeId")).withFormUrlEncodedBody(validData.toList: _*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.CreateGroupAccount.success().url)
  }

  it must "return Bad Request has verified identity and submitted invalid data" in {

    StubIdentityVerification.stubSuccessfulJourney("fakeId")

    val group = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get.copy(externalId = "has-account", organisationId = group.id)

    StubAuthConnector.stubExternalId("has-account")
    StubAuthConnector.stubGroupId("has-group-account")
    StubIndividualAccountConnector.stubAccount(person)
    StubGroupAccountConnector.stubAccount(group)
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(group, person)))

    val res = TestCreateGroupAccount.submit()(request.withSession(token, ("journeyId", "fakeId")))
    status(res) mustBe BAD_REQUEST
  }
}
