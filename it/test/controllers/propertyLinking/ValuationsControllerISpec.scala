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

import base.ISpecBase
import models.ApiAssessment
import models.assessments.AssessmentsPageSession
import models.assessments.PreviousPage._
import org.jsoup.Jsoup
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.AssessmentsPageSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class ValuationsControllerISpec extends ISpecBase {
  val testSessionId = s"stubbed-${UUID.randomUUID}"
  lazy val mockAssessmentsPageSessionRepository: AssessmentsPageSessionRepository =
    app.injector.instanceOf[AssessmentsPageSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val propertyAddress = "ADDRESS"
  val localCouncilReference = "AMBER-VALLEY-REF"
  val futureRv = "£1,000"
  val currentRv = "£2,000"
  val previousRv = "£3,000"

  val titleText = s"$propertyAddress - Valuation Office Agency - GOV.UK"
  val captionText = "Your property"
  val agentCaptionText = "Client property: Client Name"
  val backText = "Back"
  val localCouncilReferenceText = s"Local council reference:"
  val valuationsForThisPropertyText = "Valuations for this property"
  val valuationText = "Valuation"
  val helpWithValuationText = "Help with valuation"
  val effectiveDateText = "Effective date"
  val helpWithEffectiveDateText = "Help with effective date"
  val connectionToPropertyText = "Connection to property"
  val rateableValueText = "Rateable value"
  val futureText = "FUTURE"
  val currentText = "CURRENT"
  val previousText = "PREVIOUS"
  val futureDateLinkText = "from 1 April 2026"
  val currentDateLinkText = "1 April 2023 to present"
  val futureEffectiveDateText = "1 April 2026"
  val currentEffectiveDateText = "1 April 2023"
  val previousEffectiveDateText = "1 April 2017"
  val ownerText = "Owner"
  val ownerAndOccupierText = "Owner and occupier"
  val occupierText = "Occupier"
  val onlyShowValuationsWhenOwnedText = "We only show valuations for when you owned or occupied the property."
  val otherValuationsText =
    "Valuations for other periods may be available. Find public versions of all valuations for this property."
  val findValuationsLinkText = "Find public versions of all valuations for this property"
  val noValuationsText = "There are no valuations available for this property."
  val onlyShowValuationsAgentText = "We only show valuations:"
  val agentBulletOneText = "for the rating lists your client wants you to act on"
  val agentBulletTwoText = "for when your client owned or occupied the property"
  val contactClientText = "Contact your client if you need to change which lists you can act on."
  val noRvText = "N/A"
  val helpWithNaRvText = "Help with rateable value not available (N/A)"
  val rateableValueNaWhenRemovedText =
    "The rateable value is N/A because the property has been removed from the rating list."
  val thisCanBeText = "This can be when:"
  val propertySplitOrMergedText = "the property has been split or merged with other properties"
  val addressOrDescChangedText = "the address or description has changed"
  val findNewPropertyText =
    "You may find a new property with the same or similar address that does have a rateable value."
  val findNewPropertyLinkText = "find a new property"
  val ifYouOwnText = "If you own or occupy the property, add the new property to your account if you want to:"
  val viewDetailedValuationText = "view the detailed valuation"
  val sendCheckOrChallengeText = "send a Check or Challenge case"
  val propertyRemovedText =
    "The property can also be removed from the rating list and have a rateable value N/A when it’s no longer used for business. For example, the property may be being redeveloped, be exempt or have changed to domestic use."
  val toText = "to"

  val titleTextWelsh = s"$propertyAddress - Valuation Office Agency - GOV.UK"
  val captionTextWelsh = "Eich eiddo"
  val agentCaptionTextWelsh = "Eiddo‘r cleient: Client Name"
  val backTextWelsh = "Yn ôl"
  val localCouncilReferenceTextWelsh = s"Cyfeirnod yr awdurdod lleol:"
  val valuationsForThisPropertyTextWelsh = "Prisiadau ar gyfer yr eiddo hwn"
  val valuationTextWelsh = "Prisiadau"
  val helpWithValuationTextWelsh = "Cymorth gyda Prisiadau"
  val effectiveDateTextWelsh = "Dyddiad dod i rym"
  val helpWithEffectiveDateTextWelsh = "Cymorth gyda Dyddiad dod i rym"
  val connectionToPropertyTextWelsh = "Cysylltiad a r eiddo"
  val rateableValueTextWelsh = "Gwerth ardrethol"
  val futureTextWelsh = "DYFODOL"
  val currentTextWelsh = "PRESENNOL"
  val previousTextWelsh = "BLAENOROL"
  val futureDateLinkTextWelsh = "o 1 Ebrill 2026"
  val currentDateLinkTextWelsh = "1 Ebrill 2023 i presennol"
  val futureEffectiveDateTextWelsh = "1 Ebrill 2026"
  val currentEffectiveDateTextWelsh = "1 Ebrill 2023"
  val previousEffectiveDateTextWelsh = "1 Ebrill 2017"
  val ownerTextWelsh = "Perchennog"
  val ownerAndOccupierTextWelsh = "Perchennog a meddiannydd"
  val occupierTextWelsh = "Meddiannydd"
  val onlyShowValuationsWhenOwnedTextWelsh =
    "Rydym ond yn dangos prisiadau ar gyfer yr adeg yr oeddech yn berchen ar yr eiddo neu’n ei feddiannu."
  val otherValuationsTextWelsh =
    "Mae’n bosibl bod prisiadau ar gyfer cyfnodau eraill ar gael. Dewch o hyd i fersiynau cyhoeddus o’r holl brisiadau ar gyfer yr eiddo hwn."
  val findValuationsLinkTextWelsh = "Dewch o hyd i fersiynau cyhoeddus o’r holl brisiadau ar gyfer yr eiddo hwn"
  val noValuationsTextWelsh = "Nid oes prisiadau ar gael ar gyfer yr eiddo hwn."
  val onlyShowValuationsAgentTextWelsh = "Rydym ond yn dangos prisiadau ar gyfer:"
  val agentBulletOneTextWelsh = "y rhestrau ardrethu y mae’ch cleient am i chi eu gweithredu"
  val agentBulletTwoTextWelsh = "yr adeg yr oedd eich cleient yn berchen ar yr eiddo neu’n ei feddiannu"
  val contactClientTextWelsh =
    "Cysylltwch â’ch cleient os oes angen i chi newid y rhestrau mae gennych ganiatâd i weithredu arnynt."
  val noRvTextWelsh = "Dim ar gael"
  val helpWithNaRvTextWelsh = "Cymorth gyda gwerth ardrethol sydd ddim ar gael"
  val rateableValueNaWhenRemovedTextWelsh =
    "Nid yw’r gwerth ardrethol ar gael oherwydd bod yr eiddo wedi’i dynnu oddi ar y rhestr ardrethu."
  val thisCanBeTextWelsh = "Gall hyn fod pan:"
  val propertySplitOrMergedTextWelsh = "mae’r eiddo wedi’i rannu neu ei gyfuno gydag eiddo arall"
  val addressOrDescChangedTextWelsh = "mae’r cyfeiriad neu’r disgrifiad wedi newid"
  val findNewPropertyTextWelsh =
    "Efallai byddwch yn dod o hyd i eiddo newydd gyda’r un cyfeiriad neu gyfeiriad tebyg sydd â gwerth ardrethol."
  val findNewPropertyLinkTextWelsh = "dod o hyd i eiddo newydd"
  val ifYouOwnTextWelsh =
    "Os ydych chi’n berchen ar yr eiddo neu’n ei feddiannu, ychwanegwch yr eiddo newydd at eich cyfrif os ydych am:"
  val viewDetailedValuationTextWelsh = "weld y prisiad manwl"
  val sendCheckOrChallengeTextWelsh = "anfon achos Gwirio neu Herio"
  val propertyRemovedTextWelsh =
    "Gall yr eiddo hefyd gael ei dynnu oddi ar y rhestr ardrethu a bod y gwerth ardrethol ddim ar gael pan na chaiff ei ddefnyddio mwyach ar gyfer busnes. Er enghraifft, gall yr eiddo fod yn cael ei ailddatblygu, wedi’i eithrio neu wedi’i newid i ddefnydd domestig"
  val toTextWelsh = "i"

  val findValuationsHref =
    s"http://localhost:9300/business-rates-find/valuations/start/$uarn?rtp=your_assessments&submissionId=$plSubmissionId"
  val agentFindValuationsHref =
    s"http://localhost:9300/business-rates-find/valuations/start/$uarn?rtp=client_assessments&submissionId=$plSubmissionId"
  val returnToPropertiesHref = "/business-rates-dashboard/return-to-your-properties"
  val returnToClientPropertiesHref = "/business-rates-dashboard/return-to-client-properties"
  val returnToSelectedClientPropertiesHref =
    "/business-rates-dashboard/return-to-selected-client-properties?organisationId=12345&organisationName=Test+Organisation+name"
  val homeHref = "/business-rates-dashboard/home"
  val valuationHelpHref = "http://localhost:9537/business-rates-valuation/show-help#dialog-valuationPeriod"
  val effectiveDateHelpHref = "http://localhost:9537/business-rates-valuation/show-help#dialog-effectiveDate"
  val valuationHref = s"/business-rates-property-linking/detailed/$uarn/$valuationId?submissionId=$plSubmissionId"
  val agentValuationHref =
    s"/business-rates-property-linking/detailed/$uarn/$valuationId?submissionId=$plSubmissionId&owner=false"
  val naHelpHref = "https://www.gov.uk/find-business-rates"

  val headingLocator = "#main-content > div > div > div:nth-child(1) > div > h1"
  val captionLocator = "#main-content > div > div > div:nth-child(1) > div > span"
  val backLocator = "#back-link"
  val localCouncilReferenceLocator = "#main-content > div > div > div:nth-child(1) > div > p"
  val valuationsForThisPropertyLocator = "#details-content-valuations > h2"
  val valuationTableHeadingLocator = "#assessments-table > thead > tr > th:nth-child(1)"
  val valuationHelpLinkLocator = "#assessments-table > thead > tr > th:nth-child(1) > a"
  val effectiveDateTableHeadingLocator = "#assessments-table > thead > tr > th:nth-child(2)"
  val effectiveDateHelpLinkLocator = "#assessments-table > thead > tr > th:nth-child(2) > a"
  val connectionTableHeadingLocator = "#assessments-table > thead > tr > th:nth-child(3)"
  val rvTableHeadingLocator = "#assessments-table > thead > tr > th:nth-child(4)"
  val valuationHeadingResponsive = (row: Int) => s"#assessments-table > tbody > tr:nth-child($row) > td:nth-child(1) > span"
  val valuationHeadingHelpLinkResponsive = (row: Int) => s"#assessments-table > tbody > tr:nth-child($row) > td:nth-child(1) > span > a"
  val valuationTagLocator = (row: Int) => s"#assessments-table > tbody > tr:nth-child($row) > td:nth-child(1) > p > strong"
  val greyedOutDatesLocator = (row: Int) => s"#assessments-table > tbody > tr:nth-child($row) > td:nth-child(1) > p:nth-child(3)"
  val valuationDatesLinkLocator = (row: Int) => s"#assessments-table > tbody > tr:nth-child($row) > td:nth-child(1) > p > a"
  val valuationEffectiveDateLocator = (row: Int) => s"#assessments-table > tbody > tr:nth-child($row) > td:nth-child(2) > p"
  val valuationConnectionLocator = (row: Int) => s"#assessments-table > tbody > tr:nth-child($row) > td:nth-child(3) > p"
  val valuationRvLocator = (row: Int) => s"#assessments-table > tbody > tr:nth-child($row) > td:nth-child(4) > p"
  val onlyShowValuationsLocator = "#only-show"
  val otherValuationsLocator = "#other-periods"
  val findValuationsLinkLocator = "#explanatory-link"
  val agentBulletOneLocator = "#reasons-list > li:nth-child(1)"
  val agentBulletTwoLocator = "#reasons-list > li:nth-child(2)"
  val contactClientLocator = "#contact-text"
  val noValuationsLocator = "#no-valuation-text"
  val helpWithNaRvLocator = "#main-content > div > div > details:nth-child(2) > summary > span"
  val rateableValueNaWhenRemovedLocator =
    "#main-content > div > div > details:nth-child(2) > div > div > p:nth-child(1)"
  val thisCanBeLocator = "#main-content > div > div > details:nth-child(2) > div > div > p:nth-child(2)"
  val propertySplitOrMergedLocator =
    "#main-content > div > div > details:nth-child(2) > div > div > ul:nth-child(3) > li:nth-child(1)"
  val addressOrDescChangedLocator =
    "#main-content > div > div > details:nth-child(2) > div > div > ul:nth-child(3) > li:nth-child(2)"
  val findNewPropertyLocator = "#main-content > div > div > details:nth-child(2) > div > div > p.govuk-body"
  val findNewPropertyLinkLocator = "#naHelpLink"
  val ifYouOwnLocator = "#main-content > div > div > details:nth-child(2) > div > div > p:nth-child(5)"
  val viewDetailedValuationLocator =
    "#main-content > div > div > details:nth-child(2) > div > div > ul:nth-child(6) > li:nth-child(1)"
  val sendCheckOrChallengeLocator =
    "#main-content > div > div > details:nth-child(2) > div > div > ul:nth-child(6) > li:nth-child(2)"
  val propertyRemovedLocator = "#main-content > div > div > details:nth-child(2) > div > div > p:nth-child(7)"

  "ValuationsController valuations method" should {
    // English - IP
    "display the correct page for an IP in English - [OWNER with DRAFT, CURRENT AND PREVIOUS valuations]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = false,
        connectionToProperty = "OWNER",
        assessments = List(draftApiAssessment, currentApiAssessment, previousApiAssessment),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, English, userIsAgent = false, hasAssessments = true, previousPage = Dashboard)
      testFutureValuation(res, English, row = 1, connection = "OWNER", valuationHref = valuationHref)
      testCurrentValuation(res, English, row = 2, connection = "OWNER", valuationHref = valuationHref)
      testPreviousValuation(res, English, row = 3, connection = "OWNER", valuationHref = valuationHref)
    }

    "display the correct page for an IP in English - [OCCUPIER with CURRENT AND PREVIOUS valuations, when the CURRENT valuation has NO RV]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = false,
        connectionToProperty = "OCCUPIER",
        assessments = List(currentApiAssessment.copy(rateableValue = None), previousApiAssessment),
        previousPage = MyProperties
      )
      testAssessmentsPage(res, English, userIsAgent = false, hasAssessments = true, previousPage = MyProperties)
      testCurrentValuation(
        res,
        English,
        row = 1,
        connection = "OCCUPIER",
        valuationHref = valuationHref,
        noRateableValue = true
      )
      testPreviousValuation(res, English, row = 2, connection = "OCCUPIER", valuationHref = valuationHref)
      testNotApplicableExpandable(res, English)
    }

    "display the correct page for an IP in English - [OWNER AND OCCUPIER with DRAFT and CURRENT valuation]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = false,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(draftApiAssessment, currentApiAssessment),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, English, userIsAgent = false, hasAssessments = true, previousPage = Dashboard)
      testFutureValuation(res, English, row = 1, connection = "OWNER_OCCUPIER", valuationHref = valuationHref)
      testCurrentValuation(res, English, row = 2, connection = "OWNER_OCCUPIER", valuationHref = valuationHref)
    }

    "display the correct page for an IP in English - [OWNER with only a DRAFT valuation]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = false,
        connectionToProperty = "OWNER",
        assessments = List(draftApiAssessment),
        previousPage = MyProperties
      )
      testAssessmentsPage(res, English, userIsAgent = false, hasAssessments = true, previousPage = MyProperties)
      testFutureValuation(res, English, row = 1, connection = "OWNER", valuationHref = valuationHref)
    }

    "display the correct page for an IP in English - [OCCUPIER with only a CURRENT valuation" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = false,
        connectionToProperty = "OCCUPIER",
        assessments = List(currentApiAssessment),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, English, userIsAgent = false, hasAssessments = true, previousPage = Dashboard)
      testCurrentValuation(res, English, row = 1, connection = "OCCUPIER", valuationHref = valuationHref)
    }

    "display the correct page for an IP in English - [OWNER AND OCCUPIER with only a PREVIOUS valuation (that doesn't have a currentToDate)]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = false,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(previousApiAssessment.copy(currentToDate = None)),
        previousPage = MyProperties
      )
      testAssessmentsPage(res, English, userIsAgent = false, hasAssessments = true, previousPage = MyProperties)
      testPreviousValuation(res, English, row = 1, connection = "OWNER_OCCUPIER", valuationHref = valuationHref)
    }

    "display the correct page for an IP in English - [OWNER AND OCCUPIER with NO valuations]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = false,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, English, userIsAgent = false, hasAssessments = false, previousPage = Dashboard)
    }

    "display the correct page for an IP in English - [OWNER with 3 valuations that don't have an allowed action of VIEW_DETAILED_VALUATION]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = false,
        connectionToProperty = "OWNER",
        assessments = List(
          draftApiAssessment.copy(allowedActions = List()),
          currentApiAssessment.copy(allowedActions = List()),
          previousApiAssessment.copy(allowedActions = List())
        ),
        previousPage = MyProperties
      )
      testAssessmentsPage(res, English, userIsAgent = false, hasAssessments = false, previousPage = MyProperties)
    }

    "display the correct page for an IP in English - [OCCUPIER with 3 PREVIOUS valuations that are sorted by CurrentFromDate]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = false,
        connectionToProperty = "OCCUPIER",
        assessments = List(
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2017, 4, 1))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2019, 4, 1))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2018, 2, 25))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2018, 7, 10)))
        ),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, English, userIsAgent = false, hasAssessments = true, previousPage = Dashboard)
      testPreviousValuation(
        res,
        English,
        row = 1,
        connection = "OCCUPIER",
        valuationHref = valuationHref,
        currentFromDate = LocalDate.of(2019, 4, 1)
      )
      testPreviousValuation(
        res,
        English,
        row = 2,
        connection = "OCCUPIER",
        valuationHref = valuationHref,
        currentFromDate = LocalDate.of(2018, 7, 10)
      )
      testPreviousValuation(
        res,
        English,
        row = 3,
        connection = "OCCUPIER",
        valuationHref = valuationHref,
        currentFromDate = LocalDate.of(2018, 2, 25)
      )
      testPreviousValuation(
        res,
        English,
        row = 4,
        connection = "OCCUPIER",
        valuationHref = valuationHref,
        currentFromDate = LocalDate.of(2017, 4, 1)
      )
    }

    // English - Agent
    "display the correct page for an AGENT in English - [OWNER with DRAFT, CURRENT AND PREVIOUS valuations]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = true,
        connectionToProperty = "OWNER",
        assessments = List(draftApiAssessment, currentApiAssessment, previousApiAssessment),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, English, userIsAgent = true, hasAssessments = true, previousPage = Dashboard)
      testFutureValuation(res, English, row = 1, connection = "OWNER", valuationHref = agentValuationHref)
      testCurrentValuation(res, English, row = 2, connection = "OWNER", valuationHref = agentValuationHref)
      testPreviousValuation(res, English, row = 3, connection = "OWNER", valuationHref = agentValuationHref)
    }

    "display the correct page for an AGENT in English - [OCCUPIER with CURRENT AND PREVIOUS valuations, when the CURRENT valuation has NO RV]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = true,
        connectionToProperty = "OCCUPIER",
        assessments = List(currentApiAssessment.copy(rateableValue = None), previousApiAssessment),
        previousPage = AllClients
      )
      testAssessmentsPage(res, English, userIsAgent = true, hasAssessments = true, previousPage = AllClients)
      testCurrentValuation(
        res,
        English,
        row = 1,
        connection = "OCCUPIER",
        valuationHref = agentValuationHref,
        noRateableValue = true
      )
      testPreviousValuation(res, English, row = 2, connection = "OCCUPIER", valuationHref = agentValuationHref)
      testNotApplicableExpandable(res, English)
    }

    "display the correct page for an AGENT in English - [OWNER AND OCCUPIER with DRAFT and CURRENT valuation]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = true,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(draftApiAssessment, currentApiAssessment),
        previousPage = SelectedClient
      )
      testAssessmentsPage(res, English, userIsAgent = true, hasAssessments = true, previousPage = SelectedClient)
      testFutureValuation(res, English, row = 1, connection = "OWNER_OCCUPIER", valuationHref = agentValuationHref)
      testCurrentValuation(res, English, row = 2, connection = "OWNER_OCCUPIER", valuationHref = agentValuationHref)
    }

    "display the correct page for an AGENT in English - [OWNER with only a DRAFT valuation]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = true,
        connectionToProperty = "OWNER",
        assessments = List(draftApiAssessment),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, English, userIsAgent = true, hasAssessments = true, previousPage = Dashboard)
      testFutureValuation(res, English, row = 1, connection = "OWNER", valuationHref = agentValuationHref)
    }

    "display the correct page for an AGENT in English - [OCCUPIER with only a CURRENT valuation]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = true,
        connectionToProperty = "OCCUPIER",
        assessments = List(currentApiAssessment),
        previousPage = AllClients
      )
      testAssessmentsPage(res, English, userIsAgent = true, hasAssessments = true, previousPage = AllClients)
      testCurrentValuation(res, English, row = 1, connection = "OCCUPIER", valuationHref = agentValuationHref)
    }

    "display the correct page for an AGENT in English - [OWNER AND OCCUPIER with only a PREVIOUS valuation (that doesn't have a currentToDate)]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = true,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(previousApiAssessment.copy(currentToDate = None)),
        previousPage = SelectedClient
      )
      testAssessmentsPage(res, English, userIsAgent = true, hasAssessments = true, previousPage = SelectedClient)
      testPreviousValuation(res, English, row = 1, connection = "OWNER_OCCUPIER", valuationHref = agentValuationHref)
    }

    "display the correct page for an AGENT in English - [OWNER AND OCCUPIER with NO valuations]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = true,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, English, userIsAgent = true, hasAssessments = false, previousPage = Dashboard)
    }

    "display the correct page for an AGENT in English - [OWNER with 3 valuations that don't have an allowed action of VIEW_DETAILED_VALUATION]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = true,
        connectionToProperty = "OWNER",
        assessments = List(
          draftApiAssessment.copy(allowedActions = List()),
          currentApiAssessment.copy(allowedActions = List()),
          previousApiAssessment.copy(allowedActions = List())
        ),
        previousPage = AllClients
      )
      testAssessmentsPage(res, English, userIsAgent = true, hasAssessments = false, previousPage = AllClients)
    }

    "display the correct page for an AGENT in English - [OCCUPIER with 3 PREVIOUS valuations that are sorted by CurrentFromDate]" which {
      lazy val res = getAssessmentsPage(
        English,
        userIsAgent = true,
        connectionToProperty = "OCCUPIER",
        assessments = List(
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2017, 4, 1))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2019, 4, 1))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2018, 2, 25))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2018, 7, 10)))
        ),
        previousPage = SelectedClient
      )
      testAssessmentsPage(res, English, userIsAgent = true, hasAssessments = true, previousPage = SelectedClient)
      testPreviousValuation(
        res,
        English,
        row = 1,
        connection = "OCCUPIER",
        valuationHref = agentValuationHref,
        currentFromDate = LocalDate.of(2019, 4, 1)
      )
      testPreviousValuation(
        res,
        English,
        row = 2,
        connection = "OCCUPIER",
        valuationHref = agentValuationHref,
        currentFromDate = LocalDate.of(2018, 7, 10)
      )
      testPreviousValuation(
        res,
        English,
        row = 3,
        connection = "OCCUPIER",
        valuationHref = agentValuationHref,
        currentFromDate = LocalDate.of(2018, 2, 25)
      )
      testPreviousValuation(
        res,
        English,
        row = 4,
        connection = "OCCUPIER",
        valuationHref = agentValuationHref,
        currentFromDate = LocalDate.of(2017, 4, 1)
      )
    }

    // Welsh - IP
    "display the correct page for an IP in Welsh - [OWNER with DRAFT, CURRENT AND PREVIOUS valuations]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = false,
        connectionToProperty = "OWNER",
        assessments = List(draftApiAssessment, currentApiAssessment, previousApiAssessment),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, Welsh, userIsAgent = false, hasAssessments = true, previousPage = Dashboard)
      testFutureValuation(res, Welsh, row = 1, connection = "OWNER", valuationHref = valuationHref)
      testCurrentValuation(res, Welsh, row = 2, connection = "OWNER", valuationHref = valuationHref)
      testPreviousValuation(res, Welsh, row = 3, connection = "OWNER", valuationHref = valuationHref)
    }

    "display the correct page for an IP in Welsh - [OCCUPIER with CURRENT AND PREVIOUS valuations, when the CURRENT valuation has NO RV]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = false,
        connectionToProperty = "OCCUPIER",
        assessments = List(currentApiAssessment.copy(rateableValue = None), previousApiAssessment),
        previousPage = MyProperties
      )
      testAssessmentsPage(res, Welsh, userIsAgent = false, hasAssessments = true, previousPage = MyProperties)
      testCurrentValuation(
        res,
        Welsh,
        row = 1,
        connection = "OCCUPIER",
        valuationHref = valuationHref,
        noRateableValue = true
      )
      testPreviousValuation(res, Welsh, row = 2, connection = "OCCUPIER", valuationHref = valuationHref)
      testNotApplicableExpandable(res, Welsh)
    }

    "display the correct page for an IP in Welsh - [OWNER AND OCCUPIER with DRAFT and CURRENT valuation]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = false,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(draftApiAssessment, currentApiAssessment),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, Welsh, userIsAgent = false, hasAssessments = true, previousPage = Dashboard)
      testFutureValuation(res, Welsh, row = 1, connection = "OWNER_OCCUPIER", valuationHref = valuationHref)
      testCurrentValuation(res, Welsh, row = 2, connection = "OWNER_OCCUPIER", valuationHref = valuationHref)
    }

    "display the correct page for an IP in Welsh - [OWNER with only a DRAFT valuation]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = false,
        connectionToProperty = "OWNER",
        assessments = List(draftApiAssessment),
        previousPage = MyProperties
      )
      testAssessmentsPage(res, Welsh, userIsAgent = false, hasAssessments = true, previousPage = MyProperties)
      testFutureValuation(res, Welsh, row = 1, connection = "OWNER", valuationHref = valuationHref)
    }

    "display the correct page for an IP in Welsh - [OCCUPIER with only a CURRENT valuation" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = false,
        connectionToProperty = "OCCUPIER",
        assessments = List(currentApiAssessment),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, Welsh, userIsAgent = false, hasAssessments = true, previousPage = Dashboard)
      testCurrentValuation(res, Welsh, row = 1, connection = "OCCUPIER", valuationHref = valuationHref)
    }

    "display the correct page for an IP in Welsh - [OWNER AND OCCUPIER with only a PREVIOUS valuation (that doesn't have a currentToDate)]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = false,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(previousApiAssessment.copy(currentToDate = None)),
        previousPage = MyProperties
      )
      testAssessmentsPage(res, Welsh, userIsAgent = false, hasAssessments = true, previousPage = MyProperties)
      testPreviousValuation(res, Welsh, row = 1, connection = "OWNER_OCCUPIER", valuationHref = valuationHref)
    }

    "display the correct page for an IP in Welsh - [OWNER AND OCCUPIER with NO valuations]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = false,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, Welsh, userIsAgent = false, hasAssessments = false, previousPage = Dashboard)
    }

    "display the correct page for an IP in Welsh - [OWNER with 3 valuations that don't have an allowed action of VIEW_DETAILED_VALUATION]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = false,
        connectionToProperty = "OWNER",
        assessments = List(
          draftApiAssessment.copy(allowedActions = List()),
          currentApiAssessment.copy(allowedActions = List()),
          previousApiAssessment.copy(allowedActions = List())
        ),
        previousPage = MyProperties
      )
      testAssessmentsPage(res, Welsh, userIsAgent = false, hasAssessments = false, previousPage = MyProperties)
    }

    "display the correct page for an IP in Welsh - [OCCUPIER with 3 PREVIOUS valuations that are sorted by CurrentFromDate]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = false,
        connectionToProperty = "OCCUPIER",
        assessments = List(
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2017, 4, 1))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2019, 4, 1))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2018, 2, 25))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2018, 7, 10)))
        ),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, Welsh, userIsAgent = false, hasAssessments = true, previousPage = Dashboard)
      testPreviousValuation(
        res,
        Welsh,
        row = 1,
        connection = "OCCUPIER",
        valuationHref = valuationHref,
        currentFromDate = LocalDate.of(2019, 4, 1)
      )
      testPreviousValuation(
        res,
        Welsh,
        row = 2,
        connection = "OCCUPIER",
        valuationHref = valuationHref,
        currentFromDate = LocalDate.of(2018, 7, 10)
      )
      testPreviousValuation(
        res,
        Welsh,
        row = 3,
        connection = "OCCUPIER",
        valuationHref = valuationHref,
        currentFromDate = LocalDate.of(2018, 2, 25)
      )
      testPreviousValuation(
        res,
        Welsh,
        row = 4,
        connection = "OCCUPIER",
        valuationHref = valuationHref,
        currentFromDate = LocalDate.of(2017, 4, 1)
      )
    }

    // Welsh - Agent
    "display the correct page for an AGENT in Welsh - [OWNER with DRAFT, CURRENT AND PREVIOUS valuations]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = true,
        connectionToProperty = "OWNER",
        assessments = List(draftApiAssessment, currentApiAssessment, previousApiAssessment),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, Welsh, userIsAgent = true, hasAssessments = true, previousPage = Dashboard)
      testFutureValuation(res, Welsh, row = 1, connection = "OWNER", valuationHref = agentValuationHref)
      testCurrentValuation(res, Welsh, row = 2, connection = "OWNER", valuationHref = agentValuationHref)
      testPreviousValuation(res, Welsh, row = 3, connection = "OWNER", valuationHref = agentValuationHref)
    }

    "display the correct page for an AGENT in Welsh - [OCCUPIER with CURRENT AND PREVIOUS valuations, when the CURRENT valuation has NO RV]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = true,
        connectionToProperty = "OCCUPIER",
        assessments = List(currentApiAssessment.copy(rateableValue = None), previousApiAssessment),
        previousPage = AllClients
      )
      testAssessmentsPage(res, Welsh, userIsAgent = true, hasAssessments = true, previousPage = AllClients)
      testCurrentValuation(
        res,
        Welsh,
        row = 1,
        connection = "OCCUPIER",
        valuationHref = agentValuationHref,
        noRateableValue = true
      )
      testPreviousValuation(res, Welsh, row = 2, connection = "OCCUPIER", valuationHref = agentValuationHref)
      testNotApplicableExpandable(res, Welsh)
    }

    "display the correct page for an AGENT in Welsh - [OWNER AND OCCUPIER with DRAFT and CURRENT valuation]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = true,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(draftApiAssessment, currentApiAssessment),
        previousPage = SelectedClient
      )
      testAssessmentsPage(res, Welsh, userIsAgent = true, hasAssessments = true, previousPage = SelectedClient)
      testFutureValuation(res, Welsh, row = 1, connection = "OWNER_OCCUPIER", valuationHref = agentValuationHref)
      testCurrentValuation(res, Welsh, row = 2, connection = "OWNER_OCCUPIER", valuationHref = agentValuationHref)
    }

    "display the correct page for an AGENT in Welsh - [OWNER with only a DRAFT valuation]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = true,
        connectionToProperty = "OWNER",
        assessments = List(draftApiAssessment),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, Welsh, userIsAgent = true, hasAssessments = true, previousPage = Dashboard)
      testFutureValuation(res, Welsh, row = 1, connection = "OWNER", valuationHref = agentValuationHref)
    }

    "display the correct page for an AGENT in Welsh - [OCCUPIER with only a CURRENT valuation]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = true,
        connectionToProperty = "OCCUPIER",
        assessments = List(currentApiAssessment),
        previousPage = AllClients
      )
      testAssessmentsPage(res, Welsh, userIsAgent = true, hasAssessments = true, previousPage = AllClients)
      testCurrentValuation(res, Welsh, row = 1, connection = "OCCUPIER", valuationHref = agentValuationHref)
    }

    "display the correct page for an AGENT in Welsh - [OWNER AND OCCUPIER with only a PREVIOUS valuation (that doesn't have a currentToDate)]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = true,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(previousApiAssessment.copy(currentToDate = None)),
        previousPage = SelectedClient
      )
      testAssessmentsPage(res, Welsh, userIsAgent = true, hasAssessments = true, previousPage = SelectedClient)
      testPreviousValuation(res, Welsh, row = 1, connection = "OWNER_OCCUPIER", valuationHref = agentValuationHref)
    }

    "display the correct page for an AGENT in Welsh - [OWNER AND OCCUPIER with NO valuations]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = true,
        connectionToProperty = "OWNER_OCCUPIER",
        assessments = List(),
        previousPage = Dashboard
      )
      testAssessmentsPage(res, Welsh, userIsAgent = true, hasAssessments = false, previousPage = Dashboard)
    }

    "display the correct page for an AGENT in Welsh - [OWNER with 3 valuations that don't have an allowed action of VIEW_DETAILED_VALUATION]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = true,
        connectionToProperty = "OWNER",
        assessments = List(
          draftApiAssessment.copy(allowedActions = List()),
          currentApiAssessment.copy(allowedActions = List()),
          previousApiAssessment.copy(allowedActions = List())
        ),
        previousPage = AllClients
      )
      testAssessmentsPage(res, Welsh, userIsAgent = true, hasAssessments = false, previousPage = AllClients)
    }

    "display the correct page for an AGENT in Welsh - [OCCUPIER with 3 PREVIOUS valuations that are sorted by CurrentFromDate]" which {
      lazy val res = getAssessmentsPage(
        Welsh,
        userIsAgent = true,
        connectionToProperty = "OCCUPIER",
        assessments = List(
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2017, 4, 1))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2019, 4, 1))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2018, 2, 25))),
          previousApiAssessment.copy(currentFromDate = Some(LocalDate.of(2018, 7, 10)))
        ),
        previousPage = SelectedClient
      )
      testAssessmentsPage(res, Welsh, userIsAgent = true, hasAssessments = true, previousPage = SelectedClient)
      testPreviousValuation(
        res,
        Welsh,
        row = 1,
        connection = "OCCUPIER",
        valuationHref = agentValuationHref,
        currentFromDate = LocalDate.of(2019, 4, 1)
      )
      testPreviousValuation(
        res,
        Welsh,
        row = 2,
        connection = "OCCUPIER",
        valuationHref = agentValuationHref,
        currentFromDate = LocalDate.of(2018, 7, 10)
      )
      testPreviousValuation(
        res,
        Welsh,
        row = 3,
        connection = "OCCUPIER",
        valuationHref = agentValuationHref,
        currentFromDate = LocalDate.of(2018, 2, 25)
      )
      testPreviousValuation(
        res,
        Welsh,
        row = 4,
        connection = "OCCUPIER",
        valuationHref = agentValuationHref,
        currentFromDate = LocalDate.of(2017, 4, 1)
      )
    }

  }

  private def testAssessmentsPage(
        res: => WSResponse,
        language: Language,
        userIsAgent: Boolean,
        hasAssessments: Boolean,
        previousPage: PreviousPage
  ): Unit = {
    val title = if (language == English) titleText else titleTextWelsh
    val caption = if (language == English) captionText else captionTextWelsh
    val agentCaption = if (language == English) agentCaptionText else agentCaptionTextWelsh
    val expCaption = if (userIsAgent) agentCaption else caption
    val back = if (language == English) backText else backTextWelsh
    val localCouncilReference = if (language == English) localCouncilReferenceText else localCouncilReferenceTextWelsh
    val expLocalCouncilReference =
      if (!hasAssessments && !userIsAgent) localCouncilReference else s"$localCouncilReference $localAuthorityRef"
    val valuationsForThisProperty =
      if (language == English) valuationsForThisPropertyText else valuationsForThisPropertyTextWelsh
    val valuation = if (language == English) valuationText else valuationTextWelsh
    val helpWithValuation = if (language == English) helpWithValuationText else helpWithValuationTextWelsh
    val effectiveDate = if (language == English) effectiveDateText else effectiveDateTextWelsh
    val helpWithEffectiveDate = if (language == English) helpWithEffectiveDateText else helpWithEffectiveDateTextWelsh
    val connectionToProperty = if (language == English) connectionToPropertyText else connectionToPropertyTextWelsh
    val rateableValue = if (language == English) rateableValueText else rateableValueTextWelsh
    val onlyShowValuationsWhenOwned =
      if (language == English) onlyShowValuationsWhenOwnedText else onlyShowValuationsWhenOwnedTextWelsh
    val otherValuations = if (language == English) otherValuationsText else otherValuationsTextWelsh
    val findValuationsLink = if (language == English) findValuationsLinkText else findValuationsLinkTextWelsh
    val noValuations = if (language == English) noValuationsText else noValuationsTextWelsh
    val onlyShowValuationsAgent =
      if (language == English) onlyShowValuationsAgentText else onlyShowValuationsAgentTextWelsh
    val agentBulletOne = if (language == English) agentBulletOneText else agentBulletOneTextWelsh
    val agentBulletTwo = if (language == English) agentBulletTwoText else agentBulletTwoTextWelsh
    val contactClient = if (language == English) contactClientText else contactClientTextWelsh
    val expFindValuationsHref = if (userIsAgent) agentFindValuationsHref else findValuationsHref
    val expBackHref = previousPage match {
      case Dashboard      => homeHref
      case AllClients     => returnToClientPropertiesHref
      case SelectedClient => returnToSelectedClientPropertiesHref
      case _              => returnToPropertiesHref
    }

    lazy val page = Jsoup.parse(res.body)

    s"has a status of 200 OK" in {
      res.status shouldBe OK
    }

    s"has a title of $titleText in $language" in {
      page.title() shouldBe title
    }

    s"has a heading $propertyAddress in $language" in {
      page.select(headingLocator).text() shouldBe propertyAddress
    }

    s"has a caption of $expCaption in $language" in {
      page.select(captionLocator).text() shouldBe expCaption
    }

    s"has a $backText link with the correct href back to the $previousPage in $language" in {
      page.select(backLocator).text() shouldBe back
      page.select(backLocator).attr("href") shouldBe expBackHref
    }

    s"has text of $localCouncilReferenceText in $language" in {
      page.select(localCouncilReferenceLocator).text() shouldBe expLocalCouncilReference
    }

    s"has a subheading of $valuationsForThisPropertyText in $language" in {
      page.select(valuationsForThisPropertyLocator).text() shouldBe valuationsForThisProperty
    }

    if (hasAssessments) {
      s"has a $valuationText table heading with help link in $language" in {
        page.select(valuationTableHeadingLocator).text() shouldBe s"$valuation $helpWithValuation"
        page.select(valuationHelpLinkLocator).attr("href") shouldBe valuationHelpHref
      }

      s"has a $effectiveDateText table heading with help link in $language" in {
        page.select(effectiveDateTableHeadingLocator).text() shouldBe s"$effectiveDate $helpWithEffectiveDate"
        page.select(effectiveDateHelpLinkLocator).attr("href") shouldBe effectiveDateHelpHref
      }

      s"has a $connectionToPropertyText table heading in $language" in {
        page.select(connectionTableHeadingLocator).text() shouldBe connectionToProperty
      }

      s"has a $rateableValueText table heading in $language" in {
        page.select(rvTableHeadingLocator).text() shouldBe rateableValue
      }

      s"has responsive heading in $language (assessments)" in {

        page.select(valuationHeadingResponsive(1)).text() shouldBe s"$valuation $helpWithValuation"
        page.select(valuationHeadingHelpLinkResponsive(1)).attr("href") shouldBe valuationHelpHref

        page.select(effectiveDateTableHeadingLocator).text() shouldBe s"$effectiveDate $helpWithEffectiveDate"
        page.select(effectiveDateHelpLinkLocator).attr("href") shouldBe effectiveDateHelpHref

        page.select(connectionTableHeadingLocator).text() shouldBe connectionToProperty

        page.select(rvTableHeadingLocator).text() shouldBe rateableValue
      }

    } else
      s"has text of $noValuationsText in $language" in {
        page.select(noValuationsLocator).text() shouldBe noValuations
      }

    if (userIsAgent) {
      s"has text of $onlyShowValuationsAgentText with two bullets of $agentBulletOneText and $agentBulletTwoText in $language" in {
        page.select(onlyShowValuationsLocator).text() shouldBe onlyShowValuationsAgent
        page.select(agentBulletOneLocator).text() shouldBe agentBulletOne
        page.select(agentBulletTwoLocator).text() shouldBe agentBulletTwo
      }

      s"has text of $contactClientText in $language" in {
        page.select(contactClientLocator).text() shouldBe contactClient
      }

    } else
      s"has text of $onlyShowValuationsWhenOwnedText in $language" in {
        page.select(onlyShowValuationsLocator).text() shouldBe onlyShowValuationsWhenOwned
      }

    s"has text of $otherValuationsText $findValuationsLinkText with the correct href in $language" in {
      val user = if (userIsAgent) "agent" else "owner"
      page.select(otherValuationsLocator).text() shouldBe otherValuations
      page.select(findValuationsLinkLocator).text() shouldBe findValuationsLink
      page.select(findValuationsLinkLocator).attr("href") shouldBe expFindValuationsHref
    }
  }

  private def testFutureValuation(
        res: => WSResponse,
        language: Language,
        row: Int,
        connection: String,
        valuationHref: String
  ): Unit = {
    lazy val page = Jsoup.parse(res.body)
    val future = if (language == English) futureText else futureTextWelsh
    val futureDateLink = if (language == English) futureDateLinkText else futureDateLinkTextWelsh
    val futureEffectiveDate = if (language == English) futureEffectiveDateText else futureEffectiveDateTextWelsh
    val owner = if (language == English) ownerText else ownerTextWelsh
    val ownerAndOccupier = if (language == English) ownerAndOccupierText else ownerAndOccupierTextWelsh
    val occupier = if (language == English) occupierText else occupierTextWelsh
    val expConnection = connection match {
      case "OWNER"          => owner
      case "OCCUPIER"       => occupier
      case "OWNER_OCCUPIER" => ownerAndOccupier
    }
    val valuation = if (language == English) valuationText else valuationTextWelsh
    val helpWithValuation = if (language == English) helpWithValuationText else helpWithValuationTextWelsh
    val effectiveDate = if (language == English) effectiveDateText else effectiveDateTextWelsh
    val helpWithEffectiveDate = if (language == English) helpWithEffectiveDateText else helpWithEffectiveDateTextWelsh
    val connectionToProperty = if (language == English) connectionToPropertyText else connectionToPropertyTextWelsh
    val rateableValue = if (language == English) rateableValueText else rateableValueTextWelsh

    s"has a $futureText tag in row $row for the draft valuation in $language" in {
      page.select(valuationTagLocator(row)).text() shouldBe future
    }

    s"has $futureDateLink displayed as a link to the detailed valuation in row $row in $language" in {
      page.select(valuationDatesLinkLocator(row)).text() shouldBe futureDateLink
      page.select(valuationDatesLinkLocator(row)).attr("href") shouldBe valuationHref
    }

    s"has an effective date of $futureEffectiveDateText in row $row for the draft valuation in $language" in {
      page.select(valuationEffectiveDateLocator(1)).text() shouldBe futureEffectiveDate
    }

    s"has a connection of $connection in row $row for the draft valuation in $language" in {
      page.select(valuationConnectionLocator(1)).text() shouldBe expConnection
    }

    s"has a rateable value of $futureRv in row $row for the draft valuation in $language" in {
      page.select(valuationRvLocator(1)).text() shouldBe futureRv
    }

    s"has responsive heading in $language (Future)" in {

        page.select(valuationHeadingResponsive(row)).text() shouldBe s"$valuation $helpWithValuation"
        page.select(valuationHeadingHelpLinkResponsive(row)).attr("href") shouldBe valuationHelpHref

        page.select(effectiveDateTableHeadingLocator).text() shouldBe s"$effectiveDate $helpWithEffectiveDate"
        page.select(effectiveDateHelpLinkLocator).attr("href") shouldBe effectiveDateHelpHref

        page.select(connectionTableHeadingLocator).text() shouldBe connectionToProperty

        page.select(rvTableHeadingLocator).text() shouldBe rateableValue
    }
  }

  private def testCurrentValuation(
        res: => WSResponse,
        language: Language,
        row: Int,
        connection: String,
        valuationHref: String,
        noRateableValue: Boolean = false
  ): Unit = {
    lazy val page = Jsoup.parse(res.body)
    val current = if (language == English) currentText else currentTextWelsh
    val currentDateLink = if (language == English) currentDateLinkText else currentDateLinkTextWelsh
    val currentEffectiveDate = if (language == English) currentEffectiveDateText else currentEffectiveDateTextWelsh
    val owner = if (language == English) ownerText else ownerTextWelsh
    val ownerAndOccupier = if (language == English) ownerAndOccupierText else ownerAndOccupierTextWelsh
    val occupier = if (language == English) occupierText else occupierTextWelsh
    val noRv = if (language == English) noRvText else noRvTextWelsh
    val expRv = if (noRateableValue) noRv else currentRv
    val expConnection = connection match {
      case "OWNER"          => owner
      case "OCCUPIER"       => occupier
      case "OWNER_OCCUPIER" => ownerAndOccupier
    }

    s"has a $currentText tag in row $row for the current valuation in $language" in {
      page.select(valuationTagLocator(row)).text() shouldBe current
    }

    if (noRateableValue)
      s"has greyed out dates of the valuation ($currentDateLink) in row $row in $language" in {
        page.select(greyedOutDatesLocator(row)).text() shouldBe currentDateLink
      }
    else
      s"has $currentDateLink displayed as a link to the detailed valuation in row $row in $language" in {
        page.select(valuationDatesLinkLocator(row)).text() shouldBe currentDateLink
        page.select(valuationDatesLinkLocator(row)).attr("href") shouldBe valuationHref
      }

    s"has an effective date of $currentEffectiveDateText in row $row for the current valuation in $language" in {
      page.select(valuationEffectiveDateLocator(row)).text() shouldBe currentEffectiveDate
    }

    s"has a connection of $connection in row $row for the current valuation in $language" in {
      page.select(valuationConnectionLocator(row)).text() shouldBe expConnection
    }

    s"has a rateable value of $expRv in row $row for the current valuation in $language" in {
      page.select(valuationRvLocator(row)).text() shouldBe expRv
    }
  }

  private def testPreviousValuation(
        res: => WSResponse,
        language: Language,
        row: Int,
        connection: String,
        valuationHref: String,
        currentFromDate: LocalDate = LocalDate.of(2017, 4, 1),
        currentToDate: LocalDate = LocalDate.of(2023, 3, 31)
  ): Unit = {
    def formatDate(language: Language, date: LocalDate): String = {
      val welshMonths = Map(
        1  -> "Ionawr",
        2  -> "Chwefror",
        3  -> "Mawrth",
        4  -> "Ebrill",
        5  -> "Mai",
        6  -> "Mehefin",
        7  -> "Gorffennaf",
        8  -> "Awst",
        9  -> "Medi",
        10 -> "Hydref",
        11 -> "Tachwedd",
        12 -> "Rhagfyr"
      )

      val day = date.getDayOfMonth
      val month =
        if (language == Welsh) welshMonths(date.getMonthValue) else date.format(DateTimeFormatter.ofPattern("MMMM"))
      val year = date.getYear
      s"$day $month $year"
    }

    lazy val page = Jsoup.parse(res.body)
    val to = if (language == English) toText else toTextWelsh
    val firstDate = formatDate(language, currentFromDate)
    val secondDate = formatDate(language, currentToDate)
    val previous = if (language == English) previousText else previousTextWelsh
    val previousDateLink = s"$firstDate $to $secondDate"
    val previousEffectiveDate = if (language == English) previousEffectiveDateText else previousEffectiveDateTextWelsh
    val owner = if (language == English) ownerText else ownerTextWelsh
    val ownerAndOccupier = if (language == English) ownerAndOccupierText else ownerAndOccupierTextWelsh
    val occupier = if (language == English) occupierText else occupierTextWelsh
    val expConnection = connection match {
      case "OWNER"          => owner
      case "OCCUPIER"       => occupier
      case "OWNER_OCCUPIER" => ownerAndOccupier
    }
    val valuation = if (language == English) valuationText else valuationTextWelsh
    val helpWithValuation = if (language == English) helpWithValuationText else helpWithValuationTextWelsh
    val effectiveDate = if (language == English) effectiveDateText else effectiveDateTextWelsh
    val helpWithEffectiveDate = if (language == English) helpWithEffectiveDateText else helpWithEffectiveDateTextWelsh
    val connectionToProperty = if (language == English) connectionToPropertyText else connectionToPropertyTextWelsh
    val rateableValue = if (language == English) rateableValueText else rateableValueTextWelsh

    s"has a $previousText tag in row $row for the previous valuation in $language" in {
      page.select(valuationTagLocator(row)).text() shouldBe previous
    }

    s"has $previousDateLink displayed as a link to the detailed valuation in row $row in $language" in {
      page.select(valuationDatesLinkLocator(row)).text() shouldBe previousDateLink
      page.select(valuationDatesLinkLocator(row)).attr("href") shouldBe valuationHref
    }

    s"has an effective date of $previousEffectiveDateText in row $row for the previous valuation in $language" in {
      page.select(valuationEffectiveDateLocator(row)).text() shouldBe previousEffectiveDate
    }

    s"has a connection of $connection in row $row for the previous valuation in $language" in {
      page.select(valuationConnectionLocator(row)).text() shouldBe expConnection
    }

    s"has a rateable value of $previousRv in row $row for the previous valuation in $language" in {
      page.select(valuationRvLocator(row)).text() shouldBe previousRv
    }

    s"has responsive heading in row $row in $language (Previous)" in {

      page.select(valuationHeadingResponsive(row)).text() shouldBe s"$valuation $helpWithValuation"
      page.select(valuationHeadingHelpLinkResponsive(row)).attr("href") shouldBe valuationHelpHref

      page.select(effectiveDateTableHeadingLocator).text() shouldBe s"$effectiveDate $helpWithEffectiveDate"
      page.select(effectiveDateHelpLinkLocator).attr("href") shouldBe effectiveDateHelpHref

      page.select(connectionTableHeadingLocator).text() shouldBe connectionToProperty

      page.select(rvTableHeadingLocator).text() shouldBe rateableValue
    }
  }

  private def testNotApplicableExpandable(res: => WSResponse, language: Language): Unit = {
    val helpWithNaRv = if (language == English) helpWithNaRvText else helpWithNaRvTextWelsh
    val rateableValueNaWhenRemoved =
      if (language == English) rateableValueNaWhenRemovedText else rateableValueNaWhenRemovedTextWelsh
    val thisCanBe = if (language == English) thisCanBeText else thisCanBeTextWelsh
    val propertySplitOrMerged = if (language == English) propertySplitOrMergedText else propertySplitOrMergedTextWelsh
    val addressOrDescChanged = if (language == English) addressOrDescChangedText else addressOrDescChangedTextWelsh
    val findNewProperty = if (language == English) findNewPropertyText else findNewPropertyTextWelsh
    val findNewPropertyLink = if (language == English) findNewPropertyLinkText else findNewPropertyLinkTextWelsh
    val ifYouOwn = if (language == English) ifYouOwnText else ifYouOwnTextWelsh
    val viewDetailedValuation = if (language == English) viewDetailedValuationText else viewDetailedValuationTextWelsh
    val sendCheckOrChallenge = if (language == English) sendCheckOrChallengeText else sendCheckOrChallengeTextWelsh
    val propertyRemoved = if (language == English) propertyRemovedText else propertyRemovedTextWelsh
    lazy val page = Jsoup.parse(res.body)

    s"has an expandable $helpWithNaRvText section with the correct content" in {
      page.select(helpWithNaRvLocator).text() shouldBe helpWithNaRv
      page.select(rateableValueNaWhenRemovedLocator).text() shouldBe rateableValueNaWhenRemoved
      page.select(thisCanBeLocator).text() shouldBe thisCanBe
      page.select(propertySplitOrMergedLocator).text() shouldBe propertySplitOrMerged
      page.select(addressOrDescChangedLocator).text() shouldBe addressOrDescChanged
      page.select(findNewPropertyLocator).text() shouldBe findNewProperty
      page.select(findNewPropertyLinkLocator).text() shouldBe findNewPropertyLink
      page.select(findNewPropertyLinkLocator).attr("href") shouldBe naHelpHref
      page.select(ifYouOwnLocator).text() shouldBe ifYouOwn
      page.select(viewDetailedValuationLocator).text() shouldBe viewDetailedValuation
      page.select(sendCheckOrChallengeLocator).text() shouldBe sendCheckOrChallenge
      page.select(propertyRemovedLocator).text() shouldBe propertyRemoved
    }

  }

  private def getAssessmentsPage(
        language: Language,
        userIsAgent: Boolean,
        connectionToProperty: String,
        assessments: List[ApiAssessment],
        previousPage: PreviousPage
  ) = {
    stubAuth(userIsAgent)
    stubOwnerProperties(assessments, connectionToProperty)
    stubAgentProperties(assessments, connectionToProperty)

    await(mockAssessmentsPageSessionRepository.saveOrUpdate(AssessmentsPageSession(previousPage = previousPage)))

    val params = if (userIsAgent) "?owner=false" else ""
    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/property-link/$plSubmissionId/assessments$params")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )
  }

}
