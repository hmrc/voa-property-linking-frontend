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

package controllers.propertyLinking

import base.{HtmlComponentHelpers, ISpecBase}
import binders.propertylinks.ClaimPropertyReturnToPage
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import repositories.PropertyLinkingSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class OccupancyOfPropertyISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: PropertyLinkingSessionRepository = app.injector.instanceOf[PropertyLinkingSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Do you still own or occupy the property? - Valuation Office Agency - GOV.UK"
  val titleTextAgent = "Does your client still own or occupy the property? - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val captionText = "Add a property"
  val headerText = "Do you still own or occupy the property?"
  val headerTextAgent = "Does your client still own or occupy the property?"
  val yesText = "Yes"
  val noText = "No"
  val dateOfText = "Date of your last day as owner and occupier of the property"
  val dateOfOwnerText = "Date of your last day as owner of the property"
  val dateOfOccupierText = "Date of your last day as occupier of the property"
  val dateOfTextAgent = "Date of your client’s last day as owner and occupier of the property"
  val dateOfOwnerTextAgent = "Date of your client’s last day as owner of the property"
  val dateOfOccupierTextAgent = "Date of your client’s last day as occupier of the property"
  val forExampleText = "For example, 2 4 2017"
  val dayText = "Day"
  val monthText = "Month"
  val yearText = "Year"
  val continueText = "Continue"
  val errorTitleText = "Error: Do you still own or occupy the property? - Valuation Office Agency - GOV.UK"
  val errorTitleTextAgent =
    "Error: Does your client still own or occupy the property? - Valuation Office Agency - GOV.UK"
  val thereIsAProblemText = "There is a problem"
  val ownerOccupierErrorText = "Select if you are still the owner and occupier of the property"
  val ownerErrorText = "Select if you are still the owner of the property"
  val occupierErrorText = "Select if you are still the occupier of the property"
  val ownerOccupierErrorAgentText = "Select if your client is still the owner and occupier of the property"
  val ownerErrorAgentText = "Select if your client is still the owner of the property"
  val occupierErrorAgentText = "Select if your client is still the occupier of the property"
  val futureDateOwnerOccupierErrorText =
    "Date of your last day as owner and occupier of the property must be in the past"
  val futureDateOwnerOccupierErrorAgentText =
    "Date of your client’s last day as owner and occupier of the property must be in the past"
  val futureDateOwnerErrorText = "Date of your last day as owner of the property must be in the past"
  val futureDateOwnerErrorAgentText = "Date of your client’s last day as owner of the property must be in the past"
  val futureDateOccupierErrorText = "Date of your last day as occupier of the property must be in the past"
  val futureDateOccupierErrorAgentText =
    "Date of your client’s last day as occupier of the property must be in the past"
  val enterAValidDateErrorText = "Enter a valid date"
  val earlyStartDateOwnerOccupierErrorText =
    "Date of your last day as owner and occupier of the property must be after 1 April 2017"
  val earlyStartDateOwnerErrorText = "Date of your last day as owner of the property must be after 1 April 2017"
  val earlyStartDateOccupierErrorText = "Date of your last day as occupier of the property must be after 1 April 2017"
  val earlyStartDateOwnerOccupierErrorAgentText =
    "Date of your client’s last day as owner and occupier of the property must be after 1 April 2017"
  val earlyStartDateOwnerErrorAgentText =
    "Date of your client’s last day as owner of the property must be after 1 April 2017"
  val earlyStartDateOccupierErrorAgentText =
    "Date of your client’s last day as occupier of the property must be after 1 April 2017"
  val emptyDateErrorText = "Date of your last day as occupier of the property - Enter a valid date"
  val invalidDateErrorText = "Date of your last day as occupier of the property - Enter a valid date"
  val enterValidDateErrorText = "Enter a valid date"
  val errorPrefix = "Error: "

  val titleTextWelsh = "Ydych chi dal i fod yn berchen neu’n meddiannu’r eiddo? - Valuation Office Agency - GOV.UK"
  val titleTextWelshAgent =
    "Ydy’ch cleient dal i fod yn berchen neu’n meddiannu’r eiddo? - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Ychwanegu eiddo"
  val headerTextWelsh = "Ydych chi dal i fod yn berchen neu’n meddiannu’r eiddo?"
  val headerTextWelshAgent = "Ydy’ch cleient dal i fod yn berchen neu’n meddiannu’r eiddo?"
  val yesTextWelsh = "Ie"
  val noTextWelsh = "Na"
  val forExampleTextWelsh = "Er enghraifft, 2 4 2017"
  val dateOfTextWelsh = "Dyddiad eich diwrnod olaf fel perchennog a meddiannydd yr eiddo"
  val dateOfOwnerTextWelsh = "Dyddiad eich diwrnod olaf fel perchennog yr eiddo"
  val dateOfOccupierTextWelsh = "Dyddiad eich diwrnod olaf fel meddiannydd yr eiddo"
  val dateOfTextWelshAgent = "Dyddiad diwrnod olaf eich cleient fel perchennog a meddiannydd yr eiddo"
  val dateOfOwnerTextWelshAgent = "Dyddiad diwrnod olaf eich cleient fel perchennog yr eiddo"
  val dateOfOccupierTextWelshAgent = "Dyddiad diwrnod olaf eich cleient fel meddiannydd yr eiddo"
  val dayTextWelsh = "Diwrnod"
  val monthTextWelsh = "Mis"
  val yearTextWelsh = "Blwyddyn"
  val continueTextWelsh = "Parhau"
  val errorTitleTextWelsh =
    "Gwall: Ydych chi dal i fod yn berchen neu’n meddiannu’r eiddo? - Valuation Office Agency - GOV.UK"
  val errorTitleTextWelshAgent =
    "Gwall: Ydy’ch cleient dal i fod yn berchen neu’n meddiannu’r eiddo? - Valuation Office Agency - GOV.UK"
  val thereIsAProblemTextWelsh = "Mae yna broblem"
  val ownerOccupierErrorTextWelsh = "Dewiswch os taw chi yw perchennog a meddiannydd yr eiddo o hyd"
  val ownerErrorTextWelsh = "Dewiswch os ydych dal yn berchennog yr eiddo"
  val occupierErrorTextWelsh = "Dewiswch os ydych dal i fod yn feddiannydd yr eiddo"
  val ownerOccupierErrorAgentTextWelsh = "Dewiswch os taw eich cleient yw perchennog a meddiannydd yr eiddo o hyd"
  val ownerErrorAgentTextWelsh = "Dewiswch os yw’ch cleient dal i fod yn berchennog yr eiddo"
  val occupierErrorAgentTextWelsh = "Dewiswch os yw eich cleient dal i fod yn feddiannydd yr eiddo"
  val futureDateOwnerOccupierErrorTextWelsh =
    "Rhaid i ddyddiad eich diwrnod olaf fel perchennog a meddiannydd yr eiddo fod yn y gorffennol"
  val futureDateOwnerOccupierErrorAgentTextWelsh =
    "Rhaid i ddyddiad diwrnod olaf eich cleient fel perchennog a meddiannydd yr eiddo fod yn y gorffennol"
  val futureDateOwnerErrorTextWelsh = "Rhaid i ddyddiad eich diwrnod olaf fel perchennog yr eiddo fod yn y gorffennol"
  val futureDateOwnerErrorAgentTextWelsh =
    "Rhaid i ddyddiad diwrnod olaf eich cleient fel perchennog yr eiddo fod yn y gorffennol"
  val futureDateOccupierErrorTextWelsh =
    "Rhaid i ddyddiad eich diwrnod olaf fel meddiannydd yr eiddo fod yn y gorffennol"
  val futureDateOccupierErrorAgentTextWelsh =
    "Rhaid i ddyddiad diwrnod olaf eich cleient fel meddiannydd yr eiddo fod yn y gorffennol"
  val enterAValidDateErrorTextWelsh = "Nodwch ddyddiad dilys"
  val earlyStartDateOwnerOccupierErrorTextWelsh =
    "Rhaid i ddyddiad eich diwrnod olaf fel perchennog a meddiannydd yr eiddo fod ar ôl 1 Ebrill 2017"
  val earlyStartDateOwnerErrorTextWelsh =
    "Rhaid i ddyddiad eich diwrnod olaf fel perchennog yr eiddo fod ar ôl 1 Ebrill 2017"
  val earlyStartDateOccupierErrorTextWelsh =
    "Rhaid i ddyddiad eich diwrnod olaf fel meddiannydd yr eiddo fod ar ôl 1 Ebrill 2017"
  val earlyStartDateOwnerOccupierErrorAgentTextWelsh =
    "Rhaid i ddyddiad diwrnod olaf eich cleient fel perchennog a meddiannydd yr eiddo fod ar ôl 1 Ebrill 2017"
  val earlyStartDateOwnerErrorAgentTextWelsh =
    "Rhaid i ddyddiad diwrnod olaf eich cleient fel perchennog yr eiddo fod ar ôl 1 Ebrill 2017"
  val earlyStartDateOccupierErrorAgentTextWelsh =
    "Rhaid i ddyddiad diwrnod olaf eich cleient fel meddiannydd yr eiddo fod ar ôl 1 Ebrill 2017"
  val emptyDateErrorTextWelsh = "Dyddiad eich diwrnod olaf fel meddiannydd yr eiddo - Nodwch ddyddiad dilys"
  val invalidDateErrorTextWelsh = "Dyddiad eich diwrnod olaf fel meddiannydd yr eiddo - Nodwch ddyddiad dilys"
  val enterValidDateErrorTextWelsh = "Nodwch ddyddiad dilys"
  val errorPrefixWelsh = "Gwall: "

  val backLinkSelector = "#back-link"
  val captionSelector = "#caption"
  val headerSelector = "h1.govuk-heading-l"
  val radioSelector = "div.govuk-radios > div.govuk-radios__item"
  val yesSelector = "#stillOccupied"
  val noSelector = "#stillOccupied-2"
  val dateOfSelector = "h1.govuk-fieldset__heading"
  val forExampleSelector = "#lastOccupiedDate_dates-hint"
  val dateInputsSelector = "div.govuk-date-input__item"
  val continueSelector = "#continue"
  val errorSummaryTitleSelector = "#main-content > div > div > div.govuk-error-summary > div > h2"
  val errorSummaryErrorSelector = "#main-content > div > div > div.govuk-error-summary > div > div"
  val aboveRadiosErrorSelector = "#stillOccupied-error"
  val aboveDateFieldsErrorSelector = "#lastOccupiedDate_dates-error"

  val backLinkHref = "/business-rates-property-linking/my-organisation/claim/property-links/ownership"
  val backLinkHrefFromCya = "/business-rates-property-linking/my-organisation/claim/property-links/summary"

  "OccupancyOfPropertyController show method" should {
    "Show the correct page for an English, non agent, owner and occupier not from cya" which {

      lazy val document: Document = getOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = true
      )

      s"has a title of '$titleText''" in {
        document.title() shouldBe titleText
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has a $yesText radio button that is un-checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesText
        document.select(yesSelector).hasAttr("checked") shouldBe false
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noText
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfText" in {
        document.select(dateOfSelector).text() shouldBe dateOfText
        document.select(forExampleSelector).text() shouldBe forExampleText
        document.select(dateInputsSelector).get(0).text() shouldBe dayText
        document.select(dateInputsSelector).get(1).text() shouldBe monthText
        document.select(dateInputsSelector).get(2).text() shouldBe yearText
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

    "Show the correct page for an English, non agent, owner not from cya" which {

      lazy val document: Document = getOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = false
      )

      s"has a title of '$titleText''" in {
        document.title() shouldBe titleText
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has a $yesText radio button that is un-checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesText
        document.select(yesSelector).hasAttr("checked") shouldBe false
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noText
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfOwnerText" in {
        document.select(dateOfSelector).text() shouldBe dateOfOwnerText
        document.select(forExampleSelector).text() shouldBe forExampleText
        document.select(dateInputsSelector).get(0).text() shouldBe dayText
        document.select(dateInputsSelector).get(1).text() shouldBe monthText
        document.select(dateInputsSelector).get(2).text() shouldBe yearText
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

    "Show the correct page for an English, non agent, occupier from cya" which {

      lazy val document: Document = getOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = true,
        owner = false,
        occupier = true
      )

      s"has a title of '$titleText''" in {
        document.title() shouldBe titleText
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHrefFromCya
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has a $yesText radio button that is un-checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesText
        document.select(yesSelector).hasAttr("checked") shouldBe true
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noText
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfOccupierText" in {
        document.select(dateOfSelector).text() shouldBe dateOfOccupierText
        document.select(forExampleSelector).text() shouldBe forExampleText
        document.select(dateInputsSelector).get(0).text() shouldBe dayText
        document.select(dateInputsSelector).get(1).text() shouldBe monthText
        document.select(dateInputsSelector).get(2).text() shouldBe yearText
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

    "Show the correct page for an English, agent, owner and occupier from cya" which {

      lazy val document: Document = getOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = true,
        owner = true,
        occupier = true
      )

      s"has a title of '$titleTextAgent''" in {
        document.title() shouldBe titleTextAgent
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHrefFromCya
      }

      s"has a header of '$headerTextAgent' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerTextAgent
        document.select(captionSelector).text shouldBe captionText
      }

      s"has a $yesText radio button that is un-checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesText
        document.select(yesSelector).hasAttr("checked") shouldBe true
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noText
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfTextAgent" in {
        document.select(dateOfSelector).text() shouldBe dateOfTextAgent
        document.select(forExampleSelector).text() shouldBe forExampleText
        document.select(dateInputsSelector).get(0).text() shouldBe dayText
        document.select(dateInputsSelector).get(1).text() shouldBe monthText
        document.select(dateInputsSelector).get(2).text() shouldBe yearText
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

    "Show the correct page for an English, agent, owner not from cya" which {

      lazy val document: Document = getOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = false
      )

      s"has a title of '$titleTextAgent''" in {
        document.title() shouldBe titleTextAgent
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerTextAgent' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerTextAgent
        document.select(captionSelector).text shouldBe captionText
      }

      s"has a $yesText radio button that is un-checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesText
        document.select(yesSelector).hasAttr("checked") shouldBe false
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noText
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfOwnerTextAgent" in {
        document.select(dateOfSelector).text() shouldBe dateOfOwnerTextAgent
        document.select(forExampleSelector).text() shouldBe forExampleText
        document.select(dateInputsSelector).get(0).text() shouldBe dayText
        document.select(dateInputsSelector).get(1).text() shouldBe monthText
        document.select(dateInputsSelector).get(2).text() shouldBe yearText
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

    "Show the correct page for an English, agent, occupier from cya" which {

      lazy val document: Document = getOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = true,
        owner = false,
        occupier = true
      )

      s"has a title of '$titleTextAgent''" in {
        document.title() shouldBe titleTextAgent
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHrefFromCya
      }

      s"has a header of '$headerTextAgent' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerTextAgent
        document.select(captionSelector).text shouldBe captionText
      }

      s"has a $yesText radio button that is un-checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesText
        document.select(yesSelector).hasAttr("checked") shouldBe true
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noText
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfOccupierTextAgent" in {
        document.select(dateOfSelector).text() shouldBe dateOfOccupierTextAgent
        document.select(forExampleSelector).text() shouldBe forExampleText
        document.select(dateInputsSelector).get(0).text() shouldBe dayText
        document.select(dateInputsSelector).get(1).text() shouldBe monthText
        document.select(dateInputsSelector).get(2).text() shouldBe yearText
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

    "Show the correct page for a Welsh, non agent, owner and occupier not from cya" which {

      lazy val document: Document = getOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = true
      )

      s"has a title of '$titleText''" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has a $yesText radio button that is un-checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesTextWelsh
        document.select(yesSelector).hasAttr("checked") shouldBe false
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noTextWelsh
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfText" in {
        document.select(dateOfSelector).text() shouldBe dateOfTextWelsh
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
        document.select(dateInputsSelector).get(0).text() shouldBe dayTextWelsh
        document.select(dateInputsSelector).get(1).text() shouldBe monthTextWelsh
        document.select(dateInputsSelector).get(2).text() shouldBe yearTextWelsh
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }

    }

    "Show the correct page for a Welsh, non agent, owner not from cya" which {

      lazy val document: Document = getOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = false
      )

      s"has a title of '$titleText''" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has a $yesText radio button that is un-checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesTextWelsh
        document.select(yesSelector).hasAttr("checked") shouldBe false
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noTextWelsh
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfOwnerText" in {
        document.select(dateOfSelector).text() shouldBe dateOfOwnerTextWelsh
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
        document.select(dateInputsSelector).get(0).text() shouldBe dayTextWelsh
        document.select(dateInputsSelector).get(1).text() shouldBe monthTextWelsh
        document.select(dateInputsSelector).get(2).text() shouldBe yearTextWelsh
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }

    }

    "Show the correct page for a Welsh, non agent, occupier from cya" which {

      lazy val document: Document = getOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = true,
        owner = false,
        occupier = true
      )

      s"has a title of '$titleText''" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHrefFromCya
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has a $yesText radio button that is checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesTextWelsh
        document.select(yesSelector).hasAttr("checked") shouldBe true
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noTextWelsh
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfOccupierText" in {
        document.select(dateOfSelector).text() shouldBe dateOfOccupierTextWelsh
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
        document.select(dateInputsSelector).get(0).text() shouldBe dayTextWelsh
        document.select(dateInputsSelector).get(1).text() shouldBe monthTextWelsh
        document.select(dateInputsSelector).get(2).text() shouldBe yearTextWelsh
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }

    }

    "Show the correct page for a Welsh, agent, owner and occupier from cya" which {

      lazy val document: Document =
        getOccupancyOfPropertyPage(language = Welsh, userIsAgent = true, fromCya = true, owner = true, occupier = true)

      s"has a title of '$titleTextAgent''" in {
        document.title() shouldBe titleTextWelshAgent
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHrefFromCya
      }

      s"has a header of '$headerTextAgent' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerTextWelshAgent
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has a $yesText radio button that is checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesTextWelsh
        document.select(yesSelector).hasAttr("checked") shouldBe true
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noTextWelsh
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfTextAgent" in {
        document.select(dateOfSelector).text() shouldBe dateOfTextWelshAgent
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
        document.select(dateInputsSelector).get(0).text() shouldBe dayTextWelsh
        document.select(dateInputsSelector).get(1).text() shouldBe monthTextWelsh
        document.select(dateInputsSelector).get(2).text() shouldBe yearTextWelsh
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }

    }

    "Show the correct page for a Welsh, agent, owner not from cya" which {

      lazy val document: Document = getOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = false
      )

      s"has a title of '$titleTextWelshAgent''" in {
        document.title() shouldBe titleTextWelshAgent
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerTextAgent' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerTextWelshAgent
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has a $yesText radio button that is un-checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesTextWelsh
        document.select(yesSelector).hasAttr("checked") shouldBe false
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noTextWelsh
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfOwnerTextAgent" in {
        document.select(dateOfSelector).text() shouldBe dateOfOwnerTextWelshAgent
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
        document.select(dateInputsSelector).get(0).text() shouldBe dayTextWelsh
        document.select(dateInputsSelector).get(1).text() shouldBe monthTextWelsh
        document.select(dateInputsSelector).get(2).text() shouldBe yearTextWelsh
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }

    }

    "Show the correct page for a Welsh, agent, occupier from cya" which {

      lazy val document: Document =
        getOccupancyOfPropertyPage(language = Welsh, userIsAgent = true, fromCya = true, owner = false, occupier = true)

      s"has a title of '$titleTextWelshAgent''" in {
        document.title() shouldBe titleTextWelshAgent
      }

      "has a back link" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHrefFromCya
      }

      s"has a header of '$headerTextAgent' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerTextWelshAgent
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has a $yesText radio button that is checked" in {
        document.select(radioSelector).get(0).text() shouldBe yesTextWelsh
        document.select(yesSelector).hasAttr("checked") shouldBe true
      }

      s"has a $noText radio button" in {
        document.select(radioSelector).get(1).text() shouldBe noTextWelsh
        document.select(noSelector).hasAttr("checked") shouldBe false
      }

      s"has a conditionally hidden section to input the date for $dateOfOccupierTextAgent" in {
        document.select(dateOfSelector).text() shouldBe dateOfOccupierTextWelshAgent
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
        document.select(dateInputsSelector).get(0).text() shouldBe dayTextWelsh
        document.select(dateInputsSelector).get(1).text() shouldBe monthTextWelsh
        document.select(dateInputsSelector).get(2).text() shouldBe yearTextWelsh
      }

      s"has a '$continueText' link on the screen" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }

    }

  }

  "OccupancyOfPropertyController post method" should {
    "Redirect to the choose evidence page if the user chooses yes and has not come from cya" in {
      await(
        mockRepository.saveOrUpdate(
          LinkingSession(
            address = "LS",
            uarn = 1L,
            submissionId = "PL-123456",
            personId = 1L,
            earliestStartDate = LocalDate.of(2017, 4, 1),
            propertyRelationship = Some(Owner).map(capacity => PropertyRelationship(capacity, 1L)),
            propertyOwnership = Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1))),
            propertyOccupancy = Some(PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None)),
            hasRatesBill = Some(true),
            uploadEvidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
            evidenceType = Some(RatesBillType),
            clientDetails = Some(ClientDetails(100, "ABC")),
            localAuthorityReference = "12341531531",
            rtp = ClaimPropertyReturnToPage.FMBR,
            fromCya = Some(false),
            isSubmitted = None
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

      val requestBody = Json.obj(
        "stillOccupied" -> "true"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/occupancy")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/evidence"

    }

    "Redirect to the choose evidence page if the user chooses no and has not come from cya" in {
      await(
        mockRepository.saveOrUpdate(
          LinkingSession(
            address = "LS",
            uarn = 1L,
            submissionId = "PL-123456",
            personId = 1L,
            earliestStartDate = LocalDate.of(2017, 4, 1),
            propertyRelationship = Some(Owner).map(capacity => PropertyRelationship(capacity, 1L)),
            propertyOwnership = Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1))),
            propertyOccupancy = Some(PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None)),
            hasRatesBill = Some(true),
            uploadEvidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
            evidenceType = Some(RatesBillType),
            clientDetails = Some(ClientDetails(100, "ABC")),
            localAuthorityReference = "12341531531",
            rtp = ClaimPropertyReturnToPage.FMBR,
            fromCya = Some(false),
            isSubmitted = None
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

      val requestBody = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "02",
        "lastOccupiedDate.month" -> "02",
        "lastOccupiedDate.year"  -> "2020"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/occupancy")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/evidence"

    }

    "Redirect to the choose evidence page if the user chooses yes and has come from cya" in {
      await(
        mockRepository.saveOrUpdate(
          LinkingSession(
            address = "LS",
            uarn = 1L,
            submissionId = "PL-123456",
            personId = 1L,
            earliestStartDate = LocalDate.of(2017, 4, 1),
            propertyRelationship = Some(Owner).map(capacity => PropertyRelationship(capacity, 1L)),
            propertyOwnership = Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1))),
            propertyOccupancy = Some(PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None)),
            hasRatesBill = Some(true),
            uploadEvidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
            evidenceType = Some(RatesBillType),
            clientDetails = Some(ClientDetails(100, "ABC")),
            localAuthorityReference = "12341531531",
            rtp = ClaimPropertyReturnToPage.FMBR,
            fromCya = Some(true),
            isSubmitted = None
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

      val requestBody = Json.obj(
        "stillOccupied" -> "true"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/occupancy")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/summary"

    }

    "Redirect to the choose evidence page if the user chooses no and has come from cya" in {
      await(
        mockRepository.saveOrUpdate(
          LinkingSession(
            address = "LS",
            uarn = 1L,
            submissionId = "PL-123456",
            personId = 1L,
            earliestStartDate = LocalDate.of(2017, 4, 1),
            propertyRelationship = Some(Owner).map(capacity => PropertyRelationship(capacity, 1L)),
            propertyOwnership = Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1))),
            propertyOccupancy = Some(PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None)),
            hasRatesBill = Some(true),
            uploadEvidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
            evidenceType = Some(RatesBillType),
            clientDetails = Some(ClientDetails(100, "ABC")),
            localAuthorityReference = "12341531531",
            rtp = ClaimPropertyReturnToPage.FMBR,
            fromCya = Some(true),
            isSubmitted = None
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

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "02",
        "lastOccupiedDate.month" -> "02",
        "lastOccupiedDate.year"  -> "2020"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/occupancy")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/summary"

    }

    "Return a bad request and show the error page if the user is an owner and occupier and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$ownerOccupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe ownerOccupierErrorText
      }

      s"has an error message above the radio button of $ownerOccupierErrorText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefix + ownerOccupierErrorText
      }

    }

    "Return a bad request and show the error page if the user is an owner and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$ownerErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe ownerErrorText
      }

      s"has an error message above the radio button of $ownerErrorText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefix + ownerErrorText
      }

    }

    "Return a bad request and show the error page if the user is an occupier and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$occupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe occupierErrorText
      }

      s"has an error message above the radio button of $occupierErrorText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefix + occupierErrorText
      }

    }

    "Return a bad request and show the error page if it's the agent of an owner and occupier and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextAgent
      }

      s"has an error summary that contains the correct error message '$ownerOccupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe ownerOccupierErrorAgentText
      }

      s"has an error message above the radio button of $ownerOccupierErrorAgentText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefix + ownerOccupierErrorAgentText
      }

    }

    "Return a bad request and show the error page if it's the agent of an owner and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextAgent
      }

      s"has an error summary that contains the correct error message '$ownerErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe ownerErrorAgentText
      }

      s"has an error message above the radio button of $ownerErrorAgentText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefix + ownerErrorAgentText
      }

    }

    "Return a bad request and show the error page if it's the agent of an occupier and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextAgent
      }

      s"has an error summary that contains the correct error message '$occupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe occupierErrorAgentText
      }

      s"has an error message above the radio button of $occupierErrorAgentText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefix + occupierErrorAgentText
      }

    }

    "Return a bad request and show the error page if the user is an owner and occupier and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOwnerOccupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOwnerOccupierErrorText
      }

      s"has an error message above the date fields of ${errorPrefix + earlyStartDateOwnerOccupierErrorText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + earlyStartDateOwnerOccupierErrorText
      }

    }

    "Return a bad request and show the error page if the user is an owner and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOwnerErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOwnerErrorText
      }

      s"has an error message above the date fields of ${errorPrefix + earlyStartDateOwnerErrorText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + earlyStartDateOwnerErrorText
      }

    }

    "Return a bad request and show the error page if the user is an occupier and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOccupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOccupierErrorText
      }

      s"has an error message above the date fields of ${errorPrefix + earlyStartDateOccupierErrorText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + earlyStartDateOccupierErrorText
      }

    }

    "Return a bad request and show the error page if it's the agent of an owner and occupier and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextAgent
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOwnerOccupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOwnerOccupierErrorAgentText
      }

      s"has an error message above the date fields of ${errorPrefix + earlyStartDateOwnerOccupierErrorAgentText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefix + earlyStartDateOwnerOccupierErrorAgentText
      }

    }

    "Return a bad request and show the error page if it's the agent of an owner and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextAgent
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOwnerErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOwnerErrorAgentText
      }

      s"has an error message above the date fields of ${errorPrefix + earlyStartDateOwnerErrorAgentText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + earlyStartDateOwnerErrorAgentText
      }

    }

    "Return a bad request and show the error page if it's the agent of an occupier and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextAgent
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOccupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOccupierErrorAgentText
      }

      s"has an error message above the date fields of ${errorPrefix + earlyStartDateOccupierErrorAgentText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + earlyStartDateOccupierErrorAgentText
      }

    }

    "Return a bad request and show the error page if the user is an owner and occupier and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$futureDateOwnerOccupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOwnerOccupierErrorText
      }

      s"has an error message above the date fields of ${errorPrefix + futureDateOwnerOccupierErrorText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + futureDateOwnerOccupierErrorText
      }

    }

    "Return a bad request and show the error page if the user is an owner and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$futureDateOwnerErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOwnerErrorText
      }

      s"has an error message above the date fields of ${errorPrefix + futureDateOwnerErrorText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + futureDateOwnerErrorText
      }

    }

    "Return a bad request and show the error page if the user is an occupier and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$futureDateOccupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOccupierErrorText
      }

      s"has an error message above the date fields of ${errorPrefix + futureDateOccupierErrorText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + futureDateOccupierErrorText
      }

    }

    "Return a bad request and show the error page if if it's the agent of an an owner and occupier and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextAgent
      }

      s"has an error summary that contains the correct error message '$futureDateOwnerOccupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOwnerOccupierErrorAgentText
      }

      s"has an error message above the date fields of ${errorPrefix + futureDateOwnerOccupierErrorAgentText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefix + futureDateOwnerOccupierErrorAgentText
      }

    }

    "Return a bad request and show the error page if it's the agent of an an owner and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextAgent
      }

      s"has an error summary that contains the correct error message '$futureDateOwnerErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOwnerErrorAgentText
      }

      s"has an error message above the date fields of ${errorPrefix + futureDateOwnerErrorAgentText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + futureDateOwnerErrorAgentText
      }

    }

    "Return a bad request and show the error page if it's the agent of an an occupier and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = true,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextAgent
      }

      s"has an error summary that contains the correct error message '$futureDateOccupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOccupierErrorAgentText
      }

      s"has an error message above the date fields of ${errorPrefix + futureDateOccupierErrorAgentText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + futureDateOccupierErrorAgentText
      }

    }

    "Return a bad request and show the error page if the user has chosen no, but not given a date" when {
      val requestBody: JsObject = Json.obj(
        "stillOccupied" -> "false"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$emptyDateErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe emptyDateErrorText
      }

      s"has an error message above the date field of ${errorPrefix + enterValidDateErrorText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + enterValidDateErrorText
      }

    }

    "Return a bad request and show the error page if the user has chosen no, and given an invalid date" when {
      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "a",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2022"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = English,
        userIsAgent = false,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$invalidDateErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe invalidDateErrorText
      }

      s"has an error message above the date fields of ${errorPrefix + enterValidDateErrorText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefix + enterValidDateErrorText
      }

    }

    "Return a Welsh bad request and show the error page if the user is an owner and occupier and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$ownerOccupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe ownerOccupierErrorTextWelsh
      }

      s"has an error message above the radio button of $ownerOccupierErrorText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefixWelsh + ownerOccupierErrorTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if the user is an owner and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$ownerErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe ownerErrorTextWelsh
      }

      s"has an error message above the radio button of $ownerErrorText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefixWelsh + ownerErrorTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if the user is an occupier and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$occupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe occupierErrorTextWelsh
      }

      s"has an error message above the radio button of $occupierErrorText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefixWelsh + occupierErrorTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if it's the agent of an owner and occupier and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextWelshAgent
      }

      s"has an error summary that contains the correct error message '$ownerOccupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe ownerOccupierErrorAgentTextWelsh
      }

      s"has an error message above the radio button of $ownerOccupierErrorAgentText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefixWelsh + ownerOccupierErrorAgentTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if it's the agent of an owner and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextWelshAgent
      }

      s"has an error summary that contains the correct error message '$ownerErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe ownerErrorAgentTextWelsh
      }

      s"has an error message above the radio button of $ownerErrorAgentText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefixWelsh + ownerErrorAgentTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if it's the agent of an occupier and has not chosen a radio button" when {

      val requestBody: JsObject = Json.obj()

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = true,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextWelshAgent
      }

      s"has an error summary that contains the correct error message '$occupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe occupierErrorAgentTextWelsh
      }

      s"has an error message above the radio button of $occupierErrorAgentText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe errorPrefixWelsh + occupierErrorAgentTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if the user is an owner and occupier and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOwnerOccupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOwnerOccupierErrorTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + earlyStartDateOwnerOccupierErrorText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefixWelsh + earlyStartDateOwnerOccupierErrorTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if the user is an owner and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOwnerErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOwnerErrorTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + earlyStartDateOwnerErrorText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefixWelsh + earlyStartDateOwnerErrorTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if the user is an occupier and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOccupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOccupierErrorTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + earlyStartDateOccupierErrorText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefixWelsh + earlyStartDateOccupierErrorTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if it's the agent of an owner and occupier and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextWelshAgent
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOwnerOccupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOwnerOccupierErrorAgentTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + earlyStartDateOwnerOccupierErrorAgentText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefixWelsh + earlyStartDateOwnerOccupierErrorAgentTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if it's the agent of an owner and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextWelshAgent
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOwnerErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOwnerErrorAgentTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + earlyStartDateOwnerErrorAgentText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefixWelsh + earlyStartDateOwnerErrorAgentTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if it's the agent of an occupier and has set an early date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2017"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = true,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextWelshAgent
      }

      s"has an error summary that contains the correct error message '$earlyStartDateOccupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe earlyStartDateOccupierErrorAgentTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + earlyStartDateOccupierErrorAgentText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefixWelsh + earlyStartDateOccupierErrorAgentTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if the user is an owner and occupier and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$futureDateOwnerOccupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOwnerOccupierErrorTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + futureDateOwnerOccupierErrorText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefixWelsh + futureDateOwnerOccupierErrorTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if the user is an owner and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$futureDateOwnerErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOwnerErrorTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + futureDateOwnerErrorText}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefixWelsh + futureDateOwnerErrorTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if the user is an occupier and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$futureDateOccupierErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOccupierErrorTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + futureDateOccupierErrorText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefixWelsh + futureDateOccupierErrorTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if if it's the agent of an an owner and occupier and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextWelshAgent
      }

      s"has an error summary that contains the correct error message '$futureDateOwnerOccupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOwnerOccupierErrorAgentTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + futureDateOwnerOccupierErrorAgentText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefixWelsh + futureDateOwnerOccupierErrorAgentTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if it's the agent of an an owner and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = true,
        fromCya = false,
        owner = true,
        occupier = false,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextWelshAgent
      }

      s"has an error summary that contains the correct error message '$futureDateOwnerErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOwnerErrorAgentTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + futureDateOwnerErrorAgentText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefixWelsh + futureDateOwnerErrorAgentTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if it's the agent of an an occupier and has set a future date" when {

      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "01",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2999"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = true,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleTextAgent" in {
        document.title() shouldBe errorTitleTextWelshAgent
      }

      s"has an error summary that contains the correct error message '$futureDateOccupierErrorAgentText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe futureDateOccupierErrorAgentTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + futureDateOccupierErrorAgentText}" in {
        document
          .select(aboveDateFieldsErrorSelector)
          .text() shouldBe errorPrefixWelsh + futureDateOccupierErrorAgentTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if the user has chosen no, but not given a date" when {
      val requestBody: JsObject = Json.obj(
        "stillOccupied" -> "false"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$emptyDateErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe emptyDateErrorTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + enterValidDateErrorTextWelsh}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefixWelsh + enterValidDateErrorTextWelsh
      }

    }

    "Return a Welsh bad request and show the error page if the user has chosen no, and given an invalid date" when {
      val requestBody: JsObject = Json.obj(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "a",
        "lastOccupiedDate.month" -> "01",
        "lastOccupiedDate.year"  -> "2022"
      )

      lazy val document: Document = postOccupancyOfPropertyPage(
        language = Welsh,
        userIsAgent = false,
        fromCya = false,
        owner = false,
        occupier = true,
        postBody = requestBody
      )

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$invalidDateErrorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe invalidDateErrorTextWelsh
      }

      s"has an error message above the date fields of ${errorPrefixWelsh + enterValidDateErrorTextWelsh}" in {
        document.select(aboveDateFieldsErrorSelector).text() shouldBe errorPrefixWelsh + enterValidDateErrorTextWelsh
      }

    }

  }

  private def getOccupancyOfPropertyPage(
        language: Language,
        userIsAgent: Boolean,
        fromCya: Boolean,
        owner: Boolean,
        occupier: Boolean
  ): Document = {

    val occupancyType: CapacityType =
      (owner, occupier) match {
        case (true, true)  => OwnerOccupier
        case (true, false) => Owner
        case (false, true) => Occupier
      }

    await(
      mockRepository.saveOrUpdate(
        LinkingSession(
          address = "LS",
          uarn = 1L,
          submissionId = "PL-123456",
          personId = 1L,
          earliestStartDate = LocalDate.of(2017, 4, 1),
          propertyRelationship = Some(occupancyType).map(capacity => PropertyRelationship(capacity, 1L)),
          propertyOwnership = if (fromCya) Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1))) else None,
          propertyOccupancy =
            if (fromCya) Some(PropertyOccupancy(stillOccupied = occupier, lastOccupiedDate = None)) else None,
          hasRatesBill = Some(true),
          uploadEvidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
          evidenceType = Some(RatesBillType),
          clientDetails = if (userIsAgent) Some(ClientDetails(100, "ABC")) else None,
          localAuthorityReference = "12341531531",
          rtp = ClaimPropertyReturnToPage.FMBR,
          fromCya = Some(fromCya),
          isSubmitted = None
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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/occupancy")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def postOccupancyOfPropertyPage(
        language: Language,
        userIsAgent: Boolean,
        fromCya: Boolean,
        owner: Boolean,
        occupier: Boolean,
        postBody: JsObject
  ): Document = {

    val occupancyType: CapacityType =
      (owner, occupier) match {
        case (true, true)  => OwnerOccupier
        case (true, false) => Owner
        case (false, true) => Occupier
      }

    await(
      mockRepository.saveOrUpdate(
        LinkingSession(
          address = "LS",
          uarn = 1L,
          submissionId = "PL-123456",
          personId = 1L,
          earliestStartDate = LocalDate.of(2017, 4, 1),
          propertyRelationship = Some(occupancyType).map(capacity => PropertyRelationship(capacity, 1L)),
          propertyOwnership = if (fromCya) Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1))) else None,
          propertyOccupancy =
            if (fromCya) Some(PropertyOccupancy(stillOccupied = occupier, lastOccupiedDate = None)) else None,
          hasRatesBill = Some(true),
          uploadEvidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
          evidenceType = Some(RatesBillType),
          clientDetails = if (userIsAgent) Some(ClientDetails(100, "ABC")) else None,
          localAuthorityReference = "12341531531",
          rtp = ClaimPropertyReturnToPage.FMBR,
          fromCya = Some(fromCya),
          isSubmitted = None
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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/occupancy")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = postBody)
    )

    res.status shouldBe BAD_REQUEST
    Jsoup.parse(res.body)
  }

}
