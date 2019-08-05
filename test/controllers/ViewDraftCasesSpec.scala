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

import connectors.{Authenticated, DraftCases}
import models.Accounts
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Formatters, StubAuthentication, StubPropertyLinkConnector}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class ViewDraftCasesSpec extends VoaPropertyLinkingSpec {

  "Viewing draft cases" should "return redirect" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccountGen, individualGen)))

    val res = testController.viewDraftCases()(FakeRequest())
    status(res) mustBe SEE_OTHER
  }

  implicit lazy val mockDraftCases: DraftCases = mock[DraftCases]

  private lazy val testController = new ManageDrafts(
    StubAuthentication,
    StubPropertyLinkConnector
  )


}
