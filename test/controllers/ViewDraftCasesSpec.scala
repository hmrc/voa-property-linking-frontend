/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors.DraftCases
import connectors.authorisation.Authenticated
import models.Accounts
import connectors.DraftCases
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.StubPropertyLinkConnector

class ViewDraftCasesSpec extends VoaPropertyLinkingSpec {

  "Viewing draft cases" should "return redirect" in {
    val res = testController.viewDraftCases()(FakeRequest())
    status(res) mustBe SEE_OTHER
  }

  implicit lazy val mockDraftCases: DraftCases = mock[DraftCases]

  private lazy val testController = new ManageDrafts(
    mockCustomErrorHandler,
    preAuthenticatedActionBuilders(),
    StubPropertyLinkConnector
  )

}
