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

package controllers.manageAgentReval2026Enabled.manageAgent

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import models.propertyrepresentation.{AgentAppointmentChangesResponse, AgentList, AgentSummary, AppointmentScope}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class ManageAgentISpec extends ISpecBase with HtmlComponentHelpers {

  override lazy val extraConfig: Map[String, String] =
    Map("feature-switch.agentJourney2026Enabled" -> "true")

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "What do you want to do with your agent Test Agent? - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val continueButtonText = "Continue"
  val captionText = "Manage agent"

  def headerText(name: String) = s"What do you want to do with your agent $name?"

  val radioAssignYourText = "Assign to your property"
  val radioUnAssignYourText = "Unassign from my property"
  val radioAssignAllText = "Assign to all properties"
  val radioAssignASomeText = "Assign to one or more properties"
  val radioUnassignedAllText = "Unassign from all properties"
  val radioUnassignedASomeText = "Unassign from one or more properties"
  val radioChangeText = "Change which rating list they can act on for you"
  val radioRemoveText = "Remove from your account"
  val errorSpan = "Error: "
  val noOptionChoiceText = "Select what you want to do with this agent"

  val titleTextWelsh = "Beth hoffech ei wneud â’ch asiant Test Agent? - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val continueButtonTextWelsh = "Parhau"
  val captionTextWelsh = "Rheoli asiant"

  def headerTextWelsh(name: String) = s"Beth ydych chi eisiau ei wneud i’r asiant  $name?"

  val radioAssignYourTextWelsh = "Neilltuo i’ch eiddo"
  val radioAssignAllTextWelsh = "Neilltuo i bob eiddo"
  val radioAssignASomeTextWelsh = "Neilltuo un eiddo neu fwy iddo"
  val radioUnassignedAllTextWelsh = "Dad-neilltuo o’ch holl eiddo"
  val radioUnassignedASomeTextWelsh = "Tynnu o un eiddo neu fwy"
  val radioChangeTextWelsh = "Newid pa restr ardrethu y gall yr asiant hwn weithredu arni ar eich rhan"
  val radioRemoveTextWelsh = "Dileu o’ch cyfrif"
  val radioUnAssignYourTextWelsh = "Tynnu o’m heiddo"
  val errorSpanWelsh = "Gwall: "
  val noOptionChoiceTextWelsh = "Dewiswch beth rydych chi am ei wneud gyda’r asiant hwn"

  val backLinkSelector = "#back-link"
  val captionSelector = ".govuk-caption-l"
  val headerSelector = "h1.govuk-heading-l"
  val continueButtonSelector = "button.govuk-button"
  val firstRadioLabelSelector = ".govuk-radios__item:nth-child(1) > .govuk-label"
  val secondRadioLabelSelector = ".govuk-radios__item:nth-child(2) > .govuk-label"
  val thirdRadioLabelSelector = ".govuk-radios__item:nth-child(3) > .govuk-label"
  val fourthRadioLabelSelector = ".govuk-radios__item:nth-child(4) > .govuk-label"
  val fifthRadioLabelSelector = ".govuk-radios__item:nth-child(5) > .govuk-label"
  val errorAtSummarySelector = "#main-content > div > div > div > div > div > ul > li > a"
  val noOptionChoiceAtLabelSelector = "#manageAgentOption-error"

  val backLinkHref = s"/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=100"
  val radioAssignAllHref = "business-rates-property-linking/my-organisation/manage-agent/assign/to-all-properties"
  val radioAssignASomeHref =
    "http://localhost:9523/business-rates-property-linking/my-organisation/appoint/properties?page=1&pageSize=15&agentCode=1001&agentAppointed=BOTH&backLink=%2Fbusiness-rates-property-linking%2Fmy-organisation%2Fmanage-agent"
  val radioUnassignedAllHref =
    "/business-rates-property-linking/my-organisation/manage-agent/unassign/from-all-properties"
  val radioUnassignedSomeHref =
    "/business-rates-property-linking/my-organisation/revoke/properties?page=1&pageSize=15&agentCode=36"
  val radioChangeHref = "Change which rating list they can act on for you"
  val radioRemoveHref = "/business-rates-property-linking/my-organisation/manage-agent/remove/from-organisation"

  "ManageAgentController showManageAgent method" should {
    "display 'Manage agent' screen with the correct text, correct radio labels and the language is set to English for Agent, who has got at least 1 property assigned and Client got more properties" which {

      lazy val document = getPage(English, 1)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to Manage agent properties screen" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      "displays a correct header with agent name included" in {
        document.select(headerSelector).text() shouldBe headerText(name = "Test Agent")
      }

      "displays a correct caption above the header" in {
        document.select(captionSelector).text shouldBe captionText
      }

      "displays a correct text in continue button" in {
        document.select(continueButtonSelector).text shouldBe continueButtonText
      }

      "displays a correct text in radios, which are in the correct order" in {
        document.select(firstRadioLabelSelector).text shouldBe radioAssignAllText
        document.select(secondRadioLabelSelector).text shouldBe radioAssignASomeText
        document.select(thirdRadioLabelSelector).text shouldBe radioUnassignedAllText
        document.select(fourthRadioLabelSelector).text shouldBe radioUnassignedASomeText
        document.select(fifthRadioLabelSelector).text shouldBe radioChangeText
      }
    }

    "display 'Manage agent' screen with the correct radio labels and the language is set to English for Agent, who has no assigned properties" which {

      lazy val document = getPage(English, 0)

      "displays a correct text in radios, which are in the correct order" in {
        document.select(firstRadioLabelSelector).text shouldBe radioAssignAllText
        document.select(secondRadioLabelSelector).text shouldBe radioAssignASomeText
        document.select(thirdRadioLabelSelector).text shouldBe radioChangeText
        document.select(fourthRadioLabelSelector).text shouldBe radioRemoveText
      }
    }

    "display 'Manage agent' screen with the correct radio labels and the language is set to English for Agent, who is assigned to all client's properties" which {

      lazy val document = getPage(English, 10)

      "displays a correct text in radios, which are in the correct order" in {
        document.select(firstRadioLabelSelector).text shouldBe radioUnassignedAllText
        document.select(secondRadioLabelSelector).text shouldBe radioUnassignedASomeText
        document.select(thirdRadioLabelSelector).text shouldBe radioChangeText
      }
    }

    "display 'Manage agent' screen with the correct radio labels and the language is set to English for Agent, who has no assigned properties, but Client got exactly 1 property " which {

      lazy val document = getPageWhenPropertyCountIs1(English, 0, testAgentListFor2023)

      "displays a correct text in radios, which are in the correct order" in {
        document.select(firstRadioLabelSelector).text shouldBe radioAssignYourText
        document.select(secondRadioLabelSelector).text shouldBe radioChangeText
        document.select(thirdRadioLabelSelector).text shouldBe radioRemoveText
      }
    }

    "display 'Manage agent' screen with the correct radio labels and the language is set to English for Agent, who has 1 assigned property and Client got exactly 1  property " which {

      lazy val document = getPageWhenPropertyCountIs1(English, 1, testAgentListFor2017)

      "displays a correct text in radios, which are in the correct order" in {
        document.select(firstRadioLabelSelector).text shouldBe radioUnAssignYourText
        document.select(secondRadioLabelSelector).text shouldBe radioChangeText
      }
    }
    "display 'Manage agent' screen with the correct text, correct radio labels and the language is set to Welsh for Agent, who has got at least 1 property assigned" which {

      lazy val document = getPage(Welsh, 1)

      s"has a title of $titleTextWelsh" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to Manage agent properties screen" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      "displays a correct caption above the header" in {
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      "displays a correct text in continue button" in {
        document.select(continueButtonSelector).text shouldBe continueButtonTextWelsh
      }

      "displays a correct text in radios, which are in the correct order" in {
        document.select(firstRadioLabelSelector).text shouldBe radioAssignAllTextWelsh
        document.select(secondRadioLabelSelector).text shouldBe radioAssignASomeTextWelsh
        document.select(thirdRadioLabelSelector).text shouldBe radioUnassignedAllTextWelsh
        document.select(fourthRadioLabelSelector).text shouldBe radioUnassignedASomeTextWelsh
        document.select(fifthRadioLabelSelector).text shouldBe radioChangeTextWelsh
      }
    }

    "display 'Manage agent' screen with the correct radio labels and the language is set to Welsh for Agent, who has no assigned properties" which {

      lazy val document = getPage(Welsh, 0)

      "displays a correct text in the first radio label" in {
        document.select(firstRadioLabelSelector).text shouldBe radioAssignAllTextWelsh
        document.select(secondRadioLabelSelector).text shouldBe radioAssignASomeTextWelsh
        document.select(thirdRadioLabelSelector).text shouldBe radioChangeTextWelsh
        document.select(fourthRadioLabelSelector).text shouldBe radioRemoveTextWelsh
      }
    }

    "display 'Manage agent' screen with the correct radio labels and the language is set to Welsh for Agent, who has no assigned properties, but Client got exactly 1 property " which {

      lazy val document = getPageWhenPropertyCountIs1(Welsh, 0, testAgentListFor2023)

      "displays a correct text in radios, which are in the correct order" in {
        document.select(firstRadioLabelSelector).text shouldBe radioAssignYourTextWelsh
        document.select(secondRadioLabelSelector).text shouldBe radioChangeTextWelsh
        document.select(thirdRadioLabelSelector).text shouldBe radioRemoveTextWelsh
      }
    }

    "display 'Manage agent' screen with the correct radio labels and the language is set to Welsh for Agent, who is assigned to all client's properties" which {

      lazy val document = getPage(Welsh, 10)

      "displays a correct text in radios, which are in the correct order" in {
        document.select(firstRadioLabelSelector).text shouldBe radioUnassignedAllTextWelsh
        document.select(secondRadioLabelSelector).text shouldBe radioUnassignedASomeTextWelsh
        document.select(thirdRadioLabelSelector).text shouldBe radioChangeTextWelsh
      }
    }

    "display 'Manage agent' screen with the correct radio labels and the language is set to Welsh for Agent, who has 1 assigned property and Client got exactly 1  property " which {

      lazy val document = getPageWhenPropertyCountIs1(Welsh, 1, testAgentListFor2017)

      "displays a correct text in radios, which are in the correct order" in {
        document.select(firstRadioLabelSelector).text shouldBe radioUnAssignYourTextWelsh
        document.select(secondRadioLabelSelector).text shouldBe radioChangeTextWelsh
      }
    }
  }

  "agent assignment option not selected in English" should {
    s"returns an error" which {

      lazy val document = postPageWithNoOption(English)

      s"has an error in the error summary of $noOptionChoiceText in English" in {
        document.select(errorAtSummarySelector).text shouldBe noOptionChoiceText
        document.select(errorAtSummarySelector).attr("href") shouldBe "#manageAgentOption"
      }

      s"has an error above the label of $noOptionChoiceText in English" in {
        document.select(noOptionChoiceAtLabelSelector).text shouldBe errorSpan + noOptionChoiceText
      }

    }
  }

  "agent assignment option not selected in Welsh" should {
    s"returns an error" which {

      lazy val document = postPageWithNoOption(Welsh)

      s"has an error in the error summary of $noOptionChoiceText in Welsh" in {
        document.select(errorAtSummarySelector).text shouldBe noOptionChoiceTextWelsh
        document.select(errorAtSummarySelector).attr("href") shouldBe "#manageAgentOption"
      }

      s"has an error above the label of $noOptionChoiceText in Welsh" in {
        document.select(noOptionChoiceAtLabelSelector).text shouldBe errorSpanWelsh + noOptionChoiceTextWelsh
      }

    }
  }

  "assignAgentToAll" should {
    "return 303 SEE OTHER with valid submission & assert that 2017 listYears are submitted to backend when agent summary returns 2017" in new AssignAllSetup {

      // Return agent in summary list that has only 2017 listYears assigned
      stubFor {
        get("/property-linking/owner/agents")
          .willReturn {
            aResponse.withStatus(OK).withBody(Json.toJson(testAgentListFor2017).toString())
          }
      }

      val requestBody = Json.obj(
        "agentCode" -> agentCode,
        "scope"     -> s"${AppointmentScope.ALL_PROPERTIES}"
      )

      val res = await(
        ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent/assign/$agentCode/$agentName/to-all-properties"
        ).withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      val expectedJsonBody = Json.parse("""{
                                          |   "agentRepresentativeCode":1001,
                                          |   "action":"APPOINT",
                                          |   "scope":"ALL_PROPERTIES",
                                          |   "listYears":[
                                          |      "2017"
                                          |   ]
                                          |}""".stripMargin)

      // Check that the listYears returned from agent summary list is sent to backend
      verify(
        1,
        postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
          .withRequestBody(equalToJson(expectedJsonBody.toString()))
      )

      res.status shouldBe SEE_OTHER

    }

    "return 303 SEE OTHER assert that 2017&2023 listYears are submitted to backend when agent summary returns none for listYears" in new AssignAllSetup {

      // Return agent in summary list that has no listYears assigned
      stubFor {
        get("/property-linking/owner/agents")
          .willReturn {
            aResponse.withStatus(OK).withBody(Json.toJson(testAgentNoListYears).toString())
          }
      }

      val requestBody = Json.obj(
        "agentCode" -> agentCode,
        "scope"     -> s"${AppointmentScope.ALL_PROPERTIES}"
      )

      val res = await(
        ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent/assign/$agentCode/$agentName/to-all-properties"
        ).withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      val jsonRequest = Json.parse("""{
                                     |   "agentRepresentativeCode":1001,
                                     |   "action":"APPOINT",
                                     |   "scope":"ALL_PROPERTIES",
                                     |   "listYears":[
                                     |      "2017", "2023"
                                     |   ]
                                     |}""".stripMargin)

      // Check that the listYears returned from agent summary list is sent to backend
      verify(
        1,
        postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
          .withRequestBody(equalToJson(jsonRequest.toString()))
      )

      res.status shouldBe SEE_OTHER
    }
  }

  "ManageAgentController post method" should {
    "return correct status & redirect location" which {
      lazy val result = postPage(Welsh, answer = "changeRatingList")

      s"should have the correct $SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
      }

      s"should have the redirect location" in {
        result
          .headers("Location")
          .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm-reval"
      }
    }
  }

  class AssignAllSetup {
    val agentCode = 1001
    val agentName = "Test Agent"

    stubFor {
      post("/property-linking/my-organisation/agent/submit-appointment-changes")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(AgentAppointmentChangesResponse("some-id")).toString())
        }
    }

    stubFor {
      get(
        "/property-linking/owner/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=100&requestTotalRowCount=false"
      ).willReturn {
        aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult).toString())
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
  }

  private def getPage(language: Language, propertyCount: Int): Document = {
    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(List("2017")),
          name = "Test Agent",
          organisationId = 100L,
          representativeCode = 100L,
          appointedDate = LocalDate.now(),
          propertyCount = propertyCount
        )
      )
    )

    stubFor {
      get(
        "/property-linking/owner/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=100&requestTotalRowCount=false"
      ).willReturn {
        aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult).toString())
      }
    }

    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentList).toString())
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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def postPageWithNoOption(language: Language): Document = {
    val agentCode = 1001

    stubFor {
      post("/property-linking/my-organisation/agent/submit-appointment-changes")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(AgentAppointmentChangesResponse("some-id")).toString())
        }
    }

    stubFor {
      get(
        "/property-linking/owner/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=100&requestTotalRowCount=false"
      ).willReturn {
        aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult).toString())
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

    // Return agent in summary list that has only 2017 listYears assigned
    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentListFor2017).toString())
        }
    }

    val requestBody = Json.obj(
      "agentCode"         -> agentCode,
      "manageAgentOption" -> None
    )

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent/$agentCode"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )

    res.status shouldBe BAD_REQUEST
    Jsoup.parse(res.body)
  }

  private def postPage(language: Language, answer: String) = {
    val agentCode = 1001

    stubFor {
      post("/property-linking/my-organisation/agent/submit-appointment-changes")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(AgentAppointmentChangesResponse("some-id")).toString())
        }
    }

    stubFor {
      get(
        "/property-linking/owner/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=100&requestTotalRowCount=false"
      ).willReturn {
        aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult).toString())
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

    // Return agent in summary list that has only 2017 listYears assigned
    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentListFor2017).toString())
        }
    }

    val requestBody = Json.obj(
      "agentName"         -> "Test Agent",
      "manageAgentOption" -> answer
    )

    await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent/$agentCode"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )
  }

  private def getPageWhenPropertyCountIs1(language: Language, propertyCount: Int, agentList: AgentList): Document = {
    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(List("2017")),
          name = "Test Agent",
          organisationId = 100L,
          representativeCode = 100L,
          appointedDate = LocalDate.now(),
          propertyCount = propertyCount
        )
      )
    )

    stubFor {
      get(
        "/property-linking/owner/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=100&requestTotalRowCount=false"
      ).willReturn {
        aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult1).toString())
      }
    }

    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(agentList).toString())
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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
