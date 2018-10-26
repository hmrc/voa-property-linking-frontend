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

import connectors.{Authenticated, DraftCases}
import models.Accounts
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Formatters, StubAuthentication, StubMessagesConnector, StubPropertyLinkConnector}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class ViewDraftCasesSpec extends VoaPropertyLinkingSpec {

  override val additionalAppConfig = Seq("featureFlags.newDashboardRedirectsEnabled" -> "false")

  "Viewing draft cases, when the user has no drafts" should "tell the user they have no draft cases" in {
    when(mockDraftCases.get(anyLong)(any[HeaderCarrier])).thenReturn(Future.successful(Nil))
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccountGen, individualGen)))

    val res = testController.viewDraftCases()(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.select("p").asScala.map(_.text) must contain ("You have no saved draft cases")
  }

  "Viewing draft cases, when the user has a saved draft" should "show the property address, assessment effective date, and draft expiry date" in {
    val draftCase = randomDraftCase
    when(mockDraftCases.get(anyLong)(any[HeaderCarrier])).thenReturn(Future.successful(Seq(draftCase)))
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccountGen, individualGen)))

    val res = testController.viewDraftCases()(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    val rows = html.select("table tbody tr").asScala
    val address = rows.head.select("td").first.text
    val effectiveDate = rows.head.select("td").get(1).text
    val expirationDate = rows.head.select("td").get(2).text

    address mustBe Formatters.capitalizedAddress(draftCase.address)
    effectiveDate mustBe Formatters.formatDate(draftCase.effectiveDate)
    expirationDate mustBe Formatters.formatDate(draftCase.expirationDate)
  }

  implicit lazy val mockDraftCases: DraftCases = mock[DraftCases]

  private lazy val testController = new ManageDrafts(
    StubAuthentication,
    StubPropertyLinkConnector,
    StubMessagesConnector
  )


}
