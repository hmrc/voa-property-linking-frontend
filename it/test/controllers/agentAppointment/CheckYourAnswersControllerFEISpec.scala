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
import com.github.tomakehurst.wiremock.client.WireMock._
import models.AgentAppointBulkAction
import models.propertyrepresentation.AppointmentScope._
import models.propertyrepresentation._
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.{AppointAgentPropertiesSessionRepository, AppointAgentSessionRepository}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class CheckYourAnswersControllerFEISpec extends ISpecBase {

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  val agentCode = 1234
  val agentName = "Test Agent"

  lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
    app.injector.instanceOf[AppointAgentSessionRepository]
  lazy val mockAppointAgentPropertiesSessionRepository: AppointAgentPropertiesSessionRepository =
    app.injector.instanceOf[AppointAgentPropertiesSessionRepository]

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Check and confirm your details - Valuation Office Agency - GOV.UK"
  val headingText = "Check and confirm your details"
  val captionText = "Appoint an agent"
  val backLinkText = "Back"
  val agentHeadingText = "Agent"
  val agentAnswerText = "Test Agent"
  val ratingListHeadingText = "Which rating list do you want this agent to act on for you?"
  val ratingListAnswerThreeText = "2026, 2023, and 2017 rating lists"
  val ratingListAnswerTwoText = "2023 and 2017 rating lists"
  val ratingListAnswer2017Text = "2017 rating list"
  val ratingListAnswer2023Text = "2023 rating list"
  val propertiesHeadingText = "Which properties do you want to assign to this agent?"
  val propertiesAnswerNoPropertiesText = "No properties"
  val propertiesAnswerOnePropertyText = "Your property"
  val propertiesAnswerAllPropertiesText = "All properties"
  val propertiesAnswerSomePropertiesText = "5 of 10 properties"
  val changeLinkText = "Change"
  val confirmAndAppointButtonText = "Confirm and appoint"

  val titleTextWelsh = "Gwirio a chadarnhau eich manylion - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Gwirio a chadarnhau eich manylion"
  val captionTextWelsh = "Penodi asiant"
  val backLinkTextWelsh = "Yn ôl"
  val agentHeadingTextWelsh = "Asiant"
  val agentAnswerTextWelsh = "Test Agent"
  val ratingListHeadingTextWelsh = "Pa restr ardrethu yr hoffech i’r asiant hwn ei gweithredu ar eich rhan?"
  val ratingListAnswerThreeTextWelsh = "Rhestrau ardrethu 2026, 2023, a 2017"
  val ratingListAnswerTwoTextWelsh = "Rhestrau ardrethu 2023 a 2017"
  val ratingListAnswer2017TextWelsh = "Rhestr ardrethi 2017"
  val ratingListAnswer2023TextWelsh = "Rhestr ardrethi 2023"
  val propertiesHeadingTextWelsh = "Pa eiddo ydych chi’n dymuno neilltuo ir asiant hwn?"
  val propertiesAnswerNoPropertiesTextWelsh = "Dim eiddo"
  val propertiesAnswerOnePropertyTextWelsh = "Eich eiddo"
  val propertiesAnswerAllPropertiesTextWelsh = "Pob eiddo"
  val propertiesAnswerSomePropertiesTextWelsh = "5 o 10 eiddo"
  val changeLinkTextWelsh = "Newid"
  val confirmAndAppointButtonTextWelsh = "Cadarnhau a phenodi"

  val headingLocator = "h1"
  val captionLocator = "caption"
  val backLinkLocator = "back-link"
  val agentHeadingLocator = "agent-heading"
  val agentAnswerLocator = "agent-value"
  val agentChangeLinkLocator = "change-agent"
  val ratingListHeadingLocator = "ratings-heading"
  val ratingListAnswerLocator = "ratings-value"
  val ratingListChangeLinkLocator = "change-rating-years"
  val propertiesHeadingLocator = "properties-heading"
  val propertiesAnswerLocator = "properties-value"
  val propertiesChangeLinkLocator = "change-properties"
  val confirmAndAppointButtonLocator = "#main-content > div > div > form > button"

  val defaultBackLinkHref = "/business-rates-dashboard/home"
  val backLinkFromSessionHref = "/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties"
  val agentChangeLinkHref =
    "/business-rates-property-linking/my-organisation/appoint-new-agent/agent-code?fromCyaChange=true"
  val ratingListChangeLinkHref =
    "/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list-new?fromCyaChange=true"
  val multiplePropertiesChangeLinkHref =
    "/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties?fromCyaChange=true"
  val onePropertyChangeLinkHref =
    "/business-rates-property-linking/my-organisation/appoint-new-agent/one-property?fromCyaChange=true"
  val redirectLocation = "/business-rates-property-linking/my-organisation/confirm-appoint-agent"

  "CheckYourAnswersController onPageLoad displays the correct content in English (Client has no properties & 2017 list year)" which {
    lazy val res = getCheckYourAnswersPage(
      language = English,
      listYear = List("2017"),
      assignedProperties = 0,
      totalProperties = 0,
      backLink = None
    )

    lazy val document = Jsoup.parse(res.body)

    s"has a status of 200 OK" in {
      res.status shouldBe OK
    }

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.getElementById(captionLocator).text shouldBe captionText
    }

    "has a back link" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe defaultBackLinkHref
    }

    "has the correct agent details in the summary list" in {
      document.getElementById(agentHeadingLocator).text shouldBe agentHeadingText
      document.getElementById(agentAnswerLocator).text shouldBe agentAnswerText
      document.getElementById(agentChangeLinkLocator).text shouldBe s"$changeLinkText $agentHeadingText"
      document.getElementById(agentChangeLinkLocator).attr("href") shouldBe agentChangeLinkHref
    }

    "has the correct rating list details in the summary list" in {
      document.getElementById(ratingListHeadingLocator).text shouldBe ratingListHeadingText
      document.getElementById(ratingListAnswerLocator).text shouldBe ratingListAnswer2017Text
      document.getElementById(ratingListChangeLinkLocator).text shouldBe s"$changeLinkText $ratingListHeadingText"
      document.getElementById(ratingListChangeLinkLocator).attr("href") shouldBe ratingListChangeLinkHref
    }

    "has the correct property details in the summary list" in {
      document.getElementById(propertiesHeadingLocator).text shouldBe propertiesHeadingText
      document.getElementById(propertiesAnswerLocator).text shouldBe propertiesAnswerNoPropertiesText
      document.getElementById(propertiesChangeLinkLocator).text shouldBe s"$changeLinkText $propertiesHeadingText"
      document.getElementById(propertiesChangeLinkLocator).attr("href") shouldBe multiplePropertiesChangeLinkHref
    }

    s"has a $confirmAndAppointButtonText button" in {
      document.select(confirmAndAppointButtonLocator).text shouldBe confirmAndAppointButtonText
    }
  }

  "CheckYourAnswersController onPageLoad displays the correct content in English (Assigned to only property & 2023 list year)" which {
    lazy val res = getCheckYourAnswersPage(
      language = English,
      listYear = List("2023"),
      assignedProperties = 1,
      totalProperties = 1,
      backLink = None
    )

    lazy val document = Jsoup.parse(res.body)

    s"has a status of 200 OK" in {
      res.status shouldBe OK
    }

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.getElementById(captionLocator).text shouldBe captionText
    }

    "has a back link" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe defaultBackLinkHref
    }

    "has the correct agent details in the summary list" in {
      document.getElementById(agentHeadingLocator).text shouldBe agentHeadingText
      document.getElementById(agentAnswerLocator).text shouldBe agentAnswerText
      document.getElementById(agentChangeLinkLocator).text shouldBe s"$changeLinkText $agentHeadingText"
      document.getElementById(agentChangeLinkLocator).attr("href") shouldBe agentChangeLinkHref
    }

    "has the correct rating list details in the summary list" in {
      document.getElementById(ratingListHeadingLocator).text shouldBe ratingListHeadingText
      document.getElementById(ratingListAnswerLocator).text shouldBe ratingListAnswer2023Text
      document.getElementById(ratingListChangeLinkLocator).text shouldBe s"$changeLinkText $ratingListHeadingText"
      document.getElementById(ratingListChangeLinkLocator).attr("href") shouldBe ratingListChangeLinkHref
    }

    "has the correct property details in the summary list" in {
      document.getElementById(propertiesHeadingLocator).text shouldBe propertiesHeadingText
      document.getElementById(propertiesAnswerLocator).text shouldBe propertiesAnswerOnePropertyText
      document.getElementById(propertiesChangeLinkLocator).text shouldBe s"$changeLinkText $propertiesHeadingText"
      document.getElementById(propertiesChangeLinkLocator).attr("href") shouldBe multiplePropertiesChangeLinkHref
    }

    s"has a $confirmAndAppointButtonText button" in {
      document.select(confirmAndAppointButtonLocator).text shouldBe confirmAndAppointButtonText
    }
  }

  "CheckYourAnswersController onPageLoad displays the correct content in English (Assigned to all properties & two list years)" which {
    lazy val res = getCheckYourAnswersPage(
      language = English,
      listYear = List("2023", "2017"),
      assignedProperties = 10,
      totalProperties = 10,
      backLink = Some(backLinkFromSessionHref)
    )

    lazy val document = Jsoup.parse(res.body)

    s"has a status of 200 OK" in {
      res.status shouldBe OK
    }

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.getElementById(captionLocator).text shouldBe captionText
    }

    "has a back link" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkFromSessionHref
    }

    "has the correct agent details in the summary list" in {
      document.getElementById(agentHeadingLocator).text shouldBe agentHeadingText
      document.getElementById(agentAnswerLocator).text shouldBe agentAnswerText
      document.getElementById(agentChangeLinkLocator).text shouldBe s"$changeLinkText $agentHeadingText"
      document.getElementById(agentChangeLinkLocator).attr("href") shouldBe agentChangeLinkHref
    }

    "has the correct rating list details in the summary list" in {
      document.getElementById(ratingListHeadingLocator).text shouldBe ratingListHeadingText
      document.getElementById(ratingListAnswerLocator).text shouldBe ratingListAnswerTwoText
      document.getElementById(ratingListChangeLinkLocator).text shouldBe s"$changeLinkText $ratingListHeadingText"
      document.getElementById(ratingListChangeLinkLocator).attr("href") shouldBe ratingListChangeLinkHref
    }

    "has the correct property details in the summary list" in {
      document.getElementById(propertiesHeadingLocator).text shouldBe propertiesHeadingText
      document.getElementById(propertiesAnswerLocator).text shouldBe propertiesAnswerAllPropertiesText
      document.getElementById(propertiesChangeLinkLocator).text shouldBe s"$changeLinkText $propertiesHeadingText"
      document.getElementById(propertiesChangeLinkLocator).attr("href") shouldBe multiplePropertiesChangeLinkHref
    }

    s"has a $confirmAndAppointButtonText button" in {
      document.select(confirmAndAppointButtonLocator).text shouldBe confirmAndAppointButtonText
    }
  }

  "CheckYourAnswersController onPageLoad displays the correct content in English (Assigned to no properties & two list years)" which {
    lazy val res = getCheckYourAnswersPage(
      language = English,
      listYear = List("2023", "2017"),
      assignedProperties = 0,
      totalProperties = 10,
      backLink = Some(backLinkFromSessionHref)
    )

    lazy val document = Jsoup.parse(res.body)

    s"has a status of 200 OK" in {
      res.status shouldBe OK
    }

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.getElementById(captionLocator).text shouldBe captionText
    }

    "has a back link" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkFromSessionHref
    }

    "has the correct agent details in the summary list" in {
      document.getElementById(agentHeadingLocator).text shouldBe agentHeadingText
      document.getElementById(agentAnswerLocator).text shouldBe agentAnswerText
      document.getElementById(agentChangeLinkLocator).text shouldBe s"$changeLinkText $agentHeadingText"
      document.getElementById(agentChangeLinkLocator).attr("href") shouldBe agentChangeLinkHref
    }

    "has the correct rating list details in the summary list" in {
      document.getElementById(ratingListHeadingLocator).text shouldBe ratingListHeadingText
      document.getElementById(ratingListAnswerLocator).text shouldBe ratingListAnswerTwoText
      document.getElementById(ratingListChangeLinkLocator).text shouldBe s"$changeLinkText $ratingListHeadingText"
      document.getElementById(ratingListChangeLinkLocator).attr("href") shouldBe ratingListChangeLinkHref
    }

    "has the correct property details in the summary list" in {
      document.getElementById(propertiesHeadingLocator).text shouldBe propertiesHeadingText
      document.getElementById(propertiesAnswerLocator).text shouldBe propertiesAnswerNoPropertiesText
      document.getElementById(propertiesChangeLinkLocator).text shouldBe s"$changeLinkText $propertiesHeadingText"
      document.getElementById(propertiesChangeLinkLocator).attr("href") shouldBe multiplePropertiesChangeLinkHref
    }

    s"has a $confirmAndAppointButtonText button" in {
      document.select(confirmAndAppointButtonLocator).text shouldBe confirmAndAppointButtonText
    }
  }

  "CheckYourAnswersController onPageLoad displays the correct content in English (Assigned to some properties & all three list years)" which {
    lazy val res = getCheckYourAnswersPage(
      language = English,
      listYear = List("2026", "2023", "2017"),
      assignedProperties = 5,
      totalProperties = 10,
      backLink = Some(backLinkFromSessionHref)
    )

    lazy val document = Jsoup.parse(res.body)

    s"has a status of 200 OK" in {
      res.status shouldBe OK
    }

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.getElementById(captionLocator).text shouldBe captionText
    }

    "has a back link" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkFromSessionHref
    }

    "has the correct agent details in the summary list" in {
      document.getElementById(agentHeadingLocator).text shouldBe agentHeadingText
      document.getElementById(agentAnswerLocator).text shouldBe agentAnswerText
      document.getElementById(agentChangeLinkLocator).text shouldBe s"$changeLinkText $agentHeadingText"
      document.getElementById(agentChangeLinkLocator).attr("href") shouldBe agentChangeLinkHref
    }

    "has the correct rating list details in the summary list" in {
      document.getElementById(ratingListHeadingLocator).text shouldBe ratingListHeadingText
      document.getElementById(ratingListAnswerLocator).text shouldBe ratingListAnswerThreeText
      document.getElementById(ratingListChangeLinkLocator).text shouldBe s"$changeLinkText $ratingListHeadingText"
      document.getElementById(ratingListChangeLinkLocator).attr("href") shouldBe ratingListChangeLinkHref
    }

    "has the correct property details in the summary list" in {
      document.getElementById(propertiesHeadingLocator).text shouldBe propertiesHeadingText
      document.getElementById(propertiesAnswerLocator).text shouldBe propertiesAnswerSomePropertiesText
      document.getElementById(propertiesChangeLinkLocator).text shouldBe s"$changeLinkText $propertiesHeadingText"
      document.getElementById(propertiesChangeLinkLocator).attr("href") shouldBe multiplePropertiesChangeLinkHref
    }

    s"has a $confirmAndAppointButtonText button" in {
      document.select(confirmAndAppointButtonLocator).text shouldBe confirmAndAppointButtonText
    }
  }

  "CheckYourAnswersController onPageLoad displays the correct content in Welsh (Client has no properties & 2017 list year)" which {
    lazy val res = getCheckYourAnswersPage(
      language = Welsh,
      listYear = List("2017"),
      assignedProperties = 0,
      totalProperties = 0,
      backLink = None
    )

    lazy val document = Jsoup.parse(res.body)

    s"has a status of 200 OK" in {
      res.status shouldBe OK
    }

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.getElementById(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe defaultBackLinkHref
    }

    "has the correct agent details in the summary list in Welsh" in {
      document.getElementById(agentHeadingLocator).text shouldBe agentHeadingTextWelsh
      document.getElementById(agentAnswerLocator).text shouldBe agentAnswerTextWelsh
      document.getElementById(agentChangeLinkLocator).text shouldBe s"$changeLinkTextWelsh $agentHeadingTextWelsh"
      document.getElementById(agentChangeLinkLocator).attr("href") shouldBe agentChangeLinkHref
    }

    "has the correct rating list details in the summary list in Welsh" in {
      document.getElementById(ratingListHeadingLocator).text shouldBe ratingListHeadingTextWelsh
      document.getElementById(ratingListAnswerLocator).text shouldBe ratingListAnswer2017TextWelsh
      document
        .getElementById(ratingListChangeLinkLocator)
        .text shouldBe s"$changeLinkTextWelsh $ratingListHeadingTextWelsh"
      document.getElementById(ratingListChangeLinkLocator).attr("href") shouldBe ratingListChangeLinkHref
    }

    "has the correct property details in the summary list in Welsh" in {
      document.getElementById(propertiesHeadingLocator).text shouldBe propertiesHeadingTextWelsh
      document.getElementById(propertiesAnswerLocator).text shouldBe propertiesAnswerNoPropertiesTextWelsh
      document
        .getElementById(propertiesChangeLinkLocator)
        .text shouldBe s"$changeLinkTextWelsh $propertiesHeadingTextWelsh"
      document.getElementById(propertiesChangeLinkLocator).attr("href") shouldBe multiplePropertiesChangeLinkHref
    }

    s"has a $confirmAndAppointButtonText button in Welsh" in {
      document.select(confirmAndAppointButtonLocator).text shouldBe confirmAndAppointButtonTextWelsh
    }
  }

  "CheckYourAnswersController onPageLoad displays the correct content in Welsh (Assigned to only property & 2023 list year)" which {
    lazy val res = getCheckYourAnswersPage(
      language = Welsh,
      listYear = List("2023"),
      assignedProperties = 1,
      totalProperties = 1,
      backLink = None
    )

    lazy val document = Jsoup.parse(res.body)

    s"has a status of 200 OK" in {
      res.status shouldBe OK
    }

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.getElementById(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe defaultBackLinkHref
    }

    "has the correct agent details in the summary list in Welsh" in {
      document.getElementById(agentHeadingLocator).text shouldBe agentHeadingTextWelsh
      document.getElementById(agentAnswerLocator).text shouldBe agentAnswerTextWelsh
      document.getElementById(agentChangeLinkLocator).text shouldBe s"$changeLinkTextWelsh $agentHeadingTextWelsh"
      document.getElementById(agentChangeLinkLocator).attr("href") shouldBe agentChangeLinkHref
    }

    "has the correct rating list details in the summary list in Welsh" in {
      document.getElementById(ratingListHeadingLocator).text shouldBe ratingListHeadingTextWelsh
      document.getElementById(ratingListAnswerLocator).text shouldBe ratingListAnswer2023TextWelsh
      document
        .getElementById(ratingListChangeLinkLocator)
        .text shouldBe s"$changeLinkTextWelsh $ratingListHeadingTextWelsh"
      document.getElementById(ratingListChangeLinkLocator).attr("href") shouldBe ratingListChangeLinkHref
    }

    "has the correct property details in the summary list in Welsh" in {
      document.getElementById(propertiesHeadingLocator).text shouldBe propertiesHeadingTextWelsh
      document.getElementById(propertiesAnswerLocator).text shouldBe propertiesAnswerOnePropertyTextWelsh
      document
        .getElementById(propertiesChangeLinkLocator)
        .text shouldBe s"$changeLinkTextWelsh $propertiesHeadingTextWelsh"
      document.getElementById(propertiesChangeLinkLocator).attr("href") shouldBe multiplePropertiesChangeLinkHref
    }

    s"has a $confirmAndAppointButtonText button in Welsh" in {
      document.select(confirmAndAppointButtonLocator).text shouldBe confirmAndAppointButtonTextWelsh
    }
  }

  "CheckYourAnswersController onPageLoad displays the correct content in Welsh (Assigned to all properties & two list years)" which {
    lazy val res = getCheckYourAnswersPage(
      language = Welsh,
      listYear = List("2023", "2017"),
      assignedProperties = 10,
      totalProperties = 10,
      backLink = Some(backLinkFromSessionHref)
    )

    lazy val document = Jsoup.parse(res.body)

    s"has a status of 200 OK" in {
      res.status shouldBe OK
    }

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.getElementById(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkFromSessionHref
    }

    "has the correct agent details in the summary list in Welsh" in {
      document.getElementById(agentHeadingLocator).text shouldBe agentHeadingTextWelsh
      document.getElementById(agentAnswerLocator).text shouldBe agentAnswerTextWelsh
      document.getElementById(agentChangeLinkLocator).text shouldBe s"$changeLinkTextWelsh $agentHeadingTextWelsh"
      document.getElementById(agentChangeLinkLocator).attr("href") shouldBe agentChangeLinkHref
    }

    "has the correct rating list details in the summary list in Welsh" in {
      document.getElementById(ratingListHeadingLocator).text shouldBe ratingListHeadingTextWelsh
      document.getElementById(ratingListAnswerLocator).text shouldBe ratingListAnswerTwoTextWelsh
      document
        .getElementById(ratingListChangeLinkLocator)
        .text shouldBe s"$changeLinkTextWelsh $ratingListHeadingTextWelsh"
      document.getElementById(ratingListChangeLinkLocator).attr("href") shouldBe ratingListChangeLinkHref
    }

    "has the correct property details in the summary list in Welsh" in {
      document.getElementById(propertiesHeadingLocator).text shouldBe propertiesHeadingTextWelsh
      document.getElementById(propertiesAnswerLocator).text shouldBe propertiesAnswerAllPropertiesTextWelsh
      document
        .getElementById(propertiesChangeLinkLocator)
        .text shouldBe s"$changeLinkTextWelsh $propertiesHeadingTextWelsh"
      document.getElementById(propertiesChangeLinkLocator).attr("href") shouldBe multiplePropertiesChangeLinkHref
    }

    s"has a $confirmAndAppointButtonText button in Welsh" in {
      document.select(confirmAndAppointButtonLocator).text shouldBe confirmAndAppointButtonTextWelsh
    }
  }

  "CheckYourAnswersController onPageLoad displays the correct content in Welsh (Assigned to no properties & two list years)" which {
    lazy val res = getCheckYourAnswersPage(
      language = Welsh,
      listYear = List("2023", "2017"),
      assignedProperties = 0,
      totalProperties = 10,
      backLink = Some(backLinkFromSessionHref)
    )

    lazy val document = Jsoup.parse(res.body)

    s"has a status of 200 OK" in {
      res.status shouldBe OK
    }

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.getElementById(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkFromSessionHref
    }

    "has the correct agent details in the summary list in Welsh" in {
      document.getElementById(agentHeadingLocator).text shouldBe agentHeadingTextWelsh
      document.getElementById(agentAnswerLocator).text shouldBe agentAnswerTextWelsh
      document.getElementById(agentChangeLinkLocator).text shouldBe s"$changeLinkTextWelsh $agentHeadingTextWelsh"
      document.getElementById(agentChangeLinkLocator).attr("href") shouldBe agentChangeLinkHref
    }

    "has the correct rating list details in the summary list in Welsh" in {
      document.getElementById(ratingListHeadingLocator).text shouldBe ratingListHeadingTextWelsh
      document.getElementById(ratingListAnswerLocator).text shouldBe ratingListAnswerTwoTextWelsh
      document
        .getElementById(ratingListChangeLinkLocator)
        .text shouldBe s"$changeLinkTextWelsh $ratingListHeadingTextWelsh"
      document.getElementById(ratingListChangeLinkLocator).attr("href") shouldBe ratingListChangeLinkHref
    }

    "has the correct property details in the summary list in Welsh" in {
      document.getElementById(propertiesHeadingLocator).text shouldBe propertiesHeadingTextWelsh
      document.getElementById(propertiesAnswerLocator).text shouldBe propertiesAnswerNoPropertiesTextWelsh
      document
        .getElementById(propertiesChangeLinkLocator)
        .text shouldBe s"$changeLinkTextWelsh $propertiesHeadingTextWelsh"
      document.getElementById(propertiesChangeLinkLocator).attr("href") shouldBe multiplePropertiesChangeLinkHref
    }

    s"has a $confirmAndAppointButtonText button in Welsh" in {
      document.select(confirmAndAppointButtonLocator).text shouldBe confirmAndAppointButtonTextWelsh
    }
  }

  "CheckYourAnswersController onPageLoad displays the correct content in Welsh (Assigned to some properties & all three list years)" which {
    lazy val res = getCheckYourAnswersPage(
      language = Welsh,
      listYear = List("2026", "2023", "2017"),
      assignedProperties = 5,
      totalProperties = 10,
      backLink = Some(backLinkFromSessionHref)
    )

    lazy val document = Jsoup.parse(res.body)

    s"has a status of 200 OK" in {
      res.status shouldBe OK
    }

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.getElementById(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkFromSessionHref
    }

    "has the correct agent details in the summary list in Welsh" in {
      document.getElementById(agentHeadingLocator).text shouldBe agentHeadingTextWelsh
      document.getElementById(agentAnswerLocator).text shouldBe agentAnswerTextWelsh
      document.getElementById(agentChangeLinkLocator).text shouldBe s"$changeLinkTextWelsh $agentHeadingTextWelsh"
      document.getElementById(agentChangeLinkLocator).attr("href") shouldBe agentChangeLinkHref
    }

    "has the correct rating list details in the summary list in Welsh" in {
      document.getElementById(ratingListHeadingLocator).text shouldBe ratingListHeadingTextWelsh
      document.getElementById(ratingListAnswerLocator).text shouldBe ratingListAnswerThreeTextWelsh
      document
        .getElementById(ratingListChangeLinkLocator)
        .text shouldBe s"$changeLinkTextWelsh $ratingListHeadingTextWelsh"
      document.getElementById(ratingListChangeLinkLocator).attr("href") shouldBe ratingListChangeLinkHref
    }

    "has the correct property details in the summary list in Welsh" in {
      document.getElementById(propertiesHeadingLocator).text shouldBe propertiesHeadingTextWelsh
      document.getElementById(propertiesAnswerLocator).text shouldBe propertiesAnswerSomePropertiesTextWelsh
      document
        .getElementById(propertiesChangeLinkLocator)
        .text shouldBe s"$changeLinkTextWelsh $propertiesHeadingTextWelsh"
      document.getElementById(propertiesChangeLinkLocator).attr("href") shouldBe multiplePropertiesChangeLinkHref
    }

    s"has a $confirmAndAppointButtonText button in Welsh" in {
      document.select(confirmAndAppointButtonLocator).text shouldBe confirmAndAppointButtonTextWelsh
    }
  }

  "CheckYourAnswersController onPageLoad returns 404 NOT FOUND when no ManagingPropertyData is found in the cache" which {
    lazy val res = getCheckYourAnswersPage(
      language = English,
      listYear = List("2023", "2017"),
      assignedProperties = 10,
      totalProperties = 10,
      backLink = Some(backLinkFromSessionHref),
      errorPage = true
    )

    s"has a status of 404 NOT FOUND" in {
      res.status shouldBe NOT_FOUND
    }
  }

  "CheckYourAnswersController onSubmit returns 303 & redirects to the confirm appointment page on successful appointment (All properties & 2026 list year)" which {
    lazy val res = postCheckYourAnswersPage(ALL_PROPERTIES, List("2026"))

    "has a status of 303 SEE OTHER with the correct redirect location & request sent to the backend" in {
      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe redirectLocation

      val expJsonBody = getExpectedJsonBody(ALL_PROPERTIES, Seq("2026"))
      verify(
        1,
        postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
          .withRequestBody(equalToJson(expJsonBody.toString()))
      )
    }
  }

  "CheckYourAnswersController onSubmit returns 303 & redirects to the confirm appointment page on successful appointment (All properties & 2023 list year)" which {
    lazy val res = postCheckYourAnswersPage(ALL_PROPERTIES, List("2023"))

    "has a status of 303 SEE OTHER with the correct redirect location & request sent to the backend" in {
      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe redirectLocation

      val expJsonBody = getExpectedJsonBody(ALL_PROPERTIES, Seq("2023"))
      verify(
        1,
        postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
          .withRequestBody(equalToJson(expJsonBody.toString()))
      )
    }
  }

  "CheckYourAnswersController onSubmit returns 303 & redirects to the confirm appointment page on successful appointment (Some properties & 2017 list year)" which {
    lazy val res = postCheckYourAnswersPage(PROPERTY_LIST, List("2017"))

    "has a status of 303 SEE OTHER with the correct redirect location & request sent to the backend" in {
      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe redirectLocation

      val expJsonBody = getExpectedJsonBody(PROPERTY_LIST, Seq("2017"))
      verify(
        1,
        postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
          .withRequestBody(equalToJson(expJsonBody.toString()))
      )
    }
  }

  "CheckYourAnswersController onSubmit returns 303 & redirects to the confirm appointment page on successful appointment (No properties & two list years)" which {
    lazy val res = postCheckYourAnswersPage(RELATIONSHIP, List("2023", "2017"))

    "has a status of 303 SEE OTHER with the correct redirect location & request sent to the backend" in {
      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe redirectLocation

      val expJsonBody = getExpectedJsonBody(RELATIONSHIP, Seq("2017", "2023"))
      verify(
        1,
        postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
          .withRequestBody(equalToJson(expJsonBody.toString()))
      )
    }
  }

  "CheckYourAnswersController onSubmit returns 303 & redirects to the confirm appointment page on successful appointment (No properties & three list years)" which {
    lazy val res = postCheckYourAnswersPage(RELATIONSHIP, List("2026", "2023", "2017"))

    "has a status of 303 SEE OTHER with the correct redirect location & request sent to the backend" in {
      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe redirectLocation

      val expJsonBody = getExpectedJsonBody(RELATIONSHIP, Seq("2017", "2023", "2026"))
      verify(
        1,
        postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
          .withRequestBody(equalToJson(expJsonBody.toString()))
      )
    }
  }

  "CheckYourAnswersController onSubmit returns 400 BAD REQUEST when an agent code isn't submitted" which {
    lazy val res = postCheckYourAnswersPage(ALL_PROPERTIES, List("2023"), Some("Missing agentCode"))

    "has a status of 400 BAD REQUEST" in {
      res.status shouldBe BAD_REQUEST
    }
  }

  "CheckYourAnswersController onSubmit returns 400 BAD REQUEST when a scope isn't submitted" which {
    lazy val res = postCheckYourAnswersPage(ALL_PROPERTIES, List("2017"), Some("Missing scope"))

    "has a status of 400 BAD REQUEST" in {
      res.status shouldBe BAD_REQUEST
    }
  }
  private def commonStubs(
        listYear: List[String],
        assignedProperties: Int,
        totalProperties: Int,
        backLink: Option[String] = None
  ) = {

    val selectedOption = (assignedProperties, totalProperties) match {
      case (0, _)                                                 => "none"
      case (assigned, total) if assigned >= 1 && assigned < total => "choose_from_list"
      case _                                                      => "all"
    }

    val managingPropertyData: ManagingProperty = ManagingProperty(
      agentCode = agentCode,
      agentOrganisationName = agentName,
      isCorrectAgent = true,
      managingPropertyChoice = selectedOption,
      agentAddress = "1 Agent Street, AG3 NT1",
      backLink = backLink,
      totalPropertySelectionSize = totalProperties,
      propertySelectedSize = assignedProperties,
      ratingLists = listYear
    )

    val propertiesSessionData: AppointAgentToSomePropertiesSession = AppointAgentToSomePropertiesSession(
      agentAppointAction = Some(
        AgentAppointBulkAction(
          agentCode = agentCode,
          name = agentName,
          propertyLinkIds = List("123", "321"),
          backLinkUrl = "some-back-link"
        )
      ),
      filters = FilterAppointProperties(None, None)
    )

    await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData))
    await(mockAppointAgentPropertiesSessionRepository.saveOrUpdate(propertiesSessionData))

    stubFor {
      post("/property-linking/my-organisation/agent/submit-appointment-changes")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(AgentAppointmentChangesResponse("some-id")).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentListFor2017).toString())
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

  private def getCheckYourAnswersPage(
        language: Language,
        listYear: List[String],
        assignedProperties: Int,
        totalProperties: Int,
        backLink: Option[String],
        errorPage: Boolean = false
  ) = {
    commonStubs(listYear, assignedProperties, totalProperties, backLink)

    if (errorPage) {
      mockAppointAgentSessionRepository.remove()
      mockAppointAgentPropertiesSessionRepository.remove()
    }

    await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )
  }

  private def postCheckYourAnswersPage(
        scope: AppointmentScope,
        listYear: List[String],
        errorType: Option[String] = None
  ) = {
    val (assignedProperties, totalProperties) = scope match {
      case ALL_PROPERTIES => (10, 10)
      case PROPERTY_LIST  => (5, 10)
      case RELATIONSHIP   => (0, 10)
    }

    commonStubs(listYear, assignedProperties, totalProperties)

    val request = Json.obj("agentCode" -> agentCode, "scope" -> s"$scope")

    val requestBody = errorType match {
      case Some("Missing agentCode") => request - "agentCode"
      case Some("Missing scope")     => request - "scope"
      case _                         => request
    }

    await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"
      ).withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )
  }

  private def getExpectedJsonBody(scope: AppointmentScope, listYears: Seq[String]) =
    Json.parse(s"""{
                  |   "agentRepresentativeCode": $agentCode,
                  |   "action":"APPOINT",
                  |   "scope":"$scope",
                  |   "propertyLinks":[
                  |       "123", "321"
                  |   ],
                  |   "listYears": ${Json
                   .toJson(listYears)}
                  |}""".stripMargin)
}
