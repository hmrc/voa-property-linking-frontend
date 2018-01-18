/*
 * Copyright 2018 HM Revenue & Customs
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

import com.builtamont.play.pdf.PdfGenerator
import config.ApplicationConfig
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import models._
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import utils._

class ManageOwnerAgentsSpec extends ControllerSpec {
  override val additionalAppConfig = Seq("featureFlags.managedAgentsEnabled" -> "true", "featureFlags.enrolment" -> "false")

  implicit val request = FakeRequest()

  object TestDashboardController extends Dashboard(
    app.injector.instanceOf[ApplicationConfig],
    mock[DraftCases],
    mock[PropertyLinkConnector],
    new StubMessagesConnector(app.injector.instanceOf[ApplicationConfig]),
    StubAgentConnector,
    StubAuthentication,
    mock[PdfGenerator]
  )

  "Manage Owner Agents page" must "return Ok" in {

    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))

    val res = TestDashboardController.manageAgents()(FakeRequest())
    status(res) mustBe OK
  }

}
