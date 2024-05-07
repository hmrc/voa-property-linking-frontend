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
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.{ClientDetails, LinkingSession, Occupier, Owner, OwnerOccupier, PropertyOccupancy, PropertyOwnership, PropertyRelationship}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import repositories.PropertyLinkingSessionRepository
import utils.ListYearsHelpers

import java.time.LocalDate

class ClaimPropertyOwnershipISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  lazy val mockPropertyLinkingSessionRepository: PropertyLinkingSessionRepository =
    app.injector.instanceOf[PropertyLinkingSessionRepository]
  val localCouncilReferenceValue = "2050466366770"
  val propertyAddress = "Test Address, Test Lane, T35 T3R"
  val dayOfOwnership: Int = 1
  val monthOfOwnership: Int = 1
  val yearOfOwnership: Int = 2020
  val occupancyEndDate: Option[PropertyOccupancy] = Some(
    PropertyOccupancy(stillOccupied = false, lastOccupiedDate = Some(LocalDate.of(2021, 1, 1))))

  val titleText = "When you became the owner or occupier of the property - Valuation Office Agency - GOV.UK"
  val titleTextAgent =
    "When your client became the owner or occupier of the property - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val localCouncilText = s"Local council reference: $localCouncilReferenceValue"
  val captionText = "Add a property"
  val headingText = "When you became the owner or occupier of the property"
  val headingTextAgent = "When your client became the owner or occupier of the property"
  val iHaveOwnedText = "I have owned or occupied the property on more than one occasion"
  val iHaveOwnedTextAgent = "My client has owned or occupied the property on more than one occasion"
  val youCanAddText =
    "You can add the property for each period you were connected to it, but you need to do it one period at a time. However, if you want to talk to the Valuation Office Agency about the valuation, you will need to select the right valuation period."
  val youCanAddTextAgent =
    "You can add the property for each period your client was connected to it, but you need to do it one period at a time. However, if you want to talk to the Valuation Office Agency about the valuation, you will need to select the right valuation period."
  val onWhatDateText = "On what date did you become the owner or occupier?"
  val onWhatDateTextAgent = "On what date did your client become the owner or occupier?"
  val forExampleText = "For example, 1 4 2017"
  val dayText = "Day"
  val monthText = "Month"
  val yearText = "Year"
  val continueText = "Continue"

  val errorText = "Error: "

  val futureDateErrorOwnerText = "Date you became owner of the property must be in the past"
  val futureDateErrorOccupierText = "Date you became occupier of the property must be in the past"
  val futureDateErrorOwnerOccupierText = "Date you became owner and occupier of the property must be in the past"
  val futureDateErrorOwnerAgentText = "Date your client became owner of the property must be in the past"
  val futureDateErrorOccupierAgentText = "Date your client became occupier of the property must be in the past"
  val futureDateErrorOwnerOccupierAgentText =
    "Date your client became owner and occupier of the property must be in the past"

  val dateBeforeErrorOwnerText = "Date you became the owner of the property must be before 1 January 2021"
  val dateBeforeErrorOwnerOccupierText =
    "Date you became the owner and occupier of the property must be before 1 January 2021"
  val dateBeforeErrorOccupierText = "Date you became the occupier of the property must be before 1 January 2021"
  val dateBeforeErrorOwnerAgentText = "Date your client became the owner of the property must be before 1 January 2021"
  val dateBeforeErrorOwnerOccupierAgentText =
    "Date your client became the owner and occupier of the property must be before 1 January 2021"
  val dateBeforeErrorOccupierAgentText =
    "Date your client became the occupier of the property must be before 1 January 2021"

  val emptyInputErrorText = "On what date did you become the owner or occupier? - Enter a valid date"
  val emptyInputAboveLabelErrorText = "Enter a valid date"
  val emptyInputErrorAgentText = "On what date did your client become the owner or occupier? - Enter a valid date"

  val errorSummaryTitleText = "There is a problem"

  val titleTextWelsh = "Pryd y daethoch yn berchennog neu’n feddiannydd yr eiddo - Valuation Office Agency - GOV.UK"
  val titleTextAgentWelsh =
    "Pryd ddaeth eich cleient yn berchennog neu’n feddiannydd yr eiddo - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val localCouncilTextWelsh = s"Cyfeirnod yr awdurdod lleol: $localCouncilReferenceValue"
  val captionTextWelsh = "Ychwanegu eiddo"
  val headingTextWelsh = "Pryd y daethoch yn berchennog neu’n feddiannydd yr eiddo"
  val headingTextAgentWelsh = "Pryd ddaeth eich cleient yn berchennog neu’n feddiannydd yr eiddo"
  val iHaveOwnedTextWelsh = "Rwyf wedi bod yn berchen ar yr eiddo neu wedi ei feddiannu ar fwy nag un achlysur"
  val iHaveOwnedTextAgentWelsh =
    "Mae fy nghleient wedi bod yn berchen ar yr eiddo neu wedi meddiannu’r eiddo ar fwy nag un achlysur"
  val youCanAddTextWelsh =
    "Gallwch ychwanegu’r eiddo ar gyfer pob cyfnod amser roeddech yn gysylltiedig ag ef, ond mae’n rhaid i chi wneud hyn un cyfnod amser ar y tro. Serch hyn, os ydych eisiau siarad gydag Asiantaeth y Swyddfa Brisio am eich prisiad, bydd rhaid i chi ddewis y cyfnod prisio cywir."
  val youCanAddTextAgentWelsh =
    "Gallwch ychwanegu’r eiddo ar gyfer pob cyfnod amser roedd eich cleient yn gysylltiedig ag ef, ond mae’n rhaid i chi wneud hyn un cyfnod amser ar y tro. Serch hyn, os ydych eisiau siarad gydag Asiantaeth y Swyddfa Brisio am eich prisiad, bydd rhaid i chi ddewis y cyfnod prisio cywir."
  val onWhatDateTextWelsh = "Ar ba ddyddiad ddaethoch chi’n berchennog neu’r meddiannydd?"
  val onWhatDateTextAgentWelsh = "Ar ba ddyddiad y daeth eich cleient yn berchennog neu’n feddiannydd?"
  val forExampleTextWelsh = "Er enghraifft, 1 4 2017"
  val dayTextWelsh = "Diwrnod"
  val monthTextWelsh = "Mis"
  val yearTextWelsh = "Blwyddyn"
  val continueTextWelsh = "Parhau"

  val errorTextWelsh = "Gwall: "

  val futureDateErrorOwnerTextWelsh = "Rhaid i’r dyddiad y daethoch yn owner yr eiddo fod yn y gorffennol"
  val futureDateErrorOccupierTextWelsh = "Rhaid i’r dyddiad y daethoch yn occupier yr eiddo fod yn y gorffennol"
  val futureDateErrorOwnerOccupierTextWelsh =
    "Rhaid i’r dyddiad y daethoch yn owner ac occupier yr eiddo fod yn y gorffennol"
  val futureDateErrorOwnerAgentTextWelsh =
    "Rhaid i’r dyddiad y daeth eich cleient yn owner yr eiddo fod yn y gorffennol"
  val futureDateErrorOccupierAgentTextWelsh =
    "Rhaid i’r dyddiad y daeth eich cleient yn occupier yr eiddo fod yn y gorffennol"
  val futureDateErrorOwnerOccupierAgentTextWelsh =
    "Rhaid i’r dyddiad y daeth eich cleient yn owner ac occupier yr eiddo fod yn y gorffennol"

  val dateBeforeErrorOwnerTextWelsh = "Rhaid i’r dyddiad y daethoch yn owner yr eiddo fod cyn 1 Ionawr 2021"
  val dateBeforeErrorOccupierTextWelsh = "Rhaid i’r dyddiad y daethoch yn occupier yr eiddo fod cyn 1 Ionawr 2021"
  val dateBeforeErrorOwnerOccupierTextWelsh =
    "Rhaid i’r dyddiad y daethoch yn owner ac occupier yr eiddo fod cyn 1 Ionawr 2021"
  val dateBeforeErrorOwnerAgentTextWelsh =
    "Rhaid i’r dyddiad y daeth eich cleient yn owner yr eiddo fod cyn 1 Ionawr 2021"
  val dateBeforeErrorOccupierAgentTextWelsh =
    "Rhaid i’r dyddiad y daeth eich cleient yn occupier yr eiddo fod cyn 1 Ionawr 2021"
  val dateBeforeErrorOwnerOccupierAgentTextWelsh =
    "Rhaid i’r dyddiad y daeth eich cleient yn owner ac occupier yr eiddo fod cyn 1 Ionawr 2021"

  val emptyInputErrorTextWelsh = "Ar ba ddyddiad ddaethoch chi’n berchennog neu’r meddiannydd? - Nodwch ddyddiad dilys"
  val emptyInputErrorAgentTextWelsh =
    "Ar ba ddyddiad y daeth eich cleient yn berchennog neu’n feddiannydd? - Nodwch ddyddiad dilys"
  val emptyInputAboveLabelErrorTextWelsh = "Nodwch ddyddiad dilys"

  val errorSummaryTitleTextWelsh = "Mae yna broblem"

  val backLinkSelector = "#back-link"
  val captionSelector = "span.govuk-caption-l"
  val headingSelector = "#main-content > div > div > h1"
  val iHaveOwnedSelector = "#multipleOwnership > summary > span"
  val youCanAddSelector = "#multipleOwnership > div"
  val onWhatDateSelector = "#interestedStartDate > div > fieldset > legend > h1"
  val forExampleSelector = "#interestedStartDate_dates-hint"
  val daySelector = "#interestedStartDate_dates > div:nth-child(1) > div > label"
  val dayValueSelector = "#interestedStartDate-day"
  val monthSelector = "#interestedStartDate_dates > div:nth-child(2) > div > label"
  val monthValueSelector = "#interestedStartDate-month"
  val yearSelector = "#interestedStartDate_dates > div:nth-child(3) > div > label"
  val yearValueSelector = "#interestedStartDate-year"
  val continueSelector = "#continue"
  val errorSummaryTitleLocator = "h2.govuk-error-summary__title"
  val errorSummaryMessageLocator = "div.govuk-error-summary > div > div > ul > li > a"
  val radioErrorMessageLocator = "interestedStartDate_dates-error"

  val backLinkHref = "/business-rates-property-linking/my-organisation/claim/property-links"
  val backLinkFromCyaHref = "/business-rates-property-linking/my-organisation/claim/property-links/summary"
  val errorHref = "#interestedStartDate-day"

  "ClaimPropertyOwnershipController showOwnership method" should {
    "Show an English when you became the owner screen for a user with the correct text when the language is set to English" which {

      lazy val document: Document = getClaimOwnershipPage(language = English, userIsAgent = false, fromCya = false)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to the claim property links page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of $headingText with a caption above of '$captionText'" in {
        document.select(headingSelector).text shouldBe headingText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has summary text on the screen of $iHaveOwnedText" in {
        document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedText
      }

      s"has inset text on the screen of $youCanAddText" in {
        document.select(youCanAddSelector).text() shouldBe youCanAddText
      }

      s"has a subheading on the screen of $onWhatDateText" in {
        document.select(onWhatDateSelector).text() shouldBe onWhatDateText
      }

      s"has text on the screen of $forExampleText" in {
        document.select(forExampleSelector).text() shouldBe forExampleText
      }

      s"has an empty input field for the $dayText" in {
        document.select(daySelector).text() shouldBe dayText
        document.select(dayValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $monthText" in {
        document.select(monthSelector).text() shouldBe monthText
        document.select(monthValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $yearText" in {
        document.select(yearSelector).text() shouldBe yearText
        document.select(yearValueSelector).attr("value") shouldBe ""
      }

      s"has a $continueText button on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

    "Show an English when you became the owner screen for an agent with the correct text when the language is set to English" which {

      lazy val document: Document = getClaimOwnershipPage(language = English, userIsAgent = true, fromCya = false)

      s"has a title of $titleTextAgent" in {
        document.title() shouldBe titleTextAgent
      }

      "has a back link which takes you to the claim property links page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of $headingTextAgent with a caption above of '$captionText'" in {
        document.select(headingSelector).text shouldBe headingTextAgent
        document.select(captionSelector).text shouldBe captionText
      }

      s"has summary text on the screen of $iHaveOwnedTextAgent" in {
        document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedTextAgent
      }

      s"has inset text on the screen of $youCanAddTextAgent" in {
        document.select(youCanAddSelector).text() shouldBe youCanAddTextAgent
      }

      s"has a subheading on the screen of $onWhatDateTextAgent" in {
        document.select(onWhatDateSelector).text() shouldBe onWhatDateTextAgent
      }

      s"has text on the screen of $forExampleText" in {
        document.select(forExampleSelector).text() shouldBe forExampleText
      }

      s"has an empty input field for the $dayText" in {
        document.select(daySelector).text() shouldBe dayText
        document.select(dayValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $monthText" in {
        document.select(monthSelector).text() shouldBe monthText
        document.select(monthValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $yearText" in {
        document.select(yearSelector).text() shouldBe yearText
        document.select(yearValueSelector).attr("value") shouldBe ""
      }

      s"has a $continueText button on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

    "Show a Welsh when you became the owner screen for a user with the correct text when the language is set to Welsh" which {

      lazy val document: Document = getClaimOwnershipPage(language = Welsh, userIsAgent = false, fromCya = false)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to the claim property links page in welsh" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of $headingText with a caption above of '$captionText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has summary text on the screen of $iHaveOwnedText in welsh" in {
        document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedTextWelsh
      }

      s"has inset text on the screen of $youCanAddText in welsh" in {
        document.select(youCanAddSelector).text() shouldBe youCanAddTextWelsh
      }

      s"has a subheading on the screen of $onWhatDateText in welsh" in {
        document.select(onWhatDateSelector).text() shouldBe onWhatDateTextWelsh
      }

      s"has text on the screen of $forExampleText in welsh" in {
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
      }

      s"has an empty input field for the $dayText in welsh" in {
        document.select(daySelector).text() shouldBe dayTextWelsh
        document.select(dayValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $monthText in welsh" in {
        document.select(monthSelector).text() shouldBe monthTextWelsh
        document.select(monthValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $yearText in welsh" in {
        document.select(yearSelector).text() shouldBe yearTextWelsh
        document.select(yearValueSelector).attr("value") shouldBe ""
      }

      s"has a $continueText button on the screen in welsh" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }

    }

    "Show a Welsh when you became the owner screen for an agent with the correct text when the language is set to Welsh" which {

      lazy val document: Document = getClaimOwnershipPage(language = Welsh, userIsAgent = true, fromCya = false)

      s"has a title of $titleTextAgent in welsh" in {
        document.title() shouldBe titleTextAgentWelsh
      }

      "has a back link which takes you to the claim property links page in welsh" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of $headingTextAgent with a caption above of '$captionText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextAgentWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has summary text on the screen of $iHaveOwnedTextAgent in welsh" in {
        document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedTextAgentWelsh
      }

      s"has inset text on the screen of $youCanAddTextAgent in welsh" in {
        document.select(youCanAddSelector).text() shouldBe youCanAddTextAgentWelsh
      }

      s"has a subheading on the screen of $onWhatDateTextAgent in welsh" in {
        document.select(onWhatDateSelector).text() shouldBe onWhatDateTextAgentWelsh
      }

      s"has text on the screen of $forExampleText in welsh" in {
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
      }

      s"has an empty input field for the $dayText in welsh" in {
        document.select(daySelector).text() shouldBe dayTextWelsh
        document.select(dayValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $monthText in welsh" in {
        document.select(monthSelector).text() shouldBe monthTextWelsh
        document.select(monthValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $yearText in welsh" in {
        document.select(yearSelector).text() shouldBe yearTextWelsh
        document.select(yearValueSelector).attr("value") shouldBe ""
      }

      s"has a $continueText button on the screen in welsh" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }

    }

    "Show a when you became the owner screen for a user with values populated if the user has come from cya" which {

      lazy val document: Document = getClaimOwnershipPage(language = English, userIsAgent = false, fromCya = true)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to the summary page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkFromCyaHref
      }

      s"has a header of $headingText with a caption above of '$captionText'" in {
        document.select(headingSelector).text shouldBe headingText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has summary text on the screen of $iHaveOwnedText" in {
        document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedText
      }

      s"has inset text on the screen of $youCanAddText" in {
        document.select(youCanAddSelector).text() shouldBe youCanAddText
      }

      s"has a subheading on the screen of $onWhatDateText" in {
        document.select(onWhatDateSelector).text() shouldBe onWhatDateText
      }

      s"has text on the screen of $forExampleText" in {
        document.select(forExampleSelector).text() shouldBe forExampleText
      }

      s"has a populated input field for the $dayText with value $dayOfOwnership" in {
        document.select(daySelector).text() shouldBe dayText
        document.select(dayValueSelector).attr("value") shouldBe s"$dayOfOwnership"
      }

      s"has a populated input field for the $monthText with value $monthOfOwnership" in {
        document.select(monthSelector).text() shouldBe monthText
        document.select(monthValueSelector).attr("value") shouldBe s"$monthOfOwnership"
      }

      s"has a populated input field for the $yearText with value $yearOfOwnership" in {
        document.select(yearSelector).text() shouldBe yearText
        document.select(yearValueSelector).attr("value") shouldBe s"$yearOfOwnership"
      }

      s"has a $continueText button on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

  }

  "ClaimPropertyOwnershipController submitOwnership method" should {
    "Redirect to the occupancy page when they enter a valid date" in {

      val res = submitOwnership(
        day = "01",
        month = "01",
        year = "2020",
        userIsAgent = true,
        fromCya = false,
        relationship = "owner")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/occupancy"

    }

    "Redirect to the summary page when they enter a valid date and came from cya" in {

      val res = submitOwnership(day = "01", month = "01", year = "2020", userIsAgent = true, relationship = "owner")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/summary"

    }

    "Return a bad request with errors for a user when no data is entered for the day/month/year" which {

      lazy val res = submitOwnership(day = "", month = "", year = "", relationship = "owner")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe emptyInputErrorText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + emptyInputAboveLabelErrorText
      }

      genericUserPageTests("", "", "", "owner")

    }

    "Return a bad request with errors for an agent when no data is entered for the day/month/year" which {

      lazy val res = submitOwnership(day = "", month = "", year = "", relationship = "owner", userIsAgent = true)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleTextAgent}" in {
        document.title() shouldBe errorText + titleTextAgent
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe emptyInputErrorAgentText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + emptyInputAboveLabelErrorText
      }

      genericAgentPageTests("", "", "", "owner")

    }

    "Return a bad request with errors for a user who's an owner when the date is in the future" which {

      lazy val res = submitOwnership(day = "01", month = "01", year = "2999", relationship = "owner")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOwnerText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + futureDateErrorOwnerText
      }

      genericUserPageTests("01", "01", "2999", "owner")
    }

    "Return a bad request with errors for a user who's an occupier when the date is in the future" which {

      lazy val res = submitOwnership(day = "01", month = "01", year = "2999", relationship = "occupier")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOccupierText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + futureDateErrorOccupierText
      }

      genericUserPageTests("01", "01", "2999", "occupier")

    }

    "Return a bad request with errors for a user who's an owner and occupier when the date is in the future" which {

      lazy val res = submitOwnership(day = "01", month = "01", year = "2999", relationship = "both")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOwnerOccupierText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + futureDateErrorOwnerOccupierText
      }

      genericUserPageTests("01", "01", "2999", "both")

    }

    "Return a bad request with errors for an agent of an owner when the date is in the future" which {

      lazy val res =
        submitOwnership(day = "01", month = "01", year = "2999", userIsAgent = true, relationship = "owner")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleTextAgent}" in {
        document.title() shouldBe errorText + titleTextAgent
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOwnerAgentText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + futureDateErrorOwnerAgentText
      }

      genericAgentPageTests("01", "01", "2999", "owner")

    }

    "Return a bad request with errors for an agent of an occupier when the date is in the future" which {

      lazy val res =
        submitOwnership(day = "01", month = "01", year = "2999", userIsAgent = true, relationship = "occupier")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleTextAgent}" in {
        document.title() shouldBe errorText + titleTextAgent
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOccupierAgentText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + futureDateErrorOccupierAgentText
      }

      genericAgentPageTests("01", "01", "2999", "occupier")

    }

    "Return a bad request with errors for an agent of an owner and occupier when the date is in the future" which {

      lazy val res = submitOwnership(day = "01", month = "01", year = "2999", userIsAgent = true, relationship = "both")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleTextAgent}" in {
        document.title() shouldBe errorText + titleTextAgent
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOwnerOccupierAgentText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorText + futureDateErrorOwnerOccupierAgentText
      }

      genericAgentPageTests("01", "01", "2999", "both")

    }

    "Return a bad request with errors for a user who's an owner when the date is after the end date" which {

      lazy val res =
        submitOwnership(day = "01", month = "02", year = "2021", relationship = "owner", endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOwnerText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + dateBeforeErrorOwnerText
      }

      genericUserPageTests("01", "02", "2021", "owner", endDate = occupancyEndDate)

    }

    "Return a bad request with errors for a user who's an occupier when the date is after the end date" which {

      lazy val res =
        submitOwnership(day = "01", month = "02", year = "2021", relationship = "occupier", endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOccupierText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + dateBeforeErrorOccupierText
      }

      genericUserPageTests("01", "02", "2021", "occupier", endDate = occupancyEndDate)

    }

    "Return a bad request with errors for a user who's an owner and occupier when the date is after the end date" which {

      lazy val res =
        submitOwnership(day = "01", month = "02", year = "2021", relationship = "both", endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOwnerOccupierText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + dateBeforeErrorOwnerOccupierText
      }

      genericUserPageTests("01", "02", "2021", "both", endDate = occupancyEndDate)

    }

    "Return a bad request with errors for an agent of an owner when the date is after the end date" which {

      lazy val res = submitOwnership(
        day = "01",
        month = "02",
        year = "2021",
        userIsAgent = true,
        relationship = "owner",
        endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleTextAgent}" in {
        document.title() shouldBe errorText + titleTextAgent
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOwnerAgentText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + dateBeforeErrorOwnerAgentText
      }

      genericAgentPageTests("01", "02", "2021", "owner", endDate = occupancyEndDate)

    }

    "Return a bad request with errors for an agent of an occupier when the date is after the end date" which {

      lazy val res = submitOwnership(
        day = "01",
        month = "02",
        year = "2021",
        userIsAgent = true,
        relationship = "occupier",
        endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleTextAgent}" in {
        document.title() shouldBe errorText + titleTextAgent
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOccupierAgentText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorText + dateBeforeErrorOccupierAgentText
      }

      genericAgentPageTests("01", "02", "2021", "occupier", endDate = occupancyEndDate)

    }

    "Return a bad request with errors for an agent of an owner and occupier when the date is after the end date" which {

      lazy val res = submitOwnership(
        day = "01",
        month = "02",
        year = "2021",
        userIsAgent = true,
        relationship = "both",
        endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleTextAgent}" in {
        document.title() shouldBe errorText + titleTextAgent
      }

      s"has an error summary with correct error messages" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOwnerOccupierAgentText
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorText + dateBeforeErrorOwnerOccupierAgentText
      }

      genericAgentPageTests("01", "02", "2021", "both", endDate = occupancyEndDate)

    }

    "Return a bad request with errors for a user when no data is entered for the day/month/year in welsh" which {

      lazy val res = submitOwnership(language = Welsh, day = "", month = "", year = "", relationship = "owner")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe emptyInputErrorTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + emptyInputAboveLabelErrorTextWelsh
      }

      genericUserPageTestsInWelsh("", "", "", "owner")

    }

    "Return a bad request with errors for an agent when no data is entered for the day/month/year in welsh" which {

      lazy val res =
        submitOwnership(language = Welsh, day = "", month = "", year = "", relationship = "owner", userIsAgent = true)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleTextAgent} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextAgentWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe emptyInputErrorAgentTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + emptyInputAboveLabelErrorTextWelsh
      }

      genericAgentPageTestsInWelsh("", "", "", "owner")

    }

    "Return a bad request with errors for a user who's an owner when the date is in the future in welsh" which {

      lazy val res = submitOwnership(language = Welsh, day = "01", month = "01", year = "2999", relationship = "owner")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOwnerTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorTextWelsh + futureDateErrorOwnerTextWelsh
      }

      genericUserPageTestsInWelsh("01", "01", "2999", "owner")
    }

    "Return a bad request with errors for a user who's an occupier when the date is in the future in welsh" which {

      lazy val res =
        submitOwnership(language = Welsh, day = "01", month = "01", year = "2999", relationship = "occupier")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOccupierTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + futureDateErrorOccupierTextWelsh
      }

      genericUserPageTestsInWelsh("01", "01", "2999", "occupier")

    }

    "Return a bad request with errors for a user who's an owner and occupier when the date is in the future in welsh" which {

      lazy val res = submitOwnership(language = Welsh, day = "01", month = "01", year = "2999", relationship = "both")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOwnerOccupierTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + futureDateErrorOwnerOccupierTextWelsh
      }

      genericUserPageTestsInWelsh("01", "01", "2999", "both")

    }

    "Return a bad request with errors for an agent of an owner when the date is in the future in welsh" which {

      lazy val res = submitOwnership(
        language = Welsh,
        day = "01",
        month = "01",
        year = "2999",
        userIsAgent = true,
        relationship = "owner")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorTextWelsh + titleTextAgent} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextAgentWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOwnerAgentTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + futureDateErrorOwnerAgentTextWelsh
      }

      genericAgentPageTestsInWelsh("01", "01", "2999", "owner")

    }

    "Return a bad request with errors for an agent of an occupier when the date is in the future in welsh" which {

      lazy val res = submitOwnership(
        language = Welsh,
        day = "01",
        month = "01",
        year = "2999",
        userIsAgent = true,
        relationship = "occupier")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorTextWelsh + titleTextAgent} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextAgentWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOccupierAgentTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + futureDateErrorOccupierAgentTextWelsh
      }

      genericAgentPageTestsInWelsh("01", "01", "2999", "occupier")

    }

    "Return a bad request with errors for an agent of an owner and occupier when the date is in the future in welsh" which {

      lazy val res = submitOwnership(
        language = Welsh,
        day = "01",
        month = "01",
        year = "2999",
        userIsAgent = true,
        relationship = "both")

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorTextWelsh + titleTextAgent} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextAgentWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe futureDateErrorOwnerOccupierAgentTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + futureDateErrorOwnerOccupierAgentTextWelsh
      }

      genericAgentPageTestsInWelsh("01", "01", "2999", "both")

    }

    "Return a bad request with errors for a user who's an owner when the date is after the end date in welsh" which {

      lazy val res = submitOwnership(
        language = Welsh,
        day = "01",
        month = "02",
        year = "2021",
        relationship = "owner",
        endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOwnerTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document.getElementById(radioErrorMessageLocator).text shouldBe errorTextWelsh + dateBeforeErrorOwnerTextWelsh
      }

      genericUserPageTestsInWelsh("01", "02", "2021", "owner", endDate = occupancyEndDate)

    }

    "Return a bad request with errors for a user who's an occupier when the date is after the end date in welsh" which {

      lazy val res = submitOwnership(
        language = Welsh,
        day = "01",
        month = "02",
        year = "2021",
        relationship = "occupier",
        endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOccupierTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + dateBeforeErrorOccupierTextWelsh
      }

      genericUserPageTestsInWelsh("01", "02", "2021", "occupier", endDate = occupancyEndDate)

    }

    "Return a bad request with errors for a user who's an owner and occupier when the date is after the end date in welsh" which {

      lazy val res = submitOwnership(
        language = Welsh,
        day = "01",
        month = "02",
        year = "2021",
        relationship = "both",
        endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOwnerOccupierTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + dateBeforeErrorOwnerOccupierTextWelsh
      }

      genericUserPageTestsInWelsh("01", "02", "2021", "both", endDate = occupancyEndDate)

    }

    "Return a bad request with errors for an agent of an owner when the date is after the end date in welsh" which {

      lazy val res = submitOwnership(
        language = Welsh,
        day = "01",
        month = "02",
        year = "2021",
        userIsAgent = true,
        relationship = "owner",
        endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleTextAgent} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextAgentWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOwnerAgentTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + dateBeforeErrorOwnerAgentTextWelsh
      }

      genericAgentPageTestsInWelsh("01", "02", "2021", "owner", endDate = occupancyEndDate)

    }

    "Return a bad request with errors for an agent of an occupier when the date is after the end date in welsh" which {

      lazy val res = submitOwnership(
        language = Welsh,
        day = "01",
        month = "02",
        year = "2021",
        userIsAgent = true,
        relationship = "occupier",
        endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleTextAgent} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextAgentWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOccupierAgentTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + dateBeforeErrorOccupierAgentTextWelsh
      }

      genericAgentPageTestsInWelsh("01", "02", "2021", "occupier", endDate = occupancyEndDate)

    }

    "Return a bad request with errors for an agent of an owner and occupier when the date is after the end date in welsh" which {

      lazy val res = submitOwnership(
        language = Welsh,
        day = "01",
        month = "02",
        year = "2021",
        userIsAgent = true,
        relationship = "both",
        endDate = occupancyEndDate)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleTextAgent} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextAgentWelsh
      }

      s"has an error summary with correct error messages in welsh" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
        document.select(errorSummaryMessageLocator).text shouldBe dateBeforeErrorOwnerOccupierAgentTextWelsh
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
        document
          .getElementById(radioErrorMessageLocator)
          .text shouldBe errorTextWelsh + dateBeforeErrorOwnerOccupierAgentTextWelsh
      }

      genericAgentPageTestsInWelsh("01", "02", "2021", "both", endDate = occupancyEndDate)

    }

  }

  private def getClaimOwnershipPage(language: Language, userIsAgent: Boolean, fromCya: Boolean): Document = {

    stubsSetup(userIsAgent, fromCya)

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/ownership")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def stubsSetup(
        userIsAgent: Boolean,
        fromCya: Boolean,
        relationshipChoice: String = "owner",
        propertyOccupancy: Option[PropertyOccupancy] = None): StubMapping = {

    val relationship: PropertyRelationship = relationshipChoice match {
      case "owner"    => PropertyRelationship(capacity = Owner, uarn = 1)
      case "occupier" => PropertyRelationship(capacity = Occupier, uarn = 1)
      case "both"     => PropertyRelationship(capacity = OwnerOccupier, uarn = 1)
    }

    await(
      mockPropertyLinkingSessionRepository.saveOrUpdate(LinkingSession(
        address = propertyAddress,
        uarn = 1L,
        submissionId = "PL-123456",
        personId = 1L,
        earliestStartDate = LocalDate.of(2017, 4, 1),
        propertyRelationship = Some(relationship),
        propertyOwnership =
          if (fromCya) Some(PropertyOwnership(LocalDate.of(yearOfOwnership, monthOfOwnership, dayOfOwnership)))
          else None,
        propertyOccupancy = propertyOccupancy,
        hasRatesBill = None,
        clientDetails = if (userIsAgent) Some(ClientDetails(123, "Client Name")) else None,
        localAuthorityReference = localCouncilReferenceValue,
        rtp = ClaimPropertyReturnToPage.FMBR,
        fromCya = Some(fromCya),
        isSubmitted = None
      )))

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

  private def submitOwnership(
        language: Language = English,
        day: String,
        month: String,
        year: String,
        userIsAgent: Boolean = false,
        fromCya: Boolean = true,
        relationship: String,
        endDate: Option[PropertyOccupancy] = None): WSResponse = {

    stubsSetup(
      userIsAgent = userIsAgent,
      fromCya = fromCya,
      relationshipChoice = relationship,
      propertyOccupancy = endDate)

    val requestBody = Json.obj(
      "interestedStartDate.day"   -> day,
      "interestedStartDate.month" -> month,
      "interestedStartDate.year"  -> year
    )

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/ownership")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )
  }

  private def genericUserPageTests(
        day: String,
        month: String,
        year: String,
        relationshipChoice: String,
        endDate: Option[PropertyOccupancy] = None): Unit = {

    lazy val res =
      submitOwnership(day = day, month = month, year = year, relationship = relationshipChoice, endDate = endDate)

    lazy val document = Jsoup.parse(res.body)

    "has a back link which takes you to the claim property links page" in {
      document.select(backLinkSelector).text() shouldBe backLinkText
      document.select(backLinkSelector).attr("href") shouldBe backLinkFromCyaHref
    }

    s"has a header of $headingText with a caption above of '$captionText'" in {
      document.select(headingSelector).text shouldBe headingText
      document.select(captionSelector).text shouldBe captionText
    }

    s"has summary text on the screen of $iHaveOwnedText" in {
      document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedText
    }

    s"has inset text on the screen of $youCanAddText" in {
      document.select(youCanAddSelector).text() shouldBe youCanAddText
    }

    s"has a subheading on the screen of $onWhatDateText" in {
      document.select(onWhatDateSelector).text() shouldBe onWhatDateText
    }

    s"has text on the screen of $forExampleText" in {
      document.select(forExampleSelector).text() shouldBe forExampleText
    }

    s"has an empty input field for the $dayText" in {
      document.select(daySelector).text() shouldBe dayText
      document.select(dayValueSelector).attr("value") shouldBe day
    }

    s"has an empty input field for the $monthText" in {
      document.select(monthSelector).text() shouldBe monthText
      document.select(monthValueSelector).attr("value") shouldBe month
    }

    s"has an empty input field for the $yearText" in {
      document.select(yearSelector).text() shouldBe yearText
      document.select(yearValueSelector).attr("value") shouldBe year
    }

    s"has a $continueText button on the screen" in {
      document.select(continueSelector).text() shouldBe continueText
    }
  }

  private def genericAgentPageTests(
        day: String,
        month: String,
        year: String,
        relationshipChoice: String,
        endDate: Option[PropertyOccupancy] = None): Unit = {

    lazy val res = submitOwnership(
      day = day,
      month = month,
      year = year,
      userIsAgent = true,
      relationship = relationshipChoice,
      endDate = endDate)

    lazy val document = Jsoup.parse(res.body)

    "has a back link which takes you to the claim property links page" in {
      document.select(backLinkSelector).text() shouldBe backLinkText
      document.select(backLinkSelector).attr("href") shouldBe backLinkFromCyaHref
    }

    s"has a header of $headingTextAgent with a caption above of '$captionText'" in {
      document.select(headingSelector).text shouldBe headingTextAgent
      document.select(captionSelector).text shouldBe captionText
    }

    s"has summary text on the screen of $iHaveOwnedTextAgent" in {
      document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedTextAgent
    }

    s"has inset text on the screen of $youCanAddTextAgent" in {
      document.select(youCanAddSelector).text() shouldBe youCanAddTextAgent
    }

    s"has a subheading on the screen of $onWhatDateTextAgent" in {
      document.select(onWhatDateSelector).text() shouldBe onWhatDateTextAgent
    }

    s"has text on the screen of $forExampleText" in {
      document.select(forExampleSelector).text() shouldBe forExampleText
    }

    s"has a populated input field for the $dayText" in {
      document.select(daySelector).text() shouldBe dayText
      document.select(dayValueSelector).attr("value") shouldBe day
    }

    s"has a populated input field for the $monthText" in {
      document.select(monthSelector).text() shouldBe monthText
      document.select(monthValueSelector).attr("value") shouldBe month
    }

    s"has a populated input field for the $yearText" in {
      document.select(yearSelector).text() shouldBe yearText
      document.select(yearValueSelector).attr("value") shouldBe year
    }

    s"has a $continueText button on the screen" in {
      document.select(continueSelector).text() shouldBe continueText
    }
  }

  private def genericUserPageTestsInWelsh(
        day: String,
        month: String,
        year: String,
        relationshipChoice: String,
        endDate: Option[PropertyOccupancy] = None): Unit = {

    lazy val res = submitOwnership(
      language = Welsh,
      day = day,
      month = month,
      year = year,
      relationship = relationshipChoice,
      endDate = endDate)

    lazy val document = Jsoup.parse(res.body)

    "has a back link which takes you to the claim property links page in welsh" in {
      document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
      document.select(backLinkSelector).attr("href") shouldBe backLinkFromCyaHref
    }

    s"has a header of $headingText with a caption above of '$captionText' in welsh" in {
      document.select(headingSelector).text shouldBe headingTextWelsh
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has summary text on the screen of $iHaveOwnedText in welsh" in {
      document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedTextWelsh
    }

    s"has inset text on the screen of $youCanAddText in welsh" in {
      document.select(youCanAddSelector).text() shouldBe youCanAddTextWelsh
    }

    s"has a subheading on the screen of $onWhatDateText in welsh" in {
      document.select(onWhatDateSelector).text() shouldBe onWhatDateTextWelsh
    }

    s"has text on the screen of $forExampleText in welsh" in {
      document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
    }

    s"has a populated input field for the $dayText in welsh" in {
      document.select(daySelector).text() shouldBe dayTextWelsh
      document.select(dayValueSelector).attr("value") shouldBe day
    }

    s"has a populated input field for the $monthText in welsh" in {
      document.select(monthSelector).text() shouldBe monthTextWelsh
      document.select(monthValueSelector).attr("value") shouldBe month
    }

    s"has a populated input field for the $yearText in welsh" in {
      document.select(yearSelector).text() shouldBe yearTextWelsh
      document.select(yearValueSelector).attr("value") shouldBe year
    }

    s"has a $continueText button on the screen in welsh" in {
      document.select(continueSelector).text() shouldBe continueTextWelsh
    }
  }

  private def genericAgentPageTestsInWelsh(
        day: String,
        month: String,
        year: String,
        relationshipChoice: String,
        endDate: Option[PropertyOccupancy] = None): Unit = {

    lazy val res = submitOwnership(
      language = Welsh,
      day = day,
      month = month,
      year = year,
      userIsAgent = true,
      relationship = relationshipChoice,
      endDate = endDate)

    lazy val document = Jsoup.parse(res.body)

    "has a back link which takes you to the claim property links page in welsh" in {
      document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
      document.select(backLinkSelector).attr("href") shouldBe backLinkFromCyaHref
    }

    s"has a header of $headingTextAgent with a caption above of '$captionText' in welsh" in {
      document.select(headingSelector).text shouldBe headingTextAgentWelsh
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has summary text on the screen of $iHaveOwnedTextAgent in welsh" in {
      document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedTextAgentWelsh
    }

    s"has inset text on the screen of $youCanAddTextAgent in welsh" in {
      document.select(youCanAddSelector).text() shouldBe youCanAddTextAgentWelsh
    }

    s"has a subheading on the screen of $onWhatDateTextAgent in welsh" in {
      document.select(onWhatDateSelector).text() shouldBe onWhatDateTextAgentWelsh
    }

    s"has text on the screen of $forExampleText in welsh" in {
      document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
    }

    s"has a populated input field for the $dayText in welsh" in {
      document.select(daySelector).text() shouldBe dayTextWelsh
      document.select(dayValueSelector).attr("value") shouldBe day
    }

    s"has a populated  input field for the $monthText in welsh" in {
      document.select(monthSelector).text() shouldBe monthTextWelsh
      document.select(monthValueSelector).attr("value") shouldBe month
    }

    s"has a populated  input field for the $yearText in welsh" in {
      document.select(yearSelector).text() shouldBe yearTextWelsh
      document.select(yearValueSelector).attr("value") shouldBe year
    }

    s"has a $continueText button on the screen in welsh" in {
      document.select(continueSelector).text() shouldBe continueTextWelsh
    }
  }

}
