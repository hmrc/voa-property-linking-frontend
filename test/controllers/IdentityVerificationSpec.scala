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

package controllers
import auth.GGAction
import connectors.{AccountConnector, UserDetails, VPLAuthConnector}
import play.api.test.FakeRequest
import utils.{StubAccountConnector, StubAuthConnector, StubGGAction, StubUserDetails}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier

class IdentityVerificationSpec extends ControllerSpec {

  private object TestIdentityVerification extends IdentityVerification {
    override val accounts: AccountConnector = StubAccountConnector
    override val userDetails: UserDetails = StubUserDetails
    override val auth: VPLAuthConnector = StubAuthConnector
    override val ggAction: GGAction = StubGGAction
  }

  val request = FakeRequest()

  "Successfully verifying identity when the group does not have a CCA account" must "redirect to the create group account page, and not create an individual account" in {
    StubAccountConnector.reset()
    StubUserDetails.stubGroupId("groupwithoutaccount")

    val res = TestIdentityVerification.succeed()(request)
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.CreateGroupAccount.show.url)

    implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

    await(StubAccountConnector.get()) mustBe Nil
  }

  "Successfully verifying identity when the group does have a CCA account" must "redirect to the dashboard, and create the individual account" in {
    StubAccountConnector.reset()
    StubAccountConnector.stubAccount(Account("groupwithaccount", false))
    StubAuthConnector.stubInternalId("individualwithoutaccount")
    StubUserDetails.stubGroupId("groupwithaccount")

    val res = TestIdentityVerification.succeed()(request)
    status(res) mustBe SEE_OTHER
    header("location", res) mustBe Some(routes.Dashboard.home().url)

    implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

    await(StubAccountConnector.get("individualwithoutaccount")) must not be None
  }
}
