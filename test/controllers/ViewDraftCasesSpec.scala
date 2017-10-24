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

import config.ApplicationConfig
import connectors.{Authenticated, DraftCases}
import models.Accounts
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{Formatters, StubAuthentication, StubMessagesConnector, StubPropertyLinkConnector}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class ViewDraftCasesSpec extends ControllerSpec {

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

  it should "show a link to continue the draft, and a link to view the detailed valuation" in {
    val draftCase = randomDraftCase
    when(mockDraftCases.get(anyLong)(any[HeaderCarrier])).thenReturn(Future.successful(Seq(draftCase)))
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccountGen, individualGen)))

    val res = testController.viewDraftCases()(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    val links = html.select("table tbody tr").asScala.head.select("td").get(3)
    val resumeLink = links.select("li a").first.attr("href")
    val viewValuationLink = links.select("li a").get(1).attr("href")

    resumeLink mustBe draftCase.url
    viewValuationLink mustBe routes.Assessments.viewDetailedAssessment(draftCase.propertyLinkId, draftCase.assessmentRef, draftCase.baRef).url
  }

  "Viewing draft cases, when the user has multiple saved drafts" should "show the address, expiry date, and links, for each case" in {
    val draftCases = Seq(randomDraftCase, randomDraftCase, randomDraftCase)
    when(mockDraftCases.get(anyLong)(any[HeaderCarrier])).thenReturn(Future.successful(draftCases))
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccountGen, individualGen)))

    val res = testController.viewDraftCases()(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    val rows = html.select("table tbody tr").asScala
    rows must have size draftCases.size

    draftCases.zipWithIndex.map { case (draft, index) =>
      val columns = rows(index).select("td")
      
      val address = columns.first.text
      address mustBe Formatters.capitalizedAddress(draft.address)

      val effectiveDate = columns.get(1).text
      effectiveDate mustBe Formatters.formatDate(draft.effectiveDate)

      val expirationDate = columns.get(2).text
      expirationDate mustBe Formatters.formatDate(draft.expirationDate)

      val links = columns.get(3).select("li a")
      links.first.attr("href") mustBe draft.url
      links.get(1).attr("href") mustBe routes.Assessments.viewDetailedAssessment(draft.propertyLinkId, draft.assessmentRef, draft.baRef).url
    }
  }

  private lazy val testController = new Dashboard(
    app.injector.instanceOf[ApplicationConfig],
    mockDraftCases,
    StubPropertyLinkConnector,
    StubMessagesConnector,
    StubAuthentication
  )

  private lazy val mockDraftCases = mock[DraftCases]
}
