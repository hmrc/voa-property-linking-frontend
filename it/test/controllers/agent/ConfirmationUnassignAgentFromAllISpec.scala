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

package controllers.agent

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.AgentSummary
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class ConfirmationUnassignAgentFromAllISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val headingTag = "h1"
  val paragraphOneLocator = "#main-content > div > div > p:nth-child(2)"
  val paragraphTwoLocator = "#main-content > div > div > p:nth-child(3)"
  val whatHappensNextLocator = "#main-content > div > div > h2"
  val whatHappensNextParaOneLocator = "#main-content > div > div > p:nth-child(5)"
  val whatHappensNextParaTwoLocator = "#main-content > div > div > p:nth-child(6)"
  val removeAgentLinkLocator = "remove-agent-from-account"
  val goHomeLinkLocator = "#main-content > div > div > p:nth-child(7) > a"

  val titleMultiplePropertiesText =
    "Test Agent has been unassigned from all your properties - Valuation Office Agency - GOV.UK"
  val titleOnePropertyText = "Test Agent has been unassigned from your property - Valuation Office Agency - GOV.UK"
  val headingMultiplePropertiesText = "Test Agent has been unassigned from all your properties"
  val headingOnePropertyText = "Test Agent has been unassigned from your property"
  val paragraphOneText = "The agent can no longer act for you on any of your properties."
  val paragraphTwoText =
    "The agent has not been removed from your account. They can still act for you if they add other properties to your account."
  val whatHappensNextText = "What happens next"
  val whatHappensNextParaOneText = "You can remove this agent from your account."
  val whatHappensRemoveAgentLinkText = "remove this agent from your account"
  val whatHappensNextParaTwoText = "You can reassign an agent to a property if you want them to act for you again."
  val goHomeLinkText = "Go to your account home"

  val titleMultiplePropertiesTextWelsh =
    "Mae Test Agent wedi’i ddadneilltuo o’ch holl eiddo - Valuation Office Agency - GOV.UK"
  val titleOnePropertyTextWelsh = "Mae Test Agent wedi’i ddad-neilltuo o’ch eiddo - Valuation Office Agency - GOV.UK"
  val headingMultiplePropertiesTextWelsh = "Mae Test Agent wedi’i ddadneilltuo o’ch holl eiddo"
  val headingOnePropertyTextWelsh = "Mae Test Agent wedi’i ddad-neilltuo o’ch eiddo"
  val paragraphOneTextWelsh = "Ni all yr asiant weithredu ar eich rhan mwyach ar unrhyw un o’ch eiddo."
  val paragraphTwoTextWelsh =
    "Nid yw’r asiant wedi’i dynnu o’ch cyfrif. Gallant barhau i weithredu ar eich rhan os ydynt yn ychwanegu eiddo eraill at eich cyfrif."
  val whatHappensNextTextWelsh = "Yr hyn sy’n digwydd nesaf"
  val whatHappensNextParaOneTextWelsh = "Gallwch dynnu’r asiant hwn o’ch cyfrif."
  val whatHappensRemoveAgentLinkTextWelsh = "dynnu’r asiant hwn o’ch cyfrif"
  val whatHappensNextParaTwoTextWelsh = "Gallwch ailbennu asiant i eiddo os ydych am iddynt weithredu ar eich rhan eto."
  val goHomeLinkTextWelsh = "Ewch i hafan eich cyfrif"

  val removeAgentLinkHref = "/business-rates-property-linking/my-organisation/manage-agent/remove/from-organisation"
  val goHomeLinkHref = "/business-rates-dashboard/home"

  "confirmationUnassignAgentFromAll displays the correct content in English when a user has 1 property" which {
    lazy val document: Document = getUnassignFromAllConfirmationPage(English, numOfProperties = 1)

    s"has a title of $titleOnePropertyText" in {
      document.title() shouldBe titleOnePropertyText
    }

    s"has a heading of $headingOnePropertyText" in {
      document.getElementsByTag(headingTag).text shouldBe headingOnePropertyText
    }

    s"has the first paragraph of $paragraphOneText" in {
      document.select(paragraphOneLocator).text shouldBe paragraphOneText
    }

    s"has the second paragraph of $paragraphTwoText" in {
      document.select(paragraphTwoLocator).text shouldBe paragraphTwoText
    }

    s"has a subheading of $whatHappensNextText" in {
      document.select(whatHappensNextLocator).text shouldBe whatHappensNextText
    }

    s"has the first paragraph in the $whatHappensNextText section of $whatHappensNextParaOneText" in {
      document.select(whatHappensNextParaOneLocator).text shouldBe whatHappensNextParaOneText
      document.getElementById(removeAgentLinkLocator).text shouldBe whatHappensRemoveAgentLinkText
      document.getElementById(removeAgentLinkLocator).attr("href") shouldBe removeAgentLinkHref
    }

    s"has the second paragraph in the $whatHappensNextText section of $whatHappensNextParaTwoText" in {
      document.select(whatHappensNextParaTwoLocator).text shouldBe whatHappensNextParaTwoText
    }

    s"has a $goHomeLinkText link" in {
      document.select(goHomeLinkLocator).text shouldBe goHomeLinkText
      document.select(goHomeLinkLocator).attr("href") shouldBe goHomeLinkHref
    }
  }

  "confirmationUnassignAgentFromAll displays the correct content in Welsh when a user has 1 property" which {
    lazy val document: Document = getUnassignFromAllConfirmationPage(Welsh, numOfProperties = 1)

    s"has a title of $titleOnePropertyText in Welsh" in {
      document.title() shouldBe titleOnePropertyTextWelsh
    }

    s"has a heading of $headingOnePropertyText in Welsh" in {
      document.getElementsByTag(headingTag).text shouldBe headingOnePropertyTextWelsh
    }

    s"has the first paragraph of $paragraphOneText in Welsh" in {
      document.select(paragraphOneLocator).text shouldBe paragraphOneTextWelsh
    }

    s"has the second paragraph of $paragraphTwoText in Welsh" in {
      document.select(paragraphTwoLocator).text shouldBe paragraphTwoTextWelsh
    }

    s"has a subheading of $whatHappensNextText in Welsh" in {
      document.select(whatHappensNextLocator).text shouldBe whatHappensNextTextWelsh
    }

    s"has the first paragraph in the $whatHappensNextText section of $whatHappensNextParaOneText in Welsh" in {
      document.select(whatHappensNextParaOneLocator).text shouldBe whatHappensNextParaOneTextWelsh
      document.getElementById(removeAgentLinkLocator).text shouldBe whatHappensRemoveAgentLinkTextWelsh
      document.getElementById(removeAgentLinkLocator).attr("href") shouldBe removeAgentLinkHref
    }

    s"has the second paragraph in the $whatHappensNextText section of $whatHappensNextParaTwoText in Welsh" in {
      document.select(whatHappensNextParaTwoLocator).text shouldBe whatHappensNextParaTwoTextWelsh
    }

    s"has a $goHomeLinkText link in Welsh" in {
      document.select(goHomeLinkLocator).text shouldBe goHomeLinkTextWelsh
      document.select(goHomeLinkLocator).attr("href") shouldBe goHomeLinkHref
    }
  }

  "confirmationUnassignAgentFromAll displays the correct content in English when a user has more than 1 property" which {
    lazy val document: Document = getUnassignFromAllConfirmationPage(English, numOfProperties = 10)

    s"has a title of $titleMultiplePropertiesText" in {
      document.title() shouldBe titleMultiplePropertiesText
    }

    s"has a heading of $headingMultiplePropertiesText" in {
      document.getElementsByTag(headingTag).text shouldBe headingMultiplePropertiesText
    }

    s"has the first paragraph of $paragraphOneText" in {
      document.select(paragraphOneLocator).text shouldBe paragraphOneText
    }

    s"has the second paragraph of $paragraphTwoText" in {
      document.select(paragraphTwoLocator).text shouldBe paragraphTwoText
    }

    s"has a subheading of $whatHappensNextText" in {
      document.select(whatHappensNextLocator).text shouldBe whatHappensNextText
    }

    s"has the first paragraph in the $whatHappensNextText section of $whatHappensNextParaOneText" in {
      document.select(whatHappensNextParaOneLocator).text shouldBe whatHappensNextParaOneText
      document.getElementById(removeAgentLinkLocator).text shouldBe whatHappensRemoveAgentLinkText
      document.getElementById(removeAgentLinkLocator).attr("href") shouldBe removeAgentLinkHref
    }

    s"has the second paragraph in the $whatHappensNextText section of $whatHappensNextParaTwoText" in {
      document.select(whatHappensNextParaTwoLocator).text shouldBe whatHappensNextParaTwoText
    }

    s"has a $goHomeLinkText link" in {
      document.select(goHomeLinkLocator).text shouldBe goHomeLinkText
      document.select(goHomeLinkLocator).attr("href") shouldBe goHomeLinkHref
    }
  }

  "confirmationUnassignAgentFromAll displays the correct content in Welsh when a user has more than 1 property" which {
    lazy val document: Document = getUnassignFromAllConfirmationPage(Welsh, numOfProperties = 10)

    s"has a title of $titleMultiplePropertiesText in Welsh" in {
      document.title() shouldBe titleMultiplePropertiesTextWelsh
    }

    s"has a heading of $headingMultiplePropertiesText in Welsh" in {
      document.getElementsByTag(headingTag).text shouldBe headingMultiplePropertiesTextWelsh
    }

    s"has the first paragraph of $paragraphOneText in Welsh" in {
      document.select(paragraphOneLocator).text shouldBe paragraphOneTextWelsh
    }

    s"has the second paragraph of $paragraphTwoText in Welsh" in {
      document.select(paragraphTwoLocator).text shouldBe paragraphTwoTextWelsh
    }

    s"has a subheading of $whatHappensNextText in Welsh" in {
      document.select(whatHappensNextLocator).text shouldBe whatHappensNextTextWelsh
    }

    s"has the first paragraph in the $whatHappensNextText section of $whatHappensNextParaOneText in Welsh" in {
      document.select(whatHappensNextParaOneLocator).text shouldBe whatHappensNextParaOneTextWelsh
      document.getElementById(removeAgentLinkLocator).text shouldBe whatHappensRemoveAgentLinkTextWelsh
      document.getElementById(removeAgentLinkLocator).attr("href") shouldBe removeAgentLinkHref
    }

    s"has the second paragraph in the $whatHappensNextText section of $whatHappensNextParaTwoText in Welsh" in {
      document.select(whatHappensNextParaTwoLocator).text shouldBe whatHappensNextParaTwoTextWelsh
    }

    s"has a $goHomeLinkText link in Welsh" in {
      document.select(goHomeLinkLocator).text shouldBe goHomeLinkTextWelsh
      document.select(goHomeLinkLocator).attr("href") shouldBe goHomeLinkHref
    }
  }

  private def getUnassignFromAllConfirmationPage(language: Language, numOfProperties: Int) = {
    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(List("2017", "2023")),
          name = "Test Agent",
          organisationId = 1L,
          representativeCode = 1L,
          appointedDate = LocalDate.now(),
          propertyCount = numOfProperties
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

    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentList).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/property-links/count")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(numOfProperties).toString())
        }
    }

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent/unassign/from-all-properties/confirmation"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId")
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

}
