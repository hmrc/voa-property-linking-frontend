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

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.ListType.CURRENT
import models.{ApiAssessment, ApiAssessments, ClientDetails, ClientPropertyLink, ListType, Party, PropertyAddress, PropertyLinkingApproved}
import models.assessments.AssessmentsPageSession
import models.properties.AllowedAction
import models.properties.AllowedAction.{CHECK, VIEW_DETAILED_VALUATION}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class ValuationsControllerISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  val plSubId = "pl-submission-id"

  val paragraphOneOwnerSelector = "#owner-section > p"
  val paragraphLinkOwnerSelector = "#owner-section > span"
  val linkSectionTextSelector = "span:nth-child(3)"
  val linkSectionLinkSelector = "#explanatory-link"
  val listCaptionSelector = "#agent-section > p:nth-child(1)"
  val bulletPointOneSelector = "#reasons-list > li:nth-child(1)"
  val bulletPointTwoSelector = "#reasons-list > li:nth-child(2)"
  val contactTextSelector = "#contact-text"
  val noValuationsTextSelector = "#no-valuation-text"

  lazy val mockRepository: AssessmentsPageSession = app.injector.instanceOf[AssessmentsPageSession]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  "Valuation controller" should {

    "displays assessment page with the correct content when logged in as an IP in English" in {

      lazy val document: Document = getPage(English, false, true)

      document
        .select(paragraphOneOwnerSelector)
        .text shouldBe ("We only show valuations for when you owned or occupied the property.")
      document.select(paragraphLinkOwnerSelector).text shouldBe ("Valuations for other periods may be available.")
      document
        .select(linkSectionLinkSelector)
        .text shouldBe ("Find public versions of all valuations for this property")

    }

    "displays assessment page with the correct content when logged in as an IP in Welsh" in {

      lazy val document: Document = getPage(Welsh, false, true)

      document
        .select(paragraphOneOwnerSelector)
        .text shouldBe ("Rydym ond yn dangos prisiadau ar gyfer yr adeg yr oeddech yn berchen ar yr eiddo neu’n ei feddiannu.")
      document
        .select(paragraphLinkOwnerSelector)
        .text shouldBe ("Mae’n bosibl bod prisiadau ar gyfer cyfnodau eraill ar gael.")
      document
        .select(linkSectionLinkSelector)
        .text shouldBe ("Dewch o hyd i fersiynau cyhoeddus o’r holl brisiadau ar gyfer yr eiddo hwn")

    }

    "displays assessment page with the correct content when logged in as an agent in English" in {

      lazy val document: Document = getPage(English, false, false)

      document.title() shouldBe ("ADDRESS - Valuation Office Agency - GOV.UK")
      document.select(listCaptionSelector).text shouldBe ("We only show valuations:")
      document.select(bulletPointOneSelector).text shouldBe ("for the rating lists your client wants you to act on")
      document.select(bulletPointTwoSelector).text shouldBe ("for when your client owned or occupied the property")
      document.select(linkSectionTextSelector).text shouldBe ("Valuations for other periods may be available.")
      document
        .select(linkSectionLinkSelector)
        .text shouldBe ("Find public versions of all valuations for this property")
      document
        .select(contactTextSelector)
        .text shouldBe ("Contact your client if you need to change which lists you can act on.")

    }

    "displays assessment page with the correct content when logged in as an agent in Welsh" in {

      lazy val document: Document = getPage(Welsh, false, false)

      document.select(listCaptionSelector).text shouldBe ("Rydym ond yn dangos prisiadau ar gyfer:")
      document
        .select(bulletPointOneSelector)
        .text shouldBe ("y rhestrau ardrethu y mae’ch cleient am i chi eu gweithredu")
      document
        .select(bulletPointTwoSelector)
        .text shouldBe ("yr adeg yr oedd eich cleient yn berchen ar yr eiddo neu’n ei feddiannu")
      document
        .select(linkSectionTextSelector)
        .text shouldBe ("Mae’n bosibl bod prisiadau ar gyfer cyfnodau eraill ar gael.")
      document
        .select(linkSectionLinkSelector)
        .text shouldBe ("Dewch o hyd i fersiynau cyhoeddus o’r holl brisiadau ar gyfer yr eiddo hwn")
      document
        .select(contactTextSelector)
        .text shouldBe ("Cysylltwch â’ch cleient os oes angen i chi newid y rhestrau mae gennych ganiatâd i weithredu arnynt.")

    }
    "displays assessment page with the correct content when logged in as an agent in English when there is no valuations to display" in {

      lazy val document: Document = getPage(English, true, false)

      document.select(noValuationsTextSelector).text shouldBe ("There are no valuations available for this property.")

    }

    "displays assessment page with the correct content for client in Welsh when there is no valuations to display" in {

      lazy val document: Document = getPage(Welsh, true, false)

      document.select(noValuationsTextSelector).text shouldBe ("Nid oes prisiadau ar gael ar gyfer yr eiddo hwn.")

    }
  }

  private def getPage(language: Language, noAssessments: Boolean, isOwner: Boolean): Document = {

    def apiAssessments(): ApiAssessments =
      ApiAssessments(
        authorisationId = 1111L,
        submissionId = "submissionId",
        uarn = 1111L,
        address = "ADDRESS",
        pending = false,
        clientOrgName = None,
        capacity = Some("OWNER"),
        assessments = if (noAssessments) { Seq.empty[ApiAssessment] } else {
          Seq(
            ApiAssessment(
              authorisationId = 1111L,
              assessmentRef = 1234L,
              listYear = "2017",
              uarn = 1111L,
              effectiveDate = Some(LocalDate.of(2017, 4, 1)),
              rateableValue = Some(123L),
              address = PropertyAddress(Seq(""), "address"),
              billingAuthorityReference = "localAuthorityRef",
              billingAuthorityCode = Some("2715"),
              listType = CURRENT,
              allowedActions = List(VIEW_DETAILED_VALUATION, CHECK),
              currentFromDate = Some(LocalDate.of(2019, 2, 21)),
              currentToDate = Some(LocalDate.of(2023, 2, 21))
            ),
            ApiAssessment(
              authorisationId = 1234L,
              assessmentRef = 1236L,
              listYear = "CURRENT",
              uarn = 12345L,
              effectiveDate = Some(LocalDate.of(2017, 4, 1)),
              rateableValue = Some(1234L),
              address = PropertyAddress(Seq(""), "address"),
              billingAuthorityReference = "localAuthorityRef",
              billingAuthorityCode = Some("2715"),
              listType = ListType.CURRENT,
              allowedActions = List(AllowedAction.ENQUIRY),
              currentFromDate = Some(LocalDate.of(2017, 4, 1).plusMonths(2L)),
              currentToDate = None
            )
          )
        },
        agents = Seq.empty[Party]
      )

    val testData = ClientPropertyLink(
      authorisationId = 123345505L,
      authorisedPartyId = 45624L,
      status = PropertyLinkingApproved,
      startDate = LocalDate.of(2023, 8, 7),
      endDate = Some(LocalDate.of(2023, 12, 31)),
      submissionId = "17282920",
      capacity = "Owner",
      uarn = 789349L,
      address = "123 Main Street",
      localAuthorityRef = "LA123",
      client = ClientDetails(12345L, "Test Organisation name")
    )

    val owner = if (isOwner) "owner" else "agent"
    val assessments = if (isOwner) "assessments" else "assessments?owner=false"

    if (!isOwner) {

      stubFor {
        get(s"/property-linking/agent/property-links/$plSubId?projection=clientsPropertyLink")
          .willReturn {
            aResponse.withStatus(OK).withBody(Json.toJson(testData).toString())
          }
      }

    }

    stubFor {
      get(s"/property-linking/dashboard/$owner/assessments/$plSubId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(apiAssessments()).toString())
        }
    }

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
      ws.url(s"http://localhost:$port/business-rates-property-linking/property-link/$plSubId/$assessments")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
