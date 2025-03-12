/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.agentAppointment

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.{AgentSelected, AgentSummary, SearchedAgent, SelectedAgent}
import models.searchApi.OwnerAuthResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.{AppointAgentSessionRepository, ManageAgentSessionRepository}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class ListYearsFeatureSwitchISpec extends ISpecBase {

  override lazy val extraConfig: Map[String, Any] = Map(
    "feature-switch.agentListYears.enabled" -> "false"
  )

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  "ChooseRatingListController show method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in
      getRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/choose")
  }

  "ChooseRatingListController post method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in
      postRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/choose")
  }

  "WhichRatingListController show method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in
      getRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm")
  }

  "WhichRatingListController post method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in
      postRequest(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm"
      )
  }

  "AreYouSureSingleController show method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in
      getRequest(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=2017"
      )
  }

  "AreYouSureSingleController post method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in
      postRequest(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=2017"
      )
  }

  "AreYouSureMultipleController show method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in
      getRequest(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure-multiple"
      )
  }

  "AreYouSureMultipleController post method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in
      postRequest(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure-multiple"
      )
  }

  "RatingListConfirmedController show method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in
      getRequest(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"
      )
  }

  s"AddAgentController agentSelected method should redirect to the check your answers page if organisation has no authorisations and the user chooses yes" when {
    lazy val res = postIsCorrectAgentPage(properties = 0)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("Location") shouldBe Some(
        "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"
      )
    }
  }

  s"AddAgentController agentSelected method should redirect to the agentToManageOneProperty page if organisation has only one authorisation and the user chooses yes" when {
    lazy val res = postIsCorrectAgentPage(properties = 1)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("Location") shouldBe Some(
        "/business-rates-property-linking/my-organisation/appoint-new-agent/one-property"
      )
    }
  }

  s"AddAgentController agentSelected method should redirect to the agentToManageMultipleProperties page if organisation has multiple authorisations and the user chooses yes" when {
    lazy val res = postIsCorrectAgentPage(properties = 2)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("Location") shouldBe Some(
        "/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties"
      )
    }
  }

  private def getRequest(url: String): Document = {

    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(List("2017")),
          name = "Test Agent",
          organisationId = 100L,
          representativeCode = 100L,
          appointedDate = LocalDate.now(),
          propertyCount = 1
        )
      )
    )

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

    val res = await(
      ws.url(url)
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe NOT_FOUND
    Jsoup.parse(res.body)
  }

  private def postRequest(url: String): Document = {

    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(List("2017")),
          name = "Test Agent",
          organisationId = 100L,
          representativeCode = 100L,
          appointedDate = LocalDate.now(),
          propertyCount = 1
        )
      )
    )

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

    val res = await(
      ws.url(url)
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = "")
    )

    res.status shouldBe NOT_FOUND
    Jsoup.parse(res.body)
  }

  private def postIsCorrectAgentPage(properties: Int) = {

    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
      app.injector.instanceOf[AppointAgentSessionRepository]

    val searchedAgentData: SearchedAgent = SearchedAgent.apply(1001, "Agent", "Address", AgentSelected, None)

    val selectedAgentData = SelectedAgent.apply(searchedAgentData, isTheCorrectAgent = true, None, None, Seq.empty)

    await(mockAppointAgentSessionRepository.saveOrUpdate(selectedAgentData))

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

    val authData: OwnerAuthResult = properties match {
      case 0 => testOwnerAuthResultNoProperties
      case 1 => ownerAuthResultWithOneAuthorisation
      case 2 => testOwnerAuthResultMultipleProperty
      case _ => testOwnerAuthResult
    }

    stubFor {
      get(
        "/property-linking/my-organisation/agents/1001/available-property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
      ).willReturn {
        aResponse.withStatus(OK).withBody(Json.toJson(authData).toString())
      }
    }

    val requestBody = Json.obj(
      "isThisYourAgent" -> "true"
    )

    await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent?backLinkUrl=%2Fbusiness-rates-dashboard%2Fhome"
      ).withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )
  }

}
