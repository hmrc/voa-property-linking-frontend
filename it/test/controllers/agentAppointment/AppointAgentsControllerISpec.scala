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
import models.propertyrepresentation.{AppointAgentToSomePropertiesSession, FilterAppointProperties}
import models.searchApi.{OwnerAuthAgent, OwnerAuthResult, OwnerAuthorisation}
import models.{AgentAppointBulkAction, GroupAccount}
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.AppointAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import java.util.UUID

class AppointAgentsControllerISpec extends ISpecBase with HtmlComponentHelpers {

  val titleText = "Choose which of your properties you want to assign gg-ext-id to - Valuation Office Agency - GOV.UK"
  val headingText = "Choose which of your properties you want to assign gg-ext-id to"
  val captionText = "Appoint an agent"
  val backLinkText = "Back"
  val p1Text = "For the properties you select, the agent will be able to:"
  val bullet1Text = "see detailed property information"
  val bullet2Text = "see Check and Challenge case correspondence such as messages and emails"
  val bullet3Text = "send Check and Challenge cases"
  val searchFormHeader = "Search your properties"
  val addressSearchInputHeader = "Address"
  val addressSearchInput = ""
  val agentSearchHeader = "Agent"
  val agentSearchDropdownOneAssignedAgent = "Choose from 1 agent Test Agent"
  val searchButton = "Search"
  val clearSearchLink = "Clear search"
  val selectAllLink = "Select all"
  val showWithoutAgentsAssignedLink = "Only show properties with no agent"
  val showAllLink = "Show all properties"
  val addressHeaderSortable = "Address"
  val appointedAgentsHeaderSortable = "Appointed agents"
  val continueButtonText = "Continue"

  val titleTextWelsh = "Dewis pa eiddo yr hoffech ei neilltuo i gg-ext-id - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Dewis pa eiddo yr hoffech ei neilltuo i gg-ext-id"
  val captionTextWelsh = "Penodi asiant"
  val backLinkTextWelsh = "Yn ôl"
  val p1TextWelsh = "Ar gyfer yr eiddo a ddewiswch, bydd yr asiant yn gallu:"
  val bullet1TextWelsh = "gweld gwybodaeth eiddo fanwl"
  val bullet2TextWelsh = "gweld gohebiaeth achosion Gwirio a Herio megis negeseuon ac e-byst"
  val bullet3TextWelsh = "anfon achosion Gwirio a Herio"
  val searchFormHeaderWelsh = "Chwiliwch eich eiddo"
  val addressSearchInputHeaderWelsh = "Cyfeiriad"
  val addressSearchInputWelsh = ""
  val agentSearchHeaderWelsh = "Asiant"
  val agentSearchDropdownOneAssignedAgentWelsh = "Dewis o 1 asiant Test Agent"
  val searchButtonWelsh = "Chwilio"
  val clearSearchLinkWelsh = "Clirio’r chwiliad"
  val selectAllLinkWelsh = "Clear search"
  val showWithoutAgentsAssignedLinkWelsh = "Only show properties with no agent"
  val showAllLinkWelsh = "Show all properties"
  val addressHeaderSortableWelsh = "Cyfeiriad"
  val appointedAgentsHeaderSortableWelsh = "Asiantiaid penodedig"
  val continueButtonTextWelsh = "Yn eich blaen"

  val headingLocator = "h1"
  val captionLocator = "#caption"
  val backLinkLocator = "back-link"
  val p1Locator = "#explainer-intro"
  val bulletPoint1Locator = "#explainer-list > li:nth-child(1)"
  val bulletPoint2Locator = "#explainer-list > li:nth-child(2)"
  val bulletPoint3Locator = "#explainer-list > li:nth-child(3)"
  val searchFormHeaderLocator = "#search-fieldset > legend"
  val addressSearchInputHeaderLocator = "#address-input-label"
  val addressSearchInputLocator = "#address"
  val agentSearchHeaderLocator = "#agent-select-label"
  val agentSearchDropdownLocator = "#agent-select"
  val searchButtonLocator = "#search-submit"
  val clearSearchLinkLocator = "#clear-search"
  val selectAllLinkLocator = "Select all"
  val showWithoutAgentsAssignedLinkLocator = "Only show properties with no agent"
  val showAllLinkLocator = "Show all properties"
  val addressHeaderSortableLocator = "#sort-by-address-link"
  val addressValue1Locator =
    "#agentPropertiesTableBody > tbody > tr:nth-child(1) > td:nth-child(1) > div > label > span:nth-child(2)"
  val addressValue2Locator =
    "#agentPropertiesTableBody > tbody > tr:nth-child(2) > td:nth-child(1) > div > label > span:nth-child(2)"
  val appointedAgentsHeaderSortableLocator = "#sort-by-agent-link"
  val appointedAgentsValue1Locator =
    "#agentPropertiesTableBody > tbody > tr:nth-child(1) > td.govuk-table__cell.govuk-\\!-padding-top-3 > ul > li"
  val appointedAgentsValue2Locator =
    "#agentPropertiesTableBody > tbody > tr:nth-child(2) > td.govuk-table__cell.govuk-\\!-padding-top-3 > ul > li"
  val checkBox1Locator = "#checkbox-1"
  val checkBox2Locator = "#checkbox-2"
  val continueButtonLocator = "#submit-button"

  val defaultBackLinkHref = "http://localhost/some-back-link"
  val clearSearchLinkHref =
    "/business-rates-property-linking/my-organisation/appoint/properties?page=1&pageSize=15&agentCode=1001&backLinkUrl=http%3A%2F%2Flocalhost%2Fsome-back-link&fromManageAgentJourney=true"
  val addressHeaderSortableHref =
    "/business-rates-property-linking/my-organisation/appoint/properties/sort?sortField=ADDRESS&page=1&pageSize=15&agentCode=1001&backLinkUrl=http%3A%2F%2Flocalhost%2Fsome-back-link&fromManageAgentJourney=true"
  val appointedAgentHeaderSortableHref =
    "/business-rates-property-linking/my-organisation/appoint/properties/sort?sortField=AGENT&page=1&pageSize=15&agentCode=1001&backLinkUrl=http%3A%2F%2Flocalhost%2Fsome-back-link&fromManageAgentJourney=true"

  "getMyOrganisationPropertyLinksWithAgentFiltering" should {
    "display the correct content in English - No agents assigned" which {
      lazy val res = getMyOrganisationPropertyLinksWithAgentFilteringPage(language = English)

      lazy val document = Jsoup.parse(res.body)

      s"has a status of 200 OK" in {
        res.status shouldBe OK
      }

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "displays a correct caption above the header" in {
        document.select(captionLocator).text shouldBe captionText
      }

      s"has a heading of $headingText" in {
        document.getElementsByTag(headingLocator).text shouldBe headingText
      }

      "has a back link" in {
        document.getElementById(backLinkLocator).text shouldBe backLinkText
        document.getElementById(backLinkLocator).attr("href") shouldBe defaultBackLinkHref
      }

      s"has a p1 of $p1Text" in {
        document.select(p1Locator).text shouldBe p1Text
      }

      s"has a bullet point of $bullet1Text" in {
        document.select(bulletPoint1Locator).text shouldBe bullet1Text
      }

      s"has a bullet point of $bullet2Text" in {
        document.select(bulletPoint2Locator).text shouldBe bullet2Text
      }

      s"has a bullet point of $bullet3Text" in {
        document.select(bulletPoint3Locator).text shouldBe bullet3Text
      }

      s"has a search form header of $searchFormHeader" in {
        document.select(searchFormHeaderLocator).text shouldBe searchFormHeader
      }

      s"has a search header of $addressSearchInputHeader and Input field" in {
        document.select(addressSearchInputHeaderLocator).text shouldBe addressSearchInputHeader
        document.select(addressSearchInputLocator).text shouldBe addressSearchInput
      }

      s"has a search button of $searchButton" in {
        document.select(searchButtonLocator).text shouldBe searchButton
      }

      s"has a clear search link of $clearSearchLink" in {
        document.select(clearSearchLinkLocator).text shouldBe clearSearchLink
        document.select(clearSearchLinkLocator).attr("href") shouldBe clearSearchLinkHref
      }

      s"has a sortable table header of $addressHeaderSortable" in {
        document.select(addressHeaderSortableLocator).text shouldBe addressHeaderSortable
        document.select(addressHeaderSortableLocator).attr("href") shouldBe addressHeaderSortableHref
      }

      "has the correct address value in row 1" in {
        document.select(addressValue1Locator).text shouldBe "TEST ADDRESS"
      }

      "has the correct address value in row 2" in {
        document.select(addressValue2Locator).text shouldBe "NO.2 TEST ADDRESS"
      }

      s"has a sortable table header of $appointedAgentsHeaderSortable" in {
        document.select(appointedAgentsHeaderSortableLocator).text shouldBe appointedAgentsHeaderSortable
        document.select(appointedAgentsHeaderSortableLocator).attr("href") shouldBe appointedAgentHeaderSortableHref
      }

      "has the correct appointed agent value in row 1" in {
        document.select(appointedAgentsValue1Locator).text shouldBe ""
      }

      "has the correct appointed agent value in row 2" in {
        document.select(appointedAgentsValue2Locator).text shouldBe ""
      }

      "has a checkbox with the correct type & value" in {
        document.select(checkBox1Locator).attr("type") shouldBe "checkbox"
        document.select(checkBox1Locator).attr("value") shouldBe "12345"
      }

      "has a 2nd checkbox with the correct type & value" in {
        document.select(checkBox2Locator).attr("type") shouldBe "checkbox"
        document.select(checkBox2Locator).attr("value") shouldBe "123456"
      }

      // This is triggered by JS & not loaded in document (Not sure why JS is needed)
      //      s"has a clear search link of $selectAllLink" in {
      //        document.select(selectAllLinkLocator).text shouldBe selectAllLink
      //        document.select(selectAllLinkLocator).attr("href") shouldBe clearSearchLink
      //      }
      //
      //      s"has a clear search link of $showWithoutAgentsAssignedLink" in {
      //        document.select(showWithoutAgentsAssignedLinkLocator).text shouldBe showWithoutAgentsAssignedLink
      //        document.select(showWithoutAgentsAssignedLinkLocator).attr("href") shouldBe clearSearchLink
      //      }

      s"has a $continueButtonText button" in {
        document.select(continueButtonLocator).text shouldBe continueButtonText
      }
    }

    "display the correct content in Welsh - No agents assigned" which {
      lazy val res = getMyOrganisationPropertyLinksWithAgentFilteringPage(language = Welsh)

      lazy val document = Jsoup.parse(res.body)

      s"has a status of 200 OK" in {
        res.status shouldBe OK
      }

      s"has a title of $titleText" in {
        document.title() shouldBe titleTextWelsh
      }

      "displays a correct caption above the header" in {
        document.select(captionLocator).text shouldBe captionTextWelsh
      }

      s"has a heading of $headingText" in {
        document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
      }

      "has a back link" in {
        document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
        document.getElementById(backLinkLocator).attr("href") shouldBe defaultBackLinkHref
      }

      s"has a p1 of $p1Text" in {
        document.select(p1Locator).text shouldBe p1TextWelsh
      }

      s"has a bullet point of $bullet1Text" in {
        document.select(bulletPoint1Locator).text shouldBe bullet1TextWelsh
      }

      s"has a bullet point of $bullet2Text" in {
        document.select(bulletPoint2Locator).text shouldBe bullet2TextWelsh
      }

      s"has a bullet point of $bullet3Text" in {
        document.select(bulletPoint3Locator).text shouldBe bullet3TextWelsh
      }

      s"has a search form header of $searchFormHeader" in {
        document.select(searchFormHeaderLocator).text shouldBe searchFormHeaderWelsh
      }

      s"has a search header of $addressSearchInputHeader and Input field" in {
        document.select(addressSearchInputHeaderLocator).text shouldBe addressSearchInputHeaderWelsh
        document.select(addressSearchInputLocator).text shouldBe addressSearchInput
      }

      s"has a search button of $searchButton" in {
        document.select(searchButtonLocator).text shouldBe searchButtonWelsh
      }

      s"has a clear search link of $clearSearchLink" in {
        document.select(clearSearchLinkLocator).text shouldBe clearSearchLinkWelsh
        document.select(clearSearchLinkLocator).attr("href") shouldBe clearSearchLinkHref
      }

      s"has a sortable table header of $addressHeaderSortable" in {
        document.select(addressHeaderSortableLocator).text shouldBe addressHeaderSortableWelsh
        document.select(addressHeaderSortableLocator).attr("href") shouldBe addressHeaderSortableHref
      }

      "has the correct address value in row 1" in {
        document.select(addressValue1Locator).text shouldBe "TEST ADDRESS"
      }

      "has the correct address value in row 2" in {
        document.select(addressValue2Locator).text shouldBe "NO.2 TEST ADDRESS"
      }

      s"has a sortable table header of $appointedAgentsHeaderSortable" in {
        document.select(appointedAgentsHeaderSortableLocator).text shouldBe appointedAgentsHeaderSortableWelsh
        document.select(appointedAgentsHeaderSortableLocator).attr("href") shouldBe appointedAgentHeaderSortableHref
      }

      "has the correct appointed agent value in row 1" in {
        document.select(appointedAgentsValue1Locator).text shouldBe ""
      }

      "has the correct appointed agent value in row 2" in {
        document.select(appointedAgentsValue2Locator).text shouldBe ""
      }

      "has a checkbox with the correct type & value" in {
        document.select(checkBox1Locator).attr("type") shouldBe "checkbox"
        document.select(checkBox1Locator).attr("value") shouldBe "12345"
      }

      "has a 2nd checkbox with the correct type & value" in {
        document.select(checkBox2Locator).attr("type") shouldBe "checkbox"
        document.select(checkBox2Locator).attr("value") shouldBe "123456"
      }

      s"has a $continueButtonText button" in {
        document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
      }
    }

    "display the correct content in English - With agents assigned" which {
      lazy val res = getMyOrganisationPropertyLinksWithAgentFilteringPage(language = English, withAgents = true)

      lazy val document = Jsoup.parse(res.body)

      s"has a status of 200 OK" in {
        res.status shouldBe OK
      }

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "displays a correct caption above the header" in {
        document.select(captionLocator).text shouldBe captionText
      }

      s"has a heading of $headingText" in {
        document.getElementsByTag(headingLocator).text shouldBe headingText
      }

      "has a back link" in {
        document.getElementById(backLinkLocator).text shouldBe backLinkText
        document.getElementById(backLinkLocator).attr("href") shouldBe defaultBackLinkHref
      }

      s"has a p1 of $p1Text" in {
        document.select(p1Locator).text shouldBe p1Text
      }

      s"has a bullet point of $bullet1Text" in {
        document.select(bulletPoint1Locator).text shouldBe bullet1Text
      }

      s"has a bullet point of $bullet2Text" in {
        document.select(bulletPoint2Locator).text shouldBe bullet2Text
      }

      s"has a bullet point of $bullet3Text" in {
        document.select(bulletPoint3Locator).text shouldBe bullet3Text
      }

      s"has a search form header of $searchFormHeader" in {
        document.select(searchFormHeaderLocator).text shouldBe searchFormHeader
      }

      s"has a search header of $addressSearchInputHeader and Input field" in {
        document.select(addressSearchInputHeaderLocator).text shouldBe addressSearchInputHeader
        document.select(addressSearchInputLocator).text shouldBe addressSearchInput
      }

      s"has a search header of $agentSearchHeader and Input field" in {
        document.select(agentSearchHeaderLocator).text shouldBe agentSearchHeader
        document.select(agentSearchDropdownLocator).text shouldBe agentSearchDropdownOneAssignedAgent
      }

      s"has a search button of $searchButton" in {
        document.select(searchButtonLocator).text shouldBe searchButton
      }

      s"has a clear search link of $clearSearchLink" in {
        document.select(clearSearchLinkLocator).text shouldBe clearSearchLink
        document.select(clearSearchLinkLocator).attr("href") shouldBe clearSearchLinkHref
      }

      s"has a sortable table header of $addressHeaderSortable" in {
        document.select(addressHeaderSortableLocator).text shouldBe addressHeaderSortable
        document.select(addressHeaderSortableLocator).attr("href") shouldBe addressHeaderSortableHref
      }

      "has the correct address value in row 1" in {
        document.select(addressValue1Locator).text shouldBe "TEST ADDRESS"
      }

      "has the correct address value in row 2" in {
        document.select(addressValue2Locator).text shouldBe "NO.2 TEST ADDRESS"
      }

      s"has a sortable table header of $appointedAgentsHeaderSortable" in {
        document.select(appointedAgentsHeaderSortableLocator).text shouldBe appointedAgentsHeaderSortable
        document.select(appointedAgentsHeaderSortableLocator).attr("href") shouldBe appointedAgentHeaderSortableHref
      }

      "has the correct appointed agent value in row 1" in {
        document.select(appointedAgentsValue1Locator).text shouldBe "Test Agent"
      }

      "has the correct appointed agent value in row 2" in {
        document.select(appointedAgentsValue2Locator).text shouldBe ""
      }

      "has a checkbox with the correct type & value" in {
        document.select(checkBox1Locator).attr("type") shouldBe "checkbox"
        document.select(checkBox1Locator).attr("value") shouldBe "12345"
      }

      "has a 2nd checkbox with the correct type & value" in {
        document.select(checkBox2Locator).attr("type") shouldBe "checkbox"
        document.select(checkBox2Locator).attr("value") shouldBe "123456"
      }

      s"has a $continueButtonText button" in {
        document.select(continueButtonLocator).text shouldBe continueButtonText
      }
    }

    "display the correct content in Welsh - With agents assigned" which {
      lazy val res = getMyOrganisationPropertyLinksWithAgentFilteringPage(language = Welsh, withAgents = true)

      lazy val document = Jsoup.parse(res.body)

      s"has a status of 200 OK" in {
        res.status shouldBe OK
      }

      s"has a title of $titleText" in {
        document.title() shouldBe titleTextWelsh
      }

      "displays a correct caption above the header" in {
        document.select(captionLocator).text shouldBe captionTextWelsh
      }

      s"has a heading of $headingText" in {
        document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
      }

      "has a back link" in {
        document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
        document.getElementById(backLinkLocator).attr("href") shouldBe defaultBackLinkHref
      }

      s"has a p1 of $p1Text" in {
        document.select(p1Locator).text shouldBe p1TextWelsh
      }

      s"has a bullet point of $bullet1Text" in {
        document.select(bulletPoint1Locator).text shouldBe bullet1TextWelsh
      }

      s"has a bullet point of $bullet2Text" in {
        document.select(bulletPoint2Locator).text shouldBe bullet2TextWelsh
      }

      s"has a bullet point of $bullet3Text" in {
        document.select(bulletPoint3Locator).text shouldBe bullet3TextWelsh
      }

      s"has a search form header of $searchFormHeader" in {
        document.select(searchFormHeaderLocator).text shouldBe searchFormHeaderWelsh
      }

      s"has a search header of $addressSearchInputHeader and Input field" in {
        document.select(addressSearchInputHeaderLocator).text shouldBe addressSearchInputHeaderWelsh
        document.select(addressSearchInputLocator).text shouldBe addressSearchInput
      }

      s"has a search header of $agentSearchHeader and Input field" in {
        document.select(agentSearchHeaderLocator).text shouldBe agentSearchHeaderWelsh
        document.select(agentSearchDropdownLocator).text shouldBe agentSearchDropdownOneAssignedAgentWelsh
      }

      s"has a search button of $searchButton" in {
        document.select(searchButtonLocator).text shouldBe searchButtonWelsh
      }

      s"has a clear search link of $clearSearchLink" in {
        document.select(clearSearchLinkLocator).text shouldBe clearSearchLinkWelsh
        document.select(clearSearchLinkLocator).attr("href") shouldBe clearSearchLinkHref
      }

      s"has a sortable table header of $addressHeaderSortable" in {
        document.select(addressHeaderSortableLocator).text shouldBe addressHeaderSortableWelsh
        document.select(addressHeaderSortableLocator).attr("href") shouldBe addressHeaderSortableHref
      }

      "has the correct address value in row 1" in {
        document.select(addressValue1Locator).text shouldBe "TEST ADDRESS"
      }

      "has the correct address value in row 2" in {
        document.select(addressValue2Locator).text shouldBe "NO.2 TEST ADDRESS"
      }

      s"has a sortable table header of $appointedAgentsHeaderSortable" in {
        document.select(appointedAgentsHeaderSortableLocator).text shouldBe appointedAgentsHeaderSortableWelsh
        document.select(appointedAgentsHeaderSortableLocator).attr("href") shouldBe appointedAgentHeaderSortableHref
      }

      "has the correct appointed agent value in row 1" in {
        document.select(appointedAgentsValue1Locator).text shouldBe "Test Agent"
      }

      "has the correct appointed agent value in row 2" in {
        document.select(appointedAgentsValue2Locator).text shouldBe ""
      }

      "has a checkbox with the correct type & value" in {
        document.select(checkBox1Locator).attr("type") shouldBe "checkbox"
        document.select(checkBox1Locator).attr("value") shouldBe "12345"
      }

      "has a 2nd checkbox with the correct type & value" in {
        document.select(checkBox2Locator).attr("type") shouldBe "checkbox"
        document.select(checkBox2Locator).attr("value") shouldBe "123456"
      }

      s"has a $continueButtonText button" in {
        document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
      }
    }

    "display the correct content in English - With show only properties without agents assigned filter" which {
      lazy val res = getMyOrganisationPropertyLinksWithAgentFilteringPage(
        language = English,
        withAgents = true,
        withAgentsNotAppointed = true
      )

      lazy val document = Jsoup.parse(res.body)

      s"has a status of 200 OK" in {
        res.status shouldBe OK
      }

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "displays a correct caption above the header" in {
        document.select(captionLocator).text shouldBe captionText
      }

      s"has a heading of $headingText" in {
        document.getElementsByTag(headingLocator).text shouldBe headingText
      }

      "has a back link" in {
        document.getElementById(backLinkLocator).text shouldBe backLinkText
        document.getElementById(backLinkLocator).attr("href") shouldBe defaultBackLinkHref
      }

      s"has a p1 of $headingText" in {
        document.getElementsByTag(headingLocator).text shouldBe headingText
      }

      s"has a bullet point of $bullet1Text" in {
        document.select(bulletPoint1Locator).text shouldBe bullet1Text
      }

      s"has a bullet point of $bullet2Text" in {
        document.select(bulletPoint2Locator).text shouldBe bullet2Text
      }

      s"has a bullet point of $bullet3Text" in {
        document.select(bulletPoint3Locator).text shouldBe bullet3Text
      }

      s"has a search form header of $searchFormHeader" in {
        document.select(searchFormHeaderLocator).text shouldBe searchFormHeader
      }

      s"has a search header of $addressSearchInputHeader and Input field" in {
        document.select(addressSearchInputHeaderLocator).text shouldBe addressSearchInputHeader
        document.select(addressSearchInputLocator).text shouldBe addressSearchInput
      }

      s"has a search header of $agentSearchHeader and Input field" in {
        document.select(agentSearchHeaderLocator).text shouldBe agentSearchHeader
        document.select(agentSearchDropdownLocator).text shouldBe agentSearchDropdownOneAssignedAgent
      }

      s"has a search button of $searchButton" in {
        document.select(searchButtonLocator).text shouldBe searchButton
      }

      s"has a clear search link of $clearSearchLink" in {
        document.select(clearSearchLinkLocator).text shouldBe clearSearchLink
        document
          .select(clearSearchLinkLocator)
          .attr(
            "href"
          ) shouldBe "/business-rates-property-linking/my-organisation/appoint/properties?page=1&pageSize=15&agentCode=1001&agentAppointed=NO&backLinkUrl=http%3A%2F%2Flocalhost%2Fsome-back-link&fromManageAgentJourney=true"
      }

      s"has a sortable table header of $addressHeaderSortable" in {
        document.select(addressHeaderSortableLocator).text shouldBe addressHeaderSortable
        document
          .select(addressHeaderSortableLocator)
          .attr(
            "href"
          ) shouldBe "/business-rates-property-linking/my-organisation/appoint/properties/sort?sortField=ADDRESS&page=1&pageSize=15&agentCode=1001&agentAppointed=NO&backLinkUrl=http%3A%2F%2Flocalhost%2Fsome-back-link&fromManageAgentJourney=true"
      }

      "has the correct address value in row 1" in {
        document.select(addressValue1Locator).text shouldBe "NO.2 TEST ADDRESS"
      }

      "has the correct address value in row 2" in {
        document.select(addressValue2Locator).text shouldBe ""
      }

      s"has a sortable table header of $appointedAgentsHeaderSortable" in {
        document.select(appointedAgentsHeaderSortableLocator).text shouldBe appointedAgentsHeaderSortable
        document
          .select(appointedAgentsHeaderSortableLocator)
          .attr(
            "href"
          ) shouldBe "/business-rates-property-linking/my-organisation/appoint/properties/sort?sortField=AGENT&page=1&pageSize=15&agentCode=1001&agentAppointed=NO&backLinkUrl=http%3A%2F%2Flocalhost%2Fsome-back-link&fromManageAgentJourney=true"
      }

      "has the correct appointed agent value in row 1" in {
        document.select(appointedAgentsValue1Locator).text shouldBe ""
      }

      "has the correct appointed agent value in row 2" in {
        document.select(appointedAgentsValue2Locator).text shouldBe ""
      }

      "has a checkbox with the correct type & value" in {
        document.select(checkBox1Locator).attr("type") shouldBe "checkbox"
        document.select(checkBox1Locator).attr("value") shouldBe "123456"
      }

      s"has a $continueButtonText button" in {
        document.select(continueButtonLocator).text shouldBe continueButtonText
      }
    }

    "display the correct content in Welsh - With show only properties without agents assigned filter" which {
      lazy val res = getMyOrganisationPropertyLinksWithAgentFilteringPage(
        language = Welsh,
        withAgents = true,
        withAgentsNotAppointed = true
      )

      lazy val document = Jsoup.parse(res.body)

      s"has a status of 200 OK" in {
        res.status shouldBe OK
      }

      s"has a title of $titleText" in {
        document.title() shouldBe titleTextWelsh
      }

      "displays a correct caption above the header" in {
        document.select(captionLocator).text shouldBe captionTextWelsh
      }

      s"has a heading of $headingText" in {
        document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
      }

      "has a back link" in {
        document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
        document.getElementById(backLinkLocator).attr("href") shouldBe defaultBackLinkHref
      }

      s"has a p1 of $headingText" in {
        document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
      }

      s"has a bullet point of $bullet1Text" in {
        document.select(bulletPoint1Locator).text shouldBe bullet1TextWelsh
      }

      s"has a bullet point of $bullet2Text" in {
        document.select(bulletPoint2Locator).text shouldBe bullet2TextWelsh
      }

      s"has a bullet point of $bullet3Text" in {
        document.select(bulletPoint3Locator).text shouldBe bullet3TextWelsh
      }

      s"has a search form header of $searchFormHeader" in {
        document.select(searchFormHeaderLocator).text shouldBe searchFormHeaderWelsh
      }

      s"has a search header of $addressSearchInputHeader and Input field" in {
        document.select(addressSearchInputHeaderLocator).text shouldBe addressSearchInputHeaderWelsh
        document.select(addressSearchInputLocator).text shouldBe addressSearchInput
      }

      s"has a search header of $agentSearchHeader and Input field" in {
        document.select(agentSearchHeaderLocator).text shouldBe agentSearchHeaderWelsh
        document.select(agentSearchDropdownLocator).text shouldBe agentSearchDropdownOneAssignedAgentWelsh
      }

      s"has a search button of $searchButton" in {
        document.select(searchButtonLocator).text shouldBe searchButtonWelsh
      }

      s"has a clear search link of $clearSearchLink" in {
        document.select(clearSearchLinkLocator).text shouldBe clearSearchLinkWelsh
        document
          .select(clearSearchLinkLocator)
          .attr(
            "href"
          ) shouldBe "/business-rates-property-linking/my-organisation/appoint/properties?page=1&pageSize=15&agentCode=1001&agentAppointed=NO&backLinkUrl=http%3A%2F%2Flocalhost%2Fsome-back-link&fromManageAgentJourney=true"
      }

      s"has a sortable table header of $addressHeaderSortable" in {
        document.select(addressHeaderSortableLocator).text shouldBe addressHeaderSortableWelsh
        document
          .select(addressHeaderSortableLocator)
          .attr(
            "href"
          ) shouldBe "/business-rates-property-linking/my-organisation/appoint/properties/sort?sortField=ADDRESS&page=1&pageSize=15&agentCode=1001&agentAppointed=NO&backLinkUrl=http%3A%2F%2Flocalhost%2Fsome-back-link&fromManageAgentJourney=true"
      }

      "has the correct address value in row 1" in {
        document.select(addressValue1Locator).text shouldBe "NO.2 TEST ADDRESS"
      }

      "has the correct address value in row 2" in {
        document.select(addressValue2Locator).text shouldBe ""
      }

      s"has a sortable table header of $appointedAgentsHeaderSortable" in {
        document.select(appointedAgentsHeaderSortableLocator).text shouldBe appointedAgentsHeaderSortableWelsh
        document
          .select(appointedAgentsHeaderSortableLocator)
          .attr(
            "href"
          ) shouldBe "/business-rates-property-linking/my-organisation/appoint/properties/sort?sortField=AGENT&page=1&pageSize=15&agentCode=1001&agentAppointed=NO&backLinkUrl=http%3A%2F%2Flocalhost%2Fsome-back-link&fromManageAgentJourney=true"
      }

      "has the correct appointed agent value in row 1" in {
        document.select(appointedAgentsValue1Locator).text shouldBe ""
      }

      "has the correct appointed agent value in row 2" in {
        document.select(appointedAgentsValue2Locator).text shouldBe ""
      }

      "has a checkbox with the correct type & value" in {
        document.select(checkBox1Locator).attr("type") shouldBe "checkbox"
        document.select(checkBox1Locator).attr("value") shouldBe "123456"
      }

      s"has a $continueButtonText button" in {
        document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
      }
    }
  }

  def getMyOrganisationPropertyLinksWithAgentFilteringPage(
        language: Language,
        withAgents: Boolean = false,
        withAgentsNotAppointed: Boolean = false
  ) = {
    val testSessionId = s"stubbed-${UUID.randomUUID}"
    val agentsCode = 1001L
    val agentName = "Test Agent"
    val backLinkUrl = RedirectUrl("http://localhost/some-back-link")

    val account = GroupAccount(
      id = 2L,
      groupId = ggGroupId,
      companyName = ggExternalId,
      addressId = 1,
      email = email,
      phone = phone,
      isAgent = true,
      agentCode = Some(agentsCode)
    )

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
      app.injector.instanceOf[AppointAgentSessionRepository]

    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

    val propertiesSessionData: AppointAgentToSomePropertiesSession = AppointAgentToSomePropertiesSession(
      agentAppointAction = Some(
        AgentAppointBulkAction(
          agentCode = agentsCode,
          name = agentName,
          propertyLinkIds = List("123", "321"),
          backLinkUrl = "some-back-link"
        )
      ),
      filters = FilterAppointProperties(None, None)
    )

    val ownerAuthorisation = OwnerAuthorisation(
      authorisationId = 1,
      status = "",
      submissionId = "12345",
      uarn = 1L,
      address = "Test Address",
      localAuthorityRef = "12345",
      agents = Seq(
        OwnerAuthAgent(
          authorisedPartyId = 1L,
          organisationId = 1L,
          organisationName = "Test Agent",
          agentCode = 1001
        )
      )
    )

    val ownerAuths = if (withAgents) ownerAuthorisation else ownerAuthorisation.copy(agents = Seq.empty)
    val testOwnerAuthResult = OwnerAuthResult(
      start = 1,
      size = 15,
      filterTotal = 2,
      total = 10,
      authorisations = Seq(
        ownerAuths,
        OwnerAuthorisation(
          authorisationId = 2,
          status = "",
          submissionId = "123456",
          uarn = 123L,
          address = "No.2 Test Address ",
          localAuthorityRef = "123454",
          agents = Seq.empty
        )
      )
    )

    await(mockAppointAgentSessionRepository.saveOrUpdate(propertiesSessionData))

    commonStubs

    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentListFor2017).toString())
        }
    }

    stubFor {
      get(s"/property-linking/groups/agentCode/$agentsCode")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(account).toString())
        }
    }

    stubFor {
      get(
        "/property-linking/my-organisation/agents/1001/available-property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
      ).willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult).toString())
        }
    }

    stubFor {
      get(
        "/property-linking/owner/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
      ).willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/property-links/count")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testResultCount).toString())
        }
    }

    val agentAppointedParam = if (withAgentsNotAppointed) "&agentAppointed=NO" else ""
    await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/properties?agentCode=$agentsCode&backLinkUrl=${backLinkUrl.unsafeValue}&fromManageAgentJourney=true$agentAppointedParam"
        )
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .get()
    )
  }

  def commonStubs = {
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
}
