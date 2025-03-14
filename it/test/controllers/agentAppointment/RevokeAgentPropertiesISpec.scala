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
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.RevokeAgentPropertiesSessionRepository
import services.AgentRelationshipService
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class RevokeAgentPropertiesISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  val agentName = "gg-ext-id"
  val agentCode = "1383"

  lazy val mockAppointRevokeService: AgentRelationshipService = app.injector.instanceOf[AgentRelationshipService]
  lazy val mockRevokeAgentPropertiesSessionRepository: RevokeAgentPropertiesSessionRepository =
    app.injector.instanceOf[RevokeAgentPropertiesSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val backLinkSelector = "#back-link"
  val headingSelector = "h1"
  val forThePropertiesSelector = "#main-content > div > div > div:nth-child(1) > div > p"
  val bulletPointSelector = "#main-content > div > div > div:nth-child(1) > div > ul > li"
  val unassigningAnAgentSelector = "#warning-text > strong"
  val searchYourSelector = "#main-content > div > div > form > h2"
  val addressSelector = "#main-content > div > div > form > div > div.govuk-form-group > label"
  val addressInputSelector = "#address"
  val searchSelector = "#search-button"
  val clearSearchSelector = "#clear-search"
  val selectAllSelector = "#par-select-all-top"
  val addressHeaderSelector = "#sort-by-address > a"
  val appointedAgentsSelector = "#agentPropertiesTableBody > thead > tr > th:nth-child(2)"
  val checkboxSelector = "#checkbox-1"
  val addressValueSelector = "#agentPropertiesTableBody > tbody > tr:nth-child(1) > td:nth-child(1) > div > label"
  val appointedAgentsValueSelector = "#agentPropertiesTableBody > tbody > tr:nth-child(1) > td:nth-child(2)"
  val secondCheckboxSelector = "#checkbox-2"
  val secondAddressValueSelector = "#agentPropertiesTableBody > tbody > tr:nth-child(2) > td:nth-child(1) > div > label"
  val secondAppointedAgentsValueSelector = "#agentPropertiesTableBody > tbody > tr:nth-child(2) > td:nth-child(2)"
  val confirmAndUnassignSelector = "#submit-button"
  val cancelSelector = "#cancel-link"
  val noPropertiesSelector = "#main-content > div > div > div.govuk-grid-row.govuk-grid-column-full > p"
  val errorAtSummarySelector = "#main-content > div > div > div.govuk-error-summary > div > div > ul > li > a"
  val noAddressChoiceAtLabelSelector =
    "#main-content > div > div > div.govuk-grid-row.govuk-grid-column-full > form > div.govuk-grid-row.govuk-form-group--error.govuk-grid-column-full > span"
  val emptyAddressErrorLabelSelector = "#main-content > div > div > form > div > span.govuk-error-message"

  val titleText =
    s"Which of your properties do you want to unassign $agentName from? - Valuation Office Agency - GOV.UK"
  val errorTitleText =
    s"Error: Which of your properties do you want to unassign $agentName from? - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val headingText = s"Which of your properties do you want to unassign $agentName from?"
  val forThePropertiesText = "For the properties you select, the agent will not be able to:"
  val sendOrContinueText = "send or continue Check and Challenge cases"
  val seeNewText = "see new Check and Challenge case correspondence, such as messages and emails"
  val seeDetailedText = "see detailed property information"
  val unassigningAnAgentText =
    "Warning Unassigning an agent that has Check and Challenge cases in progress means they will no longer be able to act on them for you."
  val searchYourText = "Search your properties"
  val addressText = "Address"
  val searchText = "Search"
  val clearSearchText = "Clear search"
  val selectAllText = "Select all"
  val appointedAgentsText = "Appointed agents"
  val confirmAndUnassignText = "Confirm and unassign"
  val cancelText = "Cancel"
  val appointText = "Appoint"
  val noPropertiesText = "There are no properties to display."
  val noAddressChoiceText = "Select which properties you want to unassign this agent from"
  val emptyAddressErrorText = "You must enter something to search for"

  val titleTextWelsh = s"O ba eiddo ydych chi am ddadneilltuo $agentName? - Valuation Office Agency - GOV.UK"
  val errorTitleTextWelsh =
    s"Gwall: O ba eiddo ydych chi am ddadneilltuo $agentName? - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val headingTextWelsh = s"O ba eiddo ydych chi am ddadneilltuo $agentName?"
  val forThePropertiesTextWelsh = "Ar gyfer yr eiddo a ddewiswch, ni fydd yr asiant yn gallu:"
  val sendOrContinueTextWelsh = "anfon neu barhau ag achosion Gwirio a Herio"
  val seeNewTextWelsh = "gweld gohebiaeth achos Gwirio a Herio newydd, er enghraifft negeseuon ac e-byst"
  val seeDetailedTextWelsh = "gweld gwybodaeth eiddo fanwl"
  val unassigningAnAgentTextWelsh =
    "Rhybudd Mae dadneilltuo asiant sydd ag achosion Gwirio a Herio ar y gweill yn golygu na fydd yn gallu gweithredu arnynt ar eich rhan mwyach."
  val searchYourTextWelsh = "Chwiliwch eich eiddo"
  val addressTextWelsh = "Cyfeiriad"
  val searchTextWelsh = "Chwilio"
  val clearSearchTextWelsh = "Clirio’r chwiliad"
  val selectAllTextWelsh = "Dewiswch popeth"
  val appointedAgentsTextWelsh = "Asiantiaid penodedig"
  val confirmAndUnassignTextWelsh = "Cadarnhau a dadneilltuo"
  val cancelTextWelsh = "Canslo"
  val appointTextWelsh = "Penodi"
  val noPropertiesTextWelsh = "Nid oes eiddo i’w harddangos."
  val noAddressChoiceTextWelsh = "Dewiswch ba eiddo rydych chi am ddadaseinio’r asiant hwn oddi wrthynt"
  val emptyAddressErrorTextWelsh = "Rhaid i chi nodi rhywbeth i chwilio amdano"
  val emptyAddressErrorAboveLabelTextWelsh =
    "Mae’n rhaid i chi nodi rhywbeth i chwilio amdano" // Should be the same as the above but for some reason its not

  val backLinkHref = "/business-rates-property-linking/my-organisation/manage-agent"
  val clearSearchHref =
    s"/business-rates-property-linking/my-organisation/revoke/properties?page=1&pageSize=15&agentCode=$agentCode"
  val selectAllHref = "#"
  val cancelHref = s"/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=$agentCode"

  "revokeAgentProperties method displays the correct content in English when theres multiple properties" which {
    lazy val document: Document = getRevokeAgentPropertiesPage(English, multipleProps = Some(true))

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a $backLinkText link" in {
      document.select(backLinkSelector).text shouldBe backLinkText
      document.select(backLinkSelector).attr("href") shouldBe backLinkHref
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingSelector).text shouldBe headingText
    }

    s"has text on the screen of $forThePropertiesText" in {
      document.select(forThePropertiesSelector).text shouldBe forThePropertiesText
    }

    s"has bullet point list on the screen with text $sendOrContinueText, $seeNewText and $seeDetailedText" in {
      document.select(bulletPointSelector).get(0).text shouldBe sendOrContinueText
      document.select(bulletPointSelector).get(1).text shouldBe seeNewText
      document.select(bulletPointSelector).get(2).text shouldBe seeDetailedText
    }

    s"has a warning on the screen of $unassigningAnAgentText" in {
      document.select(unassigningAnAgentSelector).text shouldBe unassigningAnAgentText
    }

    s"has a subheading of $searchYourText" in {
      document.select(searchYourSelector).text shouldBe searchYourText
    }

    s"has a text input field for an $addressText" in {
      document.select(addressSelector).text shouldBe addressText
      document.select(addressInputSelector).attr("type") shouldBe "text"
    }

    s"has a $searchText button" in {
      document.select(searchSelector).text shouldBe searchText
    }

    s"has a $clearSearchText link" in {
      document.select(clearSearchSelector).text shouldBe clearSearchText
      document.select(clearSearchSelector).attr("href") shouldBe clearSearchHref
    }

    s"has a $selectAllText link" in {
      document.select(selectAllSelector).text shouldBe selectAllText
      document.select(selectAllSelector).attr("href") shouldBe selectAllHref
    }

    s"has an $addressText column header" in {
      document.select(addressHeaderSelector).text shouldBe addressText
    }

    s"has an $appointedAgentsText column header" in {
      document.select(appointedAgentsSelector).text shouldBe appointedAgentsText + " " + appointedAgentsText
    }

    s"has a row that contains a checkbox, $addressText and $appointedAgentsText" in {
      document.select(checkboxSelector).attr("type") shouldBe "checkbox"
      document.select(addressValueSelector).text shouldBe appointText + " TEST ADDRESS"
      document.select(appointedAgentsValueSelector).text shouldBe ""
    }

    s"have a second row that contains a checkbox, $addressText and $appointedAgentsText" in {
      document.select(secondCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(secondAddressValueSelector).text shouldBe appointText + " ADDRESSTEST 2"
      document.select(secondAppointedAgentsValueSelector).text shouldBe "Test Agent 2"
    }

    s"has a $confirmAndUnassignText button" in {
      document.select(confirmAndUnassignSelector).text shouldBe confirmAndUnassignText
    }

    s"has a $cancelText link" in {
      document.select(cancelSelector).text shouldBe cancelText
      document.select(cancelSelector).attr("href") shouldBe cancelHref
    }
  }

  "revokeAgentProperties method displays the correct content in English when theres one property" which {
    lazy val document: Document = getRevokeAgentPropertiesPage(English, multipleProps = Some(false))

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a $backLinkText link" in {
      document.select(backLinkSelector).text shouldBe backLinkText
      document.select(backLinkSelector).attr("href") shouldBe backLinkHref
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingSelector).text shouldBe headingText
    }

    s"has text on the screen of $forThePropertiesText" in {
      document.select(forThePropertiesSelector).text shouldBe forThePropertiesText
    }

    s"has bullet point list on the screen with text $sendOrContinueText, $seeNewText and $seeDetailedText" in {
      document.select(bulletPointSelector).get(0).text shouldBe sendOrContinueText
      document.select(bulletPointSelector).get(1).text shouldBe seeNewText
      document.select(bulletPointSelector).get(2).text shouldBe seeDetailedText
    }

    s"has a warning on the screen of $unassigningAnAgentText" in {
      document.select(unassigningAnAgentSelector).text shouldBe unassigningAnAgentText
    }

    s"has a subheading of $searchYourText" in {
      document.select(searchYourSelector).text shouldBe searchYourText
    }

    s"has a text input field for an $addressText" in {
      document.select(addressSelector).text shouldBe addressText
      document.select(addressInputSelector).attr("type") shouldBe "text"
    }

    s"has a $searchText button" in {
      document.select(searchSelector).text shouldBe searchText
    }

    s"has a $clearSearchText link" in {
      document.select(clearSearchSelector).text shouldBe clearSearchText
      document.select(clearSearchSelector).attr("href") shouldBe clearSearchHref
    }

    s"has a $selectAllText link" in {
      document.select(selectAllSelector).text shouldBe selectAllText
      document.select(selectAllSelector).attr("href") shouldBe selectAllHref
    }

    s"has an $addressText column header" in {
      document.select(addressHeaderSelector).text shouldBe addressText
    }

    s"has an $appointedAgentsText column header" in {
      document.select(appointedAgentsSelector).text shouldBe appointedAgentsText + " " + appointedAgentsText
    }

    s"has a row that contains a checkbox, $addressText and $appointedAgentsText" in {
      document.select(checkboxSelector).attr("type") shouldBe "checkbox"
      document.select(addressValueSelector).text shouldBe appointText + " TEST ADDRESS"
      document.select(appointedAgentsValueSelector).text shouldBe ""
    }

    s"does not have a second row that contains a checkbox, $addressText and $appointedAgentsText" in {
      document.select(secondCheckboxSelector).size() shouldBe 0
      document.select(secondAddressValueSelector).size() shouldBe 0
      document.select(secondAppointedAgentsValueSelector).size() shouldBe 0
    }

    s"has a $confirmAndUnassignText button" in {
      document.select(confirmAndUnassignSelector).text shouldBe confirmAndUnassignText
    }

    s"has a $cancelText link" in {
      document.select(cancelSelector).text shouldBe cancelText
      document.select(cancelSelector).attr("href") shouldBe cancelHref
    }
  }

  "revokeAgentProperties method displays the correct content in English when theres no properties" which {
    lazy val document: Document = getRevokeAgentPropertiesPage(English, multipleProps = None)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a $backLinkText link" in {
      document.select(backLinkSelector).text shouldBe backLinkText
      document.select(backLinkSelector).attr("href") shouldBe backLinkHref
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingSelector).text shouldBe headingText
    }

    s"has text on the screen of $forThePropertiesText" in {
      document.select(forThePropertiesSelector).text shouldBe forThePropertiesText
    }

    s"has bullet point list on the screen with text $sendOrContinueText, $seeNewText and $seeDetailedText" in {
      document.select(bulletPointSelector).get(0).text shouldBe sendOrContinueText
      document.select(bulletPointSelector).get(1).text shouldBe seeNewText
      document.select(bulletPointSelector).get(2).text shouldBe seeDetailedText
    }

    s"has a warning on the screen of $unassigningAnAgentText" in {
      document.select(unassigningAnAgentSelector).text shouldBe unassigningAnAgentText
    }

    s"has a subheading of $searchYourText" in {
      document.select(searchYourSelector).text shouldBe searchYourText
    }

    s"has a text input field for an $addressText" in {
      document.select(addressSelector).text shouldBe addressText
      document.select(addressInputSelector).attr("type") shouldBe "text"
    }

    s"has a $searchText button" in {
      document.select(searchSelector).text shouldBe searchText
    }

    s"has a $clearSearchText link" in {
      document.select(clearSearchSelector).text shouldBe clearSearchText
      document.select(clearSearchSelector).attr("href") shouldBe clearSearchHref
    }

    s"has text on the screen of $noPropertiesText" in {
      document.select(noPropertiesSelector).text shouldBe noPropertiesText
    }

    s"not have a $selectAllText link" in {
      document.select(selectAllSelector).size() shouldBe 0
    }

    s"not have an $addressText column header" in {
      document.select(addressHeaderSelector).size() shouldBe 0
    }

    s"not have an $appointedAgentsText column header" in {
      document.select(appointedAgentsSelector).size() shouldBe 0
    }

    s"not have a row that contains a checkbox, $addressText and $appointedAgentsText" in {
      document.select(checkboxSelector).size() shouldBe 0
      document.select(addressValueSelector).size() shouldBe 0
      document.select(appointedAgentsValueSelector).size() shouldBe 0
    }

    s"not have a $confirmAndUnassignText button" in {
      document.select(confirmAndUnassignSelector).size() shouldBe 0
    }

    s"not have a $cancelText link" in {
      document.select(cancelSelector).size() shouldBe 0
    }
  }

  "revokeAgentProperties method displays the correct content in Welsh when theres multiple properties" which {
    lazy val document: Document = getRevokeAgentPropertiesPage(Welsh, multipleProps = Some(true))

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a $backLinkText link in welsh" in {
      document.select(backLinkSelector).text shouldBe backLinkTextWelsh
      document.select(backLinkSelector).attr("href") shouldBe backLinkHref
    }

    s"has a heading of $headingText in welsh" in {
      document.getElementsByTag(headingSelector).text shouldBe headingTextWelsh
    }

    s"has text on the screen of $forThePropertiesText in welsh" in {
      document.select(forThePropertiesSelector).text shouldBe forThePropertiesTextWelsh
    }

    s"has bullet point list on the screen with text $sendOrContinueText, $seeNewText and $seeDetailedText in welsh" in {
      document.select(bulletPointSelector).get(0).text shouldBe sendOrContinueTextWelsh
      document.select(bulletPointSelector).get(1).text shouldBe seeNewTextWelsh
      document.select(bulletPointSelector).get(2).text shouldBe seeDetailedTextWelsh
    }

    s"has a warning on the screen of $unassigningAnAgentText in welsh" in {
      document.select(unassigningAnAgentSelector).text shouldBe unassigningAnAgentTextWelsh
    }

    s"has a subheading of $searchYourText in welsh" in {
      document.select(searchYourSelector).text shouldBe searchYourTextWelsh
    }

    s"has a text input field for an $addressText in welsh" in {
      document.select(addressSelector).text shouldBe addressTextWelsh
      document.select(addressInputSelector).attr("type") shouldBe "text"
    }

    s"has a $searchText button in welsh" in {
      document.select(searchSelector).text shouldBe searchTextWelsh
    }

    s"has a $clearSearchText link in welsh" in {
      document.select(clearSearchSelector).text shouldBe clearSearchTextWelsh
      document.select(clearSearchSelector).attr("href") shouldBe clearSearchHref
    }

    s"has a $selectAllText link in welsh" in {
      document.select(selectAllSelector).text shouldBe selectAllTextWelsh
      document.select(selectAllSelector).attr("href") shouldBe selectAllHref
    }

    s"has an $addressText column header in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
    }

    s"has an $appointedAgentsText column header in welsh" in {
      document.select(appointedAgentsSelector).text shouldBe appointedAgentsTextWelsh + " " + appointedAgentsTextWelsh
    }

    s"has a row that contains a checkbox, $addressText and $appointedAgentsText in welsh" in {
      document.select(checkboxSelector).attr("type") shouldBe "checkbox"
      document.select(addressValueSelector).text shouldBe appointTextWelsh + " TEST ADDRESS"
      document.select(appointedAgentsValueSelector).text shouldBe ""
    }

    s"have a second row that contains a checkbox, $addressText and $appointedAgentsText in welsh" in {
      document.select(secondCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(secondAddressValueSelector).text shouldBe appointTextWelsh + " ADDRESSTEST 2"
      document.select(secondAppointedAgentsValueSelector).text shouldBe "Test Agent 2"
    }

    s"has a $confirmAndUnassignText button in welsh" in {
      document.select(confirmAndUnassignSelector).text shouldBe confirmAndUnassignTextWelsh
    }

    s"has a $cancelText link in welsh" in {
      document.select(cancelSelector).text shouldBe cancelTextWelsh
      document.select(cancelSelector).attr("href") shouldBe cancelHref
    }
  }

  "revokeAgentProperties method displays the correct content in Welsh when theres one property" which {
    lazy val document: Document = getRevokeAgentPropertiesPage(Welsh, multipleProps = Some(false))

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a $backLinkText link in welsh" in {
      document.select(backLinkSelector).text shouldBe backLinkTextWelsh
      document.select(backLinkSelector).attr("href") shouldBe backLinkHref
    }

    s"has a heading of $headingText in welsh" in {
      document.getElementsByTag(headingSelector).text shouldBe headingTextWelsh
    }

    s"has text on the screen of $forThePropertiesText in welsh" in {
      document.select(forThePropertiesSelector).text shouldBe forThePropertiesTextWelsh
    }

    s"has bullet point list on the screen with text $sendOrContinueText, $seeNewText and $seeDetailedText in welsh" in {
      document.select(bulletPointSelector).get(0).text shouldBe sendOrContinueTextWelsh
      document.select(bulletPointSelector).get(1).text shouldBe seeNewTextWelsh
      document.select(bulletPointSelector).get(2).text shouldBe seeDetailedTextWelsh
    }

    s"has a warning on the screen of $unassigningAnAgentText in welsh" in {
      document.select(unassigningAnAgentSelector).text shouldBe unassigningAnAgentTextWelsh
    }

    s"has a subheading of $searchYourText in welsh" in {
      document.select(searchYourSelector).text shouldBe searchYourTextWelsh
    }

    s"has a text input field for an $addressText in welsh" in {
      document.select(addressSelector).text shouldBe addressTextWelsh
      document.select(addressInputSelector).attr("type") shouldBe "text"
    }

    s"has a $searchText button in welsh" in {
      document.select(searchSelector).text shouldBe searchTextWelsh
    }

    s"has a $clearSearchText link in welsh" in {
      document.select(clearSearchSelector).text shouldBe clearSearchTextWelsh
      document.select(clearSearchSelector).attr("href") shouldBe clearSearchHref
    }

    s"has a $selectAllText link in welsh" in {
      document.select(selectAllSelector).text shouldBe selectAllTextWelsh
      document.select(selectAllSelector).attr("href") shouldBe selectAllHref
    }

    s"has an $addressText column header in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
    }

    s"has an $appointedAgentsText column header in welsh" in {
      document.select(appointedAgentsSelector).text shouldBe appointedAgentsTextWelsh + " " + appointedAgentsTextWelsh
    }

    s"has a row that contains a checkbox, $addressText and $appointedAgentsText in welsh" in {
      document.select(checkboxSelector).attr("type") shouldBe "checkbox"
      document.select(addressValueSelector).text shouldBe appointTextWelsh + " TEST ADDRESS"
      document.select(appointedAgentsValueSelector).text shouldBe ""
    }

    s"does not have a second row that contains a checkbox, $addressText and $appointedAgentsText in welsh" in {
      document.select(secondCheckboxSelector).size() shouldBe 0
      document.select(secondAddressValueSelector).size() shouldBe 0
      document.select(secondAppointedAgentsValueSelector).size() shouldBe 0
    }

    s"has a $confirmAndUnassignText button in welsh" in {
      document.select(confirmAndUnassignSelector).text shouldBe confirmAndUnassignTextWelsh
    }

    s"has a $cancelText link in welsh" in {
      document.select(cancelSelector).text shouldBe cancelTextWelsh
      document.select(cancelSelector).attr("href") shouldBe cancelHref
    }
  }

  "revokeAgentProperties method displays the correct content in Welsh when theres no properties" which {
    lazy val document: Document = getRevokeAgentPropertiesPage(Welsh, multipleProps = None)

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a $backLinkText link in welsh" in {
      document.select(backLinkSelector).text shouldBe backLinkTextWelsh
      document.select(backLinkSelector).attr("href") shouldBe backLinkHref
    }

    s"has a heading of $headingText in welsh" in {
      document.getElementsByTag(headingSelector).text shouldBe headingTextWelsh
    }

    s"has text on the screen of $forThePropertiesText in welsh" in {
      document.select(forThePropertiesSelector).text shouldBe forThePropertiesTextWelsh
    }

    s"has bullet point list on the screen with text $sendOrContinueText, $seeNewText and $seeDetailedText in welsh" in {
      document.select(bulletPointSelector).get(0).text shouldBe sendOrContinueTextWelsh
      document.select(bulletPointSelector).get(1).text shouldBe seeNewTextWelsh
      document.select(bulletPointSelector).get(2).text shouldBe seeDetailedTextWelsh
    }

    s"has a warning on the screen of $unassigningAnAgentText in welsh" in {
      document.select(unassigningAnAgentSelector).text shouldBe unassigningAnAgentTextWelsh
    }

    s"has a subheading of $searchYourText in welsh" in {
      document.select(searchYourSelector).text shouldBe searchYourTextWelsh
    }

    s"has a text input field for an $addressText in welsh" in {
      document.select(addressSelector).text shouldBe addressTextWelsh
      document.select(addressInputSelector).attr("type") shouldBe "text"
    }

    s"has a $searchText button in welsh" in {
      document.select(searchSelector).text shouldBe searchTextWelsh
    }

    s"has a $clearSearchText link in welsh" in {
      document.select(clearSearchSelector).text shouldBe clearSearchTextWelsh
      document.select(clearSearchSelector).attr("href") shouldBe clearSearchHref
    }

    s"has text on the screen of $noPropertiesText in welsh" in {
      document.select(noPropertiesSelector).text shouldBe noPropertiesTextWelsh
    }

    s"not have a $selectAllText link" in {
      document.select(selectAllSelector).size() shouldBe 0
    }

    s"not have an $addressText column header" in {
      document.select(addressHeaderSelector).size() shouldBe 0
    }

    s"not have an $appointedAgentsText column header" in {
      document.select(appointedAgentsSelector).size() shouldBe 0
    }

    s"not have a row that contains a checkbox, $addressText and $appointedAgentsText" in {
      document.select(checkboxSelector).size() shouldBe 0
      document.select(addressValueSelector).size() shouldBe 0
      document.select(appointedAgentsValueSelector).size() shouldBe 0
    }

    s"not have a $confirmAndUnassignText button" in {
      document.select(confirmAndUnassignSelector).size() shouldBe 0
    }

    s"not have a $cancelText link" in {
      document.select(cancelSelector).size() shouldBe 0
    }
  }

  // Error scenarios on the POST requests

  "revokeAgentProperties method displays the correct error content in English when no address is selected" which {
    lazy val document: Document = postRevokeAgentPropertiesPageWithNoSelection(English, multipleProps = Some(true))

    s"has a title of $errorTitleText" in {
      document.title() shouldBe errorTitleText
    }

    s"has an error in the error summary of $noAddressChoiceText" in {
      document.select(errorAtSummarySelector).text shouldBe noAddressChoiceText
      document.select(errorAtSummarySelector).attr("href") shouldBe "#checkbox-1"
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingSelector).text shouldBe headingText
    }

    s"has an error above the label of $noAddressChoiceText" in {
      document.select(noAddressChoiceAtLabelSelector).text shouldBe noAddressChoiceText
    }

  }

  "revokeAgentProperties method displays the correct error content in Welsh when no address is selected" which {
    lazy val document: Document = postRevokeAgentPropertiesPageWithNoSelection(Welsh, multipleProps = Some(true))

    s"has a title of $errorTitleText" in {
      document.title() shouldBe errorTitleTextWelsh
    }

    s"has an error in the error summary of $noAddressChoiceText in welsh" in {
      document.select(errorAtSummarySelector).text shouldBe noAddressChoiceTextWelsh
      document.select(errorAtSummarySelector).attr("href") shouldBe "#checkbox-1"
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingSelector).text shouldBe headingTextWelsh
    }

    s"has an error above the label of $noAddressChoiceText in welsh" in {
      document.select(noAddressChoiceAtLabelSelector).text shouldBe noAddressChoiceTextWelsh
    }

  }

  "revokeAgentProperties method displays the correct error content in English when an empty address is input" which {
    lazy val document: Document = postRevokeAgentPropertiesPageWithEmptyAddress(English, multipleProps = Some(true))

    s"has a title of $errorTitleText" in {
      document.title() shouldBe errorTitleText
    }

    s"has an error in the error summary of $emptyAddressErrorText" in {
      document.select(errorAtSummarySelector).text shouldBe emptyAddressErrorText
      document.select(errorAtSummarySelector).attr("href") shouldBe "#address"
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingSelector).text shouldBe headingText
    }

    s"has an error above the label of $emptyAddressErrorText" in {
      document.select(emptyAddressErrorLabelSelector).text shouldBe emptyAddressErrorText
    }

  }

  "revokeAgentProperties method displays the correct error content in Welsh when an empty address is input" which {
    lazy val document: Document = postRevokeAgentPropertiesPageWithEmptyAddress(Welsh, multipleProps = Some(true))

    s"has a title of $errorTitleText in welsh" in {
      document.title() shouldBe errorTitleTextWelsh
    }

    s"has an error in the error summary of $emptyAddressErrorText in welsh" in {
      document.select(errorAtSummarySelector).text shouldBe emptyAddressErrorTextWelsh
      document.select(errorAtSummarySelector).attr("href") shouldBe "#address"
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingSelector).text shouldBe headingTextWelsh
    }

    s"has an error above the label of $emptyAddressErrorText in welsh" in {
      document.select(emptyAddressErrorLabelSelector).text shouldBe emptyAddressErrorAboveLabelTextWelsh
    }

  }

  private def postRevokeAgentPropertiesPageWithNoSelection(
        language: Language,
        multipleProps: Option[Boolean]
  ): Document = {

    setupStubs(multipleProps)

    val requestBody = Json.obj(
      "agentCode"   -> agentCode,
      "name"        -> agentName,
      "backLinkUrl" -> s"$backLinkHref"
    )

    stubFor {
      get(
        s"/property-linking/my-organisation/agents/$agentCode/property-links?agent=gg-ext-id&sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
      ).willReturn {
        aResponse
          .withStatus(OK)
          .withBody(
            Json
              .toJson(
                multipleProps match {
                  case Some(true)  => testOwnerAuthResultMultipleProperty
                  case Some(false) => testOwnerAuthResult
                  case None        => testOwnerAuthResultNoProperties
                }
              )
              .toString()
          )
      }
    }

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/revoke/properties/create?page=1&pageSize=1&agentCode=$agentCode"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )

    res.status shouldBe BAD_REQUEST
    Jsoup.parse(res.body)
  }

  private def postRevokeAgentPropertiesPageWithEmptyAddress(
        language: Language,
        multipleProps: Option[Boolean]
  ): Document = {

    setupStubs(multipleProps)

    val requestBody = Json.obj(
      "paseSize"    -> "15",
      "backLinkUrl" -> s"$backLinkHref",
      "address"     -> ""
    )

    stubFor {
      get(
        s"/property-linking/my-organisation/agents/$agentCode/property-links?agent=gg-ext-id&sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
      ).willReturn {
        aResponse
          .withStatus(OK)
          .withBody(
            Json
              .toJson(
                multipleProps match {
                  case Some(true)  => testOwnerAuthResultMultipleProperty
                  case Some(false) => testOwnerAuthResult
                  case None        => testOwnerAuthResultNoProperties
                }
              )
              .toString()
          )
      }
    }

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/revoke/properties/filter?page=1&pageSize=15&agentCode=$agentCode"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )

    res.status shouldBe BAD_REQUEST
    Jsoup.parse(res.body)
  }

  private def getRevokeAgentPropertiesPage(language: Language, multipleProps: Option[Boolean]): Document = {

    setupStubs(multipleProps)

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/revoke/properties?page=1&pageSize=15&agentCode=$agentCode"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId")
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def setupStubs(multipleProps: Option[Boolean]): StubMapping = {
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

    val account = groupAccount(true)

    stubFor {
      get(s"/property-linking/groups/agentCode/$agentCode")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(account).toString())
        }
    }

    stubFor {
      get(
        s"/property-linking/my-organisation/agents/$agentCode/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
      ).willReturn {
        aResponse
          .withStatus(OK)
          .withBody(
            Json
              .toJson(
                multipleProps match {
                  case Some(true)  => testOwnerAuthResultMultipleProperty
                  case Some(false) => testOwnerAuthResult
                  case None        => testOwnerAuthResultNoProperties
                }
              )
              .toString()
          )
      }
    }
  }

}
