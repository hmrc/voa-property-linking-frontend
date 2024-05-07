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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.{ManagingProperty, ManagingPropertySelected, SelectedAgent}
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.AppointAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class AppointOnePropertyISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
    app.injector.instanceOf[AppointAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Do you want to assign Test Agent to your property? - Valuation Office Agency - GOV.UK"
  val headingText = "Do you want to assign Test Agent to your property?"
  val captionText = "Appoint an agent"
  val backLinkText = "Back"
  val yesRadioText = "Yes"
  val noRadioText = "No"
  val continueButtonText = "Continue"
  val errorTitleText = "Error: Do you want to assign Test Agent to your property? - Valuation Office Agency - GOV.UK"
  val errorSummaryTitleText = "There is a problem"
  val errorMessageText = "Select if you want your agent to manage your property"
  val error = "Error: "

  val titleTextWelsh = "Pa un o’ch eiddo yr hoffech ei neilltuo i Test Agent? - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Pa un o’ch eiddo yr hoffech ei neilltuo i Test Agent?"
  val captionTextWelsh = "Penodi asiant"
  val backLinkTextWelsh = "Yn ôl"
  val yesRadioTextWelsh = "Ie"
  val noRadioTextWelsh = "Na"
  val continueButtonTextWelsh = "Yn eich blaen"
  val errorTitleTextWelsh =
    "Gwall: Pa un o’ch eiddo yr hoffech ei neilltuo i Test Agent? - Valuation Office Agency - GOV.UK"
  val errorSummaryTitleTextWelsh = "Mae yna broblem"
  val errorMessageTextWelsh = "Dewiswch os ydych am i’ch asiant reoli eich eiddo"
  val errorWelsh = "Gwall: "

  val headingLocator = "h1"
  val captionLocator = "caption"
  val backLinkLocator = "back-link"
  val yesRadioLocator = "oneProperty"
  val yesRadioLabelLocator = "#main-content > div > div > form > div > div > div:nth-child(1) > label"
  val noRadioLocator = "oneProperty-2"
  val noRadioLabelLocator = "#main-content > div > div > form > div > div > div:nth-child(2) > label"
  val continueButtonLocator = "#main-content > div > div > form > button"
  val errorTitleLocator = "#main-content > div > div > div > div > h2"
  val errorSummaryMessageLocator = "#main-content > div > div > div > div > div > ul > li > a"
  val radioErrorMessageLocator = "oneProperty-error"

  val backLinkHref = "/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list"
  val cyaBackLinkHref = "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"
  val errorHref = "#oneProperty"

  "AddAgentController oneProperty display the correct content in English" which {
    lazy val document =
      getAssignToOnePropertyPage(language = English, fromCya = false, cyaPreSelectedRadioButton = None)

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

    s"has $yesRadioText and $noRadioText radio buttons that are unchecked" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioText
      document.getElementById(yesRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLabelLocator).text shouldBe noRadioText
      document.getElementById(noRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(noRadioLocator).hasAttr("checked") shouldBe false
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "AddAgentController oneProperty display the correct content in Welsh" which {
    lazy val document = getAssignToOnePropertyPage(language = Welsh, fromCya = false, cyaPreSelectedRadioButton = None)

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

    s"has $yesRadioText and $noRadioText radio buttons that are unchecked in Welsh" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioTextWelsh
      document.getElementById(yesRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLabelLocator).text shouldBe noRadioTextWelsh
      document.getElementById(noRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(noRadioLocator).hasAttr("checked") shouldBe false
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "AddAgentController oneProperty display the correct content in English when coming back from the Check Your Answers page with the 'Yes' radio preselected" which {
    lazy val document =
      getAssignToOnePropertyPage(language = English, fromCya = true, cyaPreSelectedRadioButton = Some("all"))

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
      document.getElementById(backLinkLocator).attr("href") shouldBe cyaBackLinkHref
    }

    s"has $yesRadioText and $noRadioText radio buttons where $yesRadioText is checked" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioText
      document.getElementById(yesRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(yesRadioLocator).hasAttr("checked") shouldBe true
      document.select(noRadioLabelLocator).text shouldBe noRadioText
      document.getElementById(noRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(noRadioLocator).hasAttr("checked") shouldBe false
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "AddAgentController oneProperty display the correct content in English when coming back from the Check Your Answers page with the 'No' radio preselected" which {
    lazy val document =
      getAssignToOnePropertyPage(language = English, fromCya = true, cyaPreSelectedRadioButton = Some("no"))

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
      document.getElementById(backLinkLocator).attr("href") shouldBe cyaBackLinkHref
    }

    s"has $yesRadioText and $noRadioText radio buttons where $yesRadioText is checked" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioText
      document.getElementById(yesRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLabelLocator).text shouldBe noRadioText
      document.getElementById(noRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(noRadioLocator).hasAttr("checked") shouldBe true
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "AddAgentController oneProperty display the correct content in Welsh when coming back from the Check Your Answers page with the 'Yes' radio preselected" which {
    lazy val document =
      getAssignToOnePropertyPage(language = Welsh, fromCya = true, cyaPreSelectedRadioButton = Some("all"))

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
      document.getElementById(backLinkLocator).attr("href") shouldBe cyaBackLinkHref
    }

    s"has $yesRadioText and $noRadioText radio buttons where $yesRadioText is checked in Welsh" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioTextWelsh
      document.getElementById(yesRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(yesRadioLocator).hasAttr("checked") shouldBe true
      document.select(noRadioLabelLocator).text shouldBe noRadioTextWelsh
      document.getElementById(noRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(noRadioLocator).hasAttr("checked") shouldBe false
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "AddAgentController oneProperty display the correct content in Welsh when coming back from the Check Your Answers page with the 'No' radio preselected" which {
    lazy val document =
      getAssignToOnePropertyPage(language = Welsh, fromCya = true, cyaPreSelectedRadioButton = Some("no"))

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
      document.getElementById(backLinkLocator).attr("href") shouldBe cyaBackLinkHref
    }

    s"has $yesRadioText and $noRadioText radio buttons where $yesRadioText is checked in Welsh" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioTextWelsh
      document.getElementById(yesRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLabelLocator).text shouldBe noRadioTextWelsh
      document.getElementById(noRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(noRadioLocator).hasAttr("checked") shouldBe true
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "AddAgentController submitOneProperty redirects the user to the Check Your Answers page when the 'Yes' radio button is selected" which {
    lazy val res = postAssignToOnePropertyPage(language = English, selectedRadio = Some("all"))

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(cyaBackLinkHref)
    }
  }

  "AddAgentController submitOneProperty redirects the user to the Check Your Answers page when the 'No' radio button is selected" which {
    lazy val res = postAssignToOnePropertyPage(language = Welsh, selectedRadio = Some("no"))

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(cyaBackLinkHref)
    }
  }

  "AddAgentController submitOneProperty displays an error in English when a radio button isn't selected" which {
    lazy val res = postAssignToOnePropertyPage(language = English, selectedRadio = None)
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

    s"has $yesRadioText and $noRadioText radio buttons that are unchecked" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioText
      document.getElementById(yesRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLabelLocator).text shouldBe noRadioText
      document.getElementById(noRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(noRadioLocator).hasAttr("checked") shouldBe false
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "AddAgentController submitOneProperty displays an error in Welsh when a radio button isn't selected" which {
    lazy val res = postAssignToOnePropertyPage(language = Welsh, selectedRadio = None)
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

    s"has $yesRadioText and $noRadioText radio buttons that are unchecked in Welsh" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioTextWelsh
      document.getElementById(yesRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLabelLocator).text shouldBe noRadioTextWelsh
      document.getElementById(noRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(noRadioLocator).hasAttr("checked") shouldBe false
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  private def stubAuth() = {
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
  private def getAssignToOnePropertyPage(
        language: Language,
        fromCya: Boolean,
        cyaPreSelectedRadioButton: Option[String]) = {
    stubAuth()

    if (fromCya) {
      val cacheData = ManagingProperty(
        agentCode = 1234,
        agentOrganisationName = "Test Agent",
        agentAddress = "1 Agent Street, AG3 NT1",
        isCorrectAgent = true,
        managingPropertyChoice = cyaPreSelectedRadioButton.getOrElse(""),
        singleProperty = true,
        status = ManagingPropertySelected,
        backLink = None,
        totalPropertySelectionSize = 1,
        propertySelectedSize = 1,
        appointmentScope = None,
        bothRatingLists = Some(true),
        specificRatingList = None
      )

      await(mockAppointAgentSessionRepository.saveOrUpdate(cacheData))
    } else {
      val cacheData = SelectedAgent(
        agentCode = 1234,
        agentOrganisationName = "Test Agent",
        agentAddress = "1 Agent Street, AG3 NT1",
        isCorrectAgent = true,
        backLink = None,
        bothRatingLists = Some(true),
        specificRatingList = None
      )

      await(mockAppointAgentSessionRepository.saveOrUpdate(cacheData))
    }

    val url = if (fromCya) "one-property?fromCyaChange=true" else "one-property"
    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/$url")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def postAssignToOnePropertyPage(language: Language, selectedRadio: Option[String]) = {
    stubAuth()

    val cacheData = SelectedAgent(
      agentCode = 1234,
      agentOrganisationName = "Test Agent",
      agentAddress = "1 Agent Street, AG3 NT1",
      isCorrectAgent = true,
      backLink = Some("/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list"),
      bothRatingLists = Some(true),
      specificRatingList = None
    )

    await(mockAppointAgentSessionRepository.saveOrUpdate(cacheData))

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/one-property")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .withFollowRedirects(follow = false)
        .post(Map("oneProperty" -> Seq(selectedRadio.getOrElse(""))))
    )
  }
}
