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

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import models.propertyrepresentation.{ManagingProperty, ManagingPropertySelected, SelectedAgent}
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.AppointAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class AppointMultiplePropertiesISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
    app.injector.instanceOf[AppointAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Which of your properties do you want to assign Test Agent to? - Valuation Office Agency - GOV.UK"
  val errorTitleText =
    "Error: Which of your properties do you want to assign Test Agent to? - Valuation Office Agency - GOV.UK"
  val headingText = "Which of your properties do you want to assign Test Agent to?"
  val captionText = "Appoint an agent"
  val backLinkText = "Back"
  val assignToAllText = "Assign to all properties"
  val assignToOneOrMoreText = "Assign to one or more properties"
  val doNotAssignText = "Do not assign to any properties"
  val continueButtonText = "Continue"
  val errorSummaryTitleText = "There is a problem"
  val errorMessageText = "Select if you want your agent to manage any of your properties"
  val error = "Error: "

  val titleTextWelsh = "Pa un o’ch eiddo yr hoffech ei neilltuo i Test Agent? - Valuation Office Agency - GOV.UK"
  val errorTitleTextWelsh =
    "Gwall: Pa un o’ch eiddo yr hoffech ei neilltuo i Test Agent? - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Pa un o’ch eiddo yr hoffech ei neilltuo i Test Agent?"
  val captionTextWelsh = "Penodi asiant"
  val backLinkTextWelsh = "Yn ôl"
  val assignToAllTextWelsh = "Neilltuo pob eiddo iddo"
  val assignToOneOrMoreTextWelsh = "Neilltuo un eiddo neu fwy iddo"
  val doNotAssignTextWelsh = "Peidio â neilltuo unrhyw eiddo iddo"
  val continueButtonTextWelsh = "Yn eich blaen"
  val errorSummaryTitleTextWelsh = "Mae yna broblem"
  val errorMessageTextWelsh = "Dewiswch os ydych am i’ch asiant reoli unrhyw un o’ch eiddo"
  val errorWelsh = "Gwall: "

  val headingLocator = "h1"
  val captionLocator = "caption"
  val backLinkLocator = "back-link"
  val assignToAllRadioLocator = "multipleProperties"
  val assignToAllLabelLocator = "#main-content > div > div > form > div > div > div:nth-child(1) > label"
  val assignToOneOrMoreRadioLocator = "multipleProperties-2"
  val assignToOneOrMoreLabelLocator = "#main-content > div > div > form > div > div > div:nth-child(2) > label"
  val doNotAssignRadioLocator = "multipleProperties-3"
  val doNotAssignLabelLocator = "#main-content > div > div > form > div > div > div:nth-child(3) > label"
  val continueButtonLocator = "#main-content > div > div > form > button"
  val errorTitleLocator = "#main-content > div > div > div > div > h2"
  val errorSummaryMessageLocator = "#main-content > div > div > div > div > div > ul > li > a"
  val radioErrorMessageLocator = "multipleProperties-error"

  val backLinkHref = "/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list"
  val fromCyaBackLinkHref = "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"
  val errorHref = "#multipleProperties"
  val assignToOneOrMoreRedirectUrl =
    "/business-rates-property-linking/my-organisation/appoint/properties?page=1&pageSize=15&agentCode=1234&agentAppointed=BOTH&backLinkUrl=%2Fbusiness-rates-property-linking%2Fmy-organisation%2Fappoint-new-agent%2Fmultiple-properties&fromManageAgentJourney=false"

  "AddAgentController multipleProperties displays the correct content in English" which {
    lazy val document =
      getAssignToMultiplePropertiesPage(language = English, fromCya = false, cyaPreSelectedRadioButton = None)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.getElementById(captionLocator).text shouldBe captionText
    }

    s"has a back link of $backLinkText" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has an $assignToAllText radio button" in {
      document.getElementById(assignToAllRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToAllRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToAllLabelLocator).text shouldBe assignToAllText
    }

    s"has an $assignToOneOrMoreText radio button" in {
      document.getElementById(assignToOneOrMoreRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToOneOrMoreRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToOneOrMoreLabelLocator).text shouldBe assignToOneOrMoreText
    }

    s"has a $doNotAssignText radio button" in {
      document.getElementById(doNotAssignRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(doNotAssignRadioLocator).hasAttr("checked") shouldBe false
      document.select(doNotAssignLabelLocator).text shouldBe doNotAssignText
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "AddAgentController multipleProperties displays the correct content in Welsh" which {
    lazy val document =
      getAssignToMultiplePropertiesPage(language = Welsh, fromCya = false, cyaPreSelectedRadioButton = None)

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.getElementById(captionLocator).text shouldBe captionTextWelsh
    }

    s"has a back link of $backLinkText in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has an $assignToAllText radio button in Welsh" in {
      document.getElementById(assignToAllRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToAllRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToAllLabelLocator).text shouldBe assignToAllTextWelsh
    }

    s"has an $assignToOneOrMoreText radio button in Welsh" in {
      document.getElementById(assignToOneOrMoreRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToOneOrMoreRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToOneOrMoreLabelLocator).text shouldBe assignToOneOrMoreTextWelsh
    }

    s"has a $doNotAssignText radio button in Welsh" in {
      document.getElementById(doNotAssignRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(doNotAssignRadioLocator).hasAttr("checked") shouldBe false
      document.select(doNotAssignLabelLocator).text shouldBe doNotAssignTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "AddAgentController multipleProperties displays the correct content in English when coming back from the Check Your Answers page with the 'All properties' radio preselected" which {
    lazy val document =
      getAssignToMultiplePropertiesPage(language = English, fromCya = true, cyaPreSelectedRadioButton = Some("all"))

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.getElementById(captionLocator).text shouldBe captionText
    }

    s"has a back link of $backLinkText" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe fromCyaBackLinkHref
    }

    s"has an $assignToAllText radio button" in {
      document.getElementById(assignToAllRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToAllRadioLocator).hasAttr("checked") shouldBe true
      document.select(assignToAllLabelLocator).text shouldBe assignToAllText
    }

    s"has an $assignToOneOrMoreText radio button" in {
      document.getElementById(assignToOneOrMoreRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToOneOrMoreRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToOneOrMoreLabelLocator).text shouldBe assignToOneOrMoreText
    }

    s"has a $doNotAssignText radio button" in {
      document.getElementById(doNotAssignRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(doNotAssignRadioLocator).hasAttr("checked") shouldBe false
      document.select(doNotAssignLabelLocator).text shouldBe doNotAssignText
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "AddAgentController multipleProperties displays the correct content in English when coming back from the Check Your Answers page with the 'One or more properties' radio preselected" which {
    lazy val document = getAssignToMultiplePropertiesPage(
      language = English,
      fromCya = true,
      cyaPreSelectedRadioButton = Some("choose_from_list")
    )

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.getElementById(captionLocator).text shouldBe captionText
    }

    s"has a back link of $backLinkText" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe fromCyaBackLinkHref
    }

    s"has an $assignToAllText radio button" in {
      document.getElementById(assignToAllRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToAllRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToAllLabelLocator).text shouldBe assignToAllText
    }

    s"has an $assignToOneOrMoreText radio button" in {
      document.getElementById(assignToOneOrMoreRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToOneOrMoreRadioLocator).hasAttr("checked") shouldBe true
      document.select(assignToOneOrMoreLabelLocator).text shouldBe assignToOneOrMoreText
    }

    s"has a $doNotAssignText radio button" in {
      document.getElementById(doNotAssignRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(doNotAssignRadioLocator).hasAttr("checked") shouldBe false
      document.select(doNotAssignLabelLocator).text shouldBe doNotAssignText
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "AddAgentController multipleProperties displays the correct content in English when coming back from the Check Your Answers page with the 'Do not assign to any properties' radio preselected" which {
    lazy val document =
      getAssignToMultiplePropertiesPage(language = English, fromCya = true, cyaPreSelectedRadioButton = Some("none"))

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.getElementById(captionLocator).text shouldBe captionText
    }

    s"has a back link of $backLinkText" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe fromCyaBackLinkHref
    }

    s"has an $assignToAllText radio button" in {
      document.getElementById(assignToAllRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToAllRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToAllLabelLocator).text shouldBe assignToAllText
    }

    s"has an $assignToOneOrMoreText radio button" in {
      document.getElementById(assignToOneOrMoreRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToOneOrMoreRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToOneOrMoreLabelLocator).text shouldBe assignToOneOrMoreText
    }

    s"has a $doNotAssignText radio button" in {
      document.getElementById(doNotAssignRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(doNotAssignRadioLocator).hasAttr("checked") shouldBe true
      document.select(doNotAssignLabelLocator).text shouldBe doNotAssignText
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "AddAgentController multipleProperties displays the correct content in Welsh when coming back from the Check Your Answers page with the 'All properties' radio preselected" which {
    lazy val document =
      getAssignToMultiplePropertiesPage(language = Welsh, fromCya = true, cyaPreSelectedRadioButton = Some("all"))

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.getElementById(captionLocator).text shouldBe captionTextWelsh
    }

    s"has a back link of $backLinkText in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe fromCyaBackLinkHref
    }

    s"has an $assignToAllText radio button in Welsh" in {
      document.getElementById(assignToAllRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToAllRadioLocator).hasAttr("checked") shouldBe true
      document.select(assignToAllLabelLocator).text shouldBe assignToAllTextWelsh
    }

    s"has an $assignToOneOrMoreText radio button in Welsh" in {
      document.getElementById(assignToOneOrMoreRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToOneOrMoreRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToOneOrMoreLabelLocator).text shouldBe assignToOneOrMoreTextWelsh
    }

    s"has a $doNotAssignText radio button in Welsh" in {
      document.getElementById(doNotAssignRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(doNotAssignRadioLocator).hasAttr("checked") shouldBe false
      document.select(doNotAssignLabelLocator).text shouldBe doNotAssignTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "AddAgentController multipleProperties displays the correct content in Welsh when coming back from the Check Your Answers page with the 'One or more properties' radio preselected" which {
    lazy val document = getAssignToMultiplePropertiesPage(
      language = Welsh,
      fromCya = true,
      cyaPreSelectedRadioButton = Some("choose_from_list")
    )

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.getElementById(captionLocator).text shouldBe captionTextWelsh
    }

    s"has a back link of $backLinkText in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe fromCyaBackLinkHref
    }

    s"has an $assignToAllText radio button in Welsh" in {
      document.getElementById(assignToAllRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToAllRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToAllLabelLocator).text shouldBe assignToAllTextWelsh
    }

    s"has an $assignToOneOrMoreText radio button in Welsh" in {
      document.getElementById(assignToOneOrMoreRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToOneOrMoreRadioLocator).hasAttr("checked") shouldBe true
      document.select(assignToOneOrMoreLabelLocator).text shouldBe assignToOneOrMoreTextWelsh
    }

    s"has a $doNotAssignText radio button in Welsh" in {
      document.getElementById(doNotAssignRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(doNotAssignRadioLocator).hasAttr("checked") shouldBe false
      document.select(doNotAssignLabelLocator).text shouldBe doNotAssignTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "AddAgentController multipleProperties displays the correct content in Welsh when coming back from the Check Your Answers page with the 'Do not assign to any properties' radio preselected" which {
    lazy val document =
      getAssignToMultiplePropertiesPage(language = Welsh, fromCya = true, cyaPreSelectedRadioButton = Some("none"))

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.getElementById(captionLocator).text shouldBe captionTextWelsh
    }

    s"has a back link of $backLinkText in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe fromCyaBackLinkHref
    }

    s"has an $assignToAllText radio button in Welsh" in {
      document.getElementById(assignToAllRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToAllRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToAllLabelLocator).text shouldBe assignToAllTextWelsh
    }

    s"has an $assignToOneOrMoreText radio button in Welsh" in {
      document.getElementById(assignToOneOrMoreRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToOneOrMoreRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToOneOrMoreLabelLocator).text shouldBe assignToOneOrMoreTextWelsh
    }

    s"has a $doNotAssignText radio button in Welsh" in {
      document.getElementById(doNotAssignRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(doNotAssignRadioLocator).hasAttr("checked") shouldBe true
      document.select(doNotAssignLabelLocator).text shouldBe doNotAssignTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "AddAgentController submitMultipleProperties displays an error in English when a radio button isn't selected" which {
    lazy val res = postAssignToMultiplePropertiesPage(language = English, selectedRadio = None)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorTitleText" in {
      document.title shouldBe errorTitleText
    }

    s"has an error summary with a title of $errorSummaryTitleText and an error message of $errorMessageText" in {
      document.select(errorTitleLocator).text shouldBe errorSummaryTitleText
      document.select(errorSummaryMessageLocator).text shouldBe errorMessageText
      document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
    }

    s"has an error above the radios of $errorMessageText" in {
      document.getElementById(radioErrorMessageLocator).text shouldBe error + errorMessageText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.getElementById(captionLocator).text shouldBe captionText
    }

    s"has a back link of $backLinkText" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has an $assignToAllText radio button" in {
      document.getElementById(assignToAllRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToAllRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToAllLabelLocator).text shouldBe assignToAllText
    }

    s"has an $assignToOneOrMoreText radio button" in {
      document.getElementById(assignToOneOrMoreRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToOneOrMoreRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToOneOrMoreLabelLocator).text shouldBe assignToOneOrMoreText
    }

    s"has a $doNotAssignText radio button" in {
      document.getElementById(doNotAssignRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(doNotAssignRadioLocator).hasAttr("checked") shouldBe false
      document.select(doNotAssignLabelLocator).text shouldBe doNotAssignText
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "AddAgentController submitMultipleProperties displays an error in Welsh when a radio button isn't selected" which {
    lazy val res = postAssignToMultiplePropertiesPage(language = Welsh, selectedRadio = None)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorTitleText in Welsh" in {
      document.title shouldBe errorTitleTextWelsh
    }

    s"has an error summary with a title of $errorSummaryTitleText and an error message of $errorMessageText in Welsh" in {
      document.select(errorTitleLocator).text shouldBe errorSummaryTitleTextWelsh
      document.select(errorSummaryMessageLocator).text shouldBe errorMessageTextWelsh
      document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
    }

    s"has an error above the radios of $errorMessageText in Welsh" in {
      document.getElementById(radioErrorMessageLocator).text shouldBe errorWelsh + errorMessageTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.getElementById(captionLocator).text shouldBe captionTextWelsh
    }

    s"has a back link of $backLinkText in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has an $assignToAllText radio button in Welsh" in {
      document.getElementById(assignToAllRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToAllRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToAllLabelLocator).text shouldBe assignToAllTextWelsh
    }

    s"has an $assignToOneOrMoreText radio button in Welsh" in {
      document.getElementById(assignToOneOrMoreRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(assignToOneOrMoreRadioLocator).hasAttr("checked") shouldBe false
      document.select(assignToOneOrMoreLabelLocator).text shouldBe assignToOneOrMoreTextWelsh
    }

    s"has a $doNotAssignText radio button in Welsh" in {
      document.getElementById(doNotAssignRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(doNotAssignRadioLocator).hasAttr("checked") shouldBe false
      document.select(doNotAssignLabelLocator).text shouldBe doNotAssignTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "AddAgentController submitMultipleProperties redirects the user to the 'Check your answers' page when the 'Assign to all properties' radio is selected" which {
    lazy val res = postAssignToMultiplePropertiesPage(language = English, selectedRadio = Some("all"))

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(fromCyaBackLinkHref)
    }

  }

  "AddAgentController submitMultipleProperties redirects the user to the 'Check your answers' page when the 'Do not assign to any properties' radio is selected" which {
    lazy val res = postAssignToMultiplePropertiesPage(language = Welsh, selectedRadio = Some("none"))

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(fromCyaBackLinkHref)
    }
  }

  "AddAgentController submitMultipleProperties redirects the user to the 'Choose which properties' page when the 'Assign one or more properties' radio is selected" which {
    lazy val res = postAssignToMultiplePropertiesPage(language = Welsh, selectedRadio = Some("choose_from_list"))

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(assignToOneOrMoreRedirectUrl)
    }

  }
  private def stubAuth(): Unit = {
    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentListForBoth).toString())
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

  private def getAssignToMultiplePropertiesPage(
        language: Language,
        fromCya: Boolean,
        cyaPreSelectedRadioButton: Option[String]
  ) = {
    stubAuth()

    if (fromCya) {
      val cacheData = ManagingProperty(
        agentCode = 1234,
        agentOrganisationName = "Test Agent",
        agentAddress = "1 Agent Street, AG3 NT1",
        isCorrectAgent = true,
        managingPropertyChoice = cyaPreSelectedRadioButton.getOrElse(""),
        status = ManagingPropertySelected,
        backLink = None,
        totalPropertySelectionSize = 10,
        propertySelectedSize = 10,
        ratingLists = Seq("2023", "2017")
      )
      await(mockAppointAgentSessionRepository.saveOrUpdate(cacheData))
    } else {
      val cacheData = SelectedAgent(
        agentCode = 1234,
        agentOrganisationName = "Test Agent",
        agentAddress = "1 Agent Street, AG3 NT1",
        isCorrectAgent = true,
        backLink = None,
        ratingLists = Seq("2023", "2017")
      )

      await(mockAppointAgentSessionRepository.saveOrUpdate(cacheData))
    }

    val url = if (fromCya) "multiple-properties?fromCyaChange=true" else "multiple-properties"
    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/$url")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def postAssignToMultiplePropertiesPage(language: Language, selectedRadio: Option[String]) = {
    stubAuth()

    stubFor {
      get("/property-linking/owner/property-links/count")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testResultCount).toString())
        }
    }

    val cacheData = SelectedAgent(
      agentCode = 1234,
      agentOrganisationName = "Test Agent",
      agentAddress = "1 Agent Street, AG3 NT1",
      isCorrectAgent = true,
      backLink = Some("/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list"),
      ratingLists = Seq("2023", "2017")
    )

    await(mockAppointAgentSessionRepository.saveOrUpdate(cacheData))

    await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .withFollowRedirects(follow = false)
        .post(Map("multipleProperties" -> Seq(selectedRadio.getOrElse(""))))
    )
  }

}
