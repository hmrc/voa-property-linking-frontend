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

package controllers.detailedvaluationrequest.englishEstimatorEnabled

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class RequestDetailedValuationEnglishEstimatorEnabledISpec extends ISpecBase with HtmlComponentHelpers {

  override lazy val extraConfig: Map[String, String] =
    Map("feature-switch.draftListEnabled" -> "true", "feature-switch.englishEstimatorEnabled" -> "true", "feature-switch.welshEstimatorEnabled" -> "true")

  override def submissionId = "PL1ZRPBP7"
  override def uarn: Long = 7651789000L
  override def valuationId: Long = 10028428L

  val checkId = "1774b2a8-4ad1-4351-88fa-f9dc4868fa1c"

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "ADDRESS - Valuation Office Agency - GOV.UK"
  val titleTextWelsh = "ADDRESS - Valuation Office Agency - GOV.UK"

  val captionText = "Your property"
  val captionTextWelsh = "Eich eiddo"
  val captionTextSelector = "#main-content > div > div > div > div.govuk-grid-column-two-thirds > span"

  val h1Text = "ADDRESS"
  val h1TextWelsh = "ADDRESS"
  val h1TextSelector = "#main-content > div > div > div > div.govuk-grid-column-two-thirds > h1"

  val councilRefText = "Local council reference:"
  val councilRefTextWelsh = "Cyfeirnod yr awdurdod lleol:"
  val councilRefTextSelector = "#main-content > div > div > div > div.govuk-grid-column-two-thirds > dl > div > dt"
  val councilRefValueSelector = "#main-content > div > div > div > div.govuk-grid-column-two-thirds > dl > div > dd"

  // Valuation tab content - draft
  val valuationTabText = "Valuation"
  val valuationTabTextWelsh = "Prisiad"
  val valuationTabTextSelector =
    "#request-valuation-tabs > ul > li.govuk-tabs__list-item.govuk-tabs__list-item--selected"

  val valuationTabHeading = "Valuation"
  val valuationTabHeadingWelsh = "Prisiad"
  val valuationTabHeadingSelector = "#future-valuation-tab-content > h2"

  val valuationTabCaption = "Future rateable value (from 1 April 2026)"
  val valuationTabCaptionWelsh = "Gwerth ardrethol y dyfodol (o 1 Ebrill 2026)"
  val valuationTabCaptionSelector = "#future-valuation-caption"

  val valuationP1 =
    "The government has introduced a support package worth £4.3 billion over the next three years to protect ratepayers seeing their bills increase because of the revaluation. If you previously received Small Business Rates Relief, Rural Rate Relief, the Supporting Small Business scheme or Retail, Hospitality and Leisure relief, you may be eligible for the Supporting Small Business scheme, which caps your bill increases. Further details can be found here."
  val valuationP1TextWelsh =
    "Mae’r llywodraeth wedi cyflwyno pecyn cymorth gwerth £4.3 biliwn dros y tair blynedd nesaf i ddiogelu trethdalwyr rhag weld eu biliau’n cynyddu oherwydd yr ailbrisiad. Os ydych wedi derbyn Rhyddhad Ardrethi Busnesau Bach, Rhyddhad Ardrethi Gwledig, y cynllun Cefnogi Busnesau Bach neu’r rhyddhad ar gyfer Manwerthu, Lletygarwch a Hamdden yn y gorffennol, efallai y byddwch yn gymwys ar gyfer y cynllun Cefnogi Busnesau Bach, sy’n cyfyngu ar gynnydd yn eich bil. Gellir dod o hyd i ragor o fanylion yma."
  val valuationP1Selector = "#government-support-panel > div > p.govuk-body.white"

  val rateableValue = "£1,000"
  val rateableValueSelector = "#future-valuation-rv"

  val valuationTabInsetText =
    "This will be the rateable value for the property. It is not what you will pay in business rates or rent. Your local council uses the rateable value to calculate the business rates bill."
  val valuationTabInsetTextWelsh =
    "Dyma fydd y gwerth ardrethol ar gyfer yr eiddo. Nid dyma’r swm byddwch yn ei dalu mewn ardrethi busnes neu rent. Mae eich cyngor lleol yn defnyddio’r gwerth ardrethol er mwyn cyfrifo’r bil ardrethi busnes."
  val valuationTabInsetTextSelector = "#future-valuation-inset-rv > p:nth-child(1)"

  val valuationTabInsetLinkText = "Estimate what the business rates bill may be from 1 April 2026"
  val valuationTabInsetLinkTextWelsh = "Amcangyfrif beth all y bil ardrethi busnes fod o 1 Ebrill 2026"
  val valuationTabInsetLinkHref =
    "http://localhost:9300/business-rates-find/estimate-your-business-rates/start-from-dvr-valuation?authorisationId=1&propertyLinkSubmissionId=PL1ZRPBP7&valuationId=10028428&isOwner=true&uarn=7651789000&tabName=valuation-tab"
  val valuationTabInsetLinkTextSelector = "#future-estimate-link"

  val valuationTabP1 =
    "This detailed valuation will not be available until 1 April 2026. You will be able to request it from that date."
  val valuationTabP1Welsh =
    "Ni fydd y prisiad manwl hwn ar gael tan 1 Ebrill 2026. Byddwch yn gallu gwneud cais amdano o’r dyddiad hwnnw."
  val valuationTabP1Selector = "#future-valuation-not-available"

  val valuationTabP2 = "The current 2023 detailed valuation can be requested from the current valuation."
  val valuationTabP2Welsh = "Gellir gwneud cais am y prisiad manwl cyfredol 2023 o’r prisiad cyfredol."
  val valuationTabP2Selector = "#request-current-valuation"

  // Help with future valuation tab content - draft
  val valuationHelpTabText = "Help with future valuation"
  val valuationHelpTabTextWelsh = "Cymorth gyda’ch prisiad yn y dyfodol"
  val valuationHelpTabTextSelector = "#request-valuation-tabs > ul > li:nth-child(2)"

  val valuationHelpTabHeadingText = "Help with future valuation"
  val valuationHelpTabHeadingTextWelsh = "Cymorth gyda’ch prisiad yn y dyfodol"
  val valuationHelpTabHeadingTextSelector = "#future-valuation-help-h2-title"

  val valuationHelpTabRvMayChangeHeadingText = "Your rateable value may change on 1 April 2026"
  val valuationHelpTabRvMayChangeHeadingTextWelsh = "Gall eich gwerth ardrethol newid ar 1 Ebrill 2026"
  val valuationHelpTabRvMayChangeHeadingTextSelector = "#rateable-value-may-change-subhead"

  val valuationHelpTabRvMayChangeText =
    "We regularly update the rateable values of all business properties in England and Wales to reflect changes in the property market. The next revaluation will come into effect on 1 April 2026."
  val valuationHelpTabRvMayChangeTextWelsh =
    "Rydym yn diweddaru gwerthoedd ardrethol pob eiddo busnes yng Nghymru a Lloegr yn rheolaidd i adlewyrchu newidiadau yn y farchnad eiddo. Bydd yr ailbrisiad nesaf yn dod i rym ar 1 Ebrill 2026."
  val valuationHelpTabRvMayChangeTextSelector = "#rateable-value-may-change-content"

  val valuationHelpTabPropertyDetailsChangeHeadingText = "Your property details need changing"
  val valuationHelpTabPropertyDetailsChangeHeadingTextWelsh = "Mae angen newid manylion eich eiddo"
  val valuationHelpTabPropertyDetailsChangeHeadingTextSelector = "#property-details-changing-subhead"

  val valuationHelpTabPropertyDetailsChangeText =
    "Send us a Check case to tell us that your property details (such as floor area sizes and parking) need changing. We may accept your changes and update the current and future valuations."
  val valuationHelpTabPropertyDetailsChangeTextWelsh =
    "Anfonwch achos Gwirio atom ni i ddweud wrthym fod angen newid manylion eich eiddo (megis meintiau arwynebedd llawr a pharcio). Efallai byddwn yn derbyn eich newidiadau ac yn diweddaru’r prisiadau cyfredol a dyfodol."
  val valuationHelpTabPropertyDetailsChangeTextSelector = "#property-details-changing-content"

  val valuationHelpTabRvTooHighHeadingText = "You think the rateable value is too high"
  val valuationHelpTabRvTooHighHeadingTextWelsh = "Rydych chi’n meddwl bod y gwerth ardrethol yn rhy uchel"
  val valuationHelpTabRvTooHighHeadingTextSelector = "#rateable-value-too-high-subhead"

  val valuationHelpTabRvTooHighP1Text =
    "From 1 April 2026, send us a Challenge case to tell us you think the rateable value is too high."
  val valuationHelpTabRvTooHighP1TextWelsh =
    "O’r 1 Ebrill 2026, anfonwch achos Herio atom i ddweud wrthym eich bod yn credu bod y gwerth ardrethol yn rhy uchel."
  val valuationHelpTabRvTooHighP1TextSelector = "#rateable-value-too-high-content-1"

  val valuationHelpTabRvTooHighP2Text =
    "You must complete a Check case before sending a Challenge case. When you get our decision on your Check case, you have 4 months to send us a Challenge case."
  val valuationHelpTabRvTooHighP2TextWelsh =
    "Rhaid i chi gwblhau achos Gwirio cyn anfon achos Her. Pan fyddwch yn derbyn ein penderfyniad ar eich achos Gwirio, mae gennych 4 mis i anfon achos Her atom."
  val valuationHelpTabRvTooHighP2TextSelector = "#rateable-value-too-high-content-2"

  val valuationHelpTabValuationQuestionsHeadingText = "You have another question about your valuation"
  val valuationHelpTabValuationQuestionsHeadingTextWelsh = "Mae gennych gwestiwn arall ynglŷn â’ch prisiad"
  val valuationHelpTabValuationQuestionsHeadingTextSelector = "#other-question-subhead"

  val valuationHelpTabValuationQuestionsText = "Before 1 April 2026, send an enquiry."
  val valuationHelpTabValuationQuestionsTextWelsh = "Cyn 1 Ebrill 2026, anfon ymholiad."
  val valuationHelpTabValuationQuestionsTextSelector = "#other-question-content"

  val valuationHelpTabValuationQuestionsLinkText = "send an enquiry"
  val valuationHelpTabValuationQuestionsLinkTextWelsh = "anfon ymholiad"
  val valuationHelpTabValuationQuestionsLinkTextSelector = "#future-valuation-enquiry-link"
  val valuationHelpTabValuationQuestionsLinkHref = ""

  val link1Text = "How to use a business rates valuation account"
  val link1TextWelsh = "Sut i ddefnyddio cyfrif prisio ardrethi busnes"
  val link1Selector = "#how-to-use-account"
  val link1Href = "https://www.gov.uk/government/collections/check-and-challenge-step-by-step"

  val link2Text = "How business properties are valued"
  val link2TextWelsh = "Sut y prisir eiddo busnes"
  val link2Selector = "#how-properties-are-valued"
  val link2Href = "https://www.gov.uk/guidance/how-non-domestic-property-including-plant-and-machinery-is-valued"

  val link3Text = "Estimate what this property’s business rates bill may be from 1 April 2026"
  val link3TextWelsh = "Amcangyfrif beth all bil ardrethi busnes yr eiddo hwn fod o 1 Ebrill 2026"
  val link3Selector = "#help-estimator-link"
  val link3Href =
    "http://localhost:9300/business-rates-find/estimate-your-business-rates/start-from-dvr-valuation?authorisationId=1&propertyLinkSubmissionId=PL1ZRPBP7&valuationId=10028428&isOwner=true&uarn=7651789000&tabName=help-tab"

  val link4Text = "Business rates relief"
  val link4TextWelsh = "Rhyddhad Ardrethi Busnesau"
  val link4Selector = "#rates-relief"
  val link4Href = "https://www.gov.uk/apply-for-business-rate-relief"

  "myOrganisationAlreadyRequestedDetailValuation displays the correct content for viewing a draft valuation - English (English Property)" which {
    lazy val document: Document = getPage(English)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption of $captionText" in {
      document.select(captionTextSelector).text shouldBe captionText
    }

    s"has a heading of $h1Text" in {
      document.select(h1TextSelector).text shouldBe h1Text
    }

    s"has text and value for $councilRefText" in {
      document.select(councilRefTextSelector).text shouldBe councilRefText
      document.select(councilRefValueSelector).text shouldBe "AMBER-VALLEY-REF"
    }

    s"has a $valuationTabText tab" in {
      document.select(valuationTabTextSelector).text shouldBe valuationTabText
    }

    s"has a tab heading of $valuationTabHeading" in {
      document.select(valuationTabHeadingSelector).text shouldBe valuationTabHeading
    }

    s"has a tab caption of $valuationTabCaption" in {
      document.select(valuationTabCaptionSelector).text shouldBe valuationTabCaption
    }

    s"has a paragraph of $valuationP1" in {
      document.select(valuationP1Selector).text shouldBe valuationP1
    }

    s"has a rateable value of $rateableValue" in {
      document.select(rateableValueSelector).text shouldBe rateableValue
    }

    "has correct inset text" in {
      document.select(valuationTabInsetTextSelector).text shouldBe valuationTabInsetText
    }

    s"has correct link text of $valuationTabInsetLinkText" in {
      document.select(valuationTabInsetLinkTextSelector).text shouldBe valuationTabInsetLinkText
      document.select(valuationTabInsetLinkTextSelector).attr("href") shouldBe valuationTabInsetLinkHref
    }

    "has correct p1 content" in {
      document.select(valuationTabP1Selector).text shouldBe valuationTabP1
    }

    "has correct p2 content" in {
      document.select(valuationTabP2Selector).text shouldBe valuationTabP2
    }

    s"has a $valuationHelpTabText tab" in {
      document.select(valuationHelpTabTextSelector).text shouldBe valuationHelpTabText
    }

    s"has a tab heading of $valuationHelpTabHeadingText" in {
      document.select(valuationHelpTabHeadingTextSelector).text shouldBe valuationHelpTabHeadingText
    }

    s"has a section heading of $valuationHelpTabRvMayChangeHeadingText" in {
      document
        .select(valuationHelpTabRvMayChangeHeadingTextSelector)
        .text shouldBe valuationHelpTabRvMayChangeHeadingText
    }

    s"has correct content within $valuationHelpTabRvMayChangeHeadingText section" in {
      document.select(valuationHelpTabRvMayChangeTextSelector).text shouldBe valuationHelpTabRvMayChangeText
    }

    s"has a section heading of $valuationHelpTabPropertyDetailsChangeHeadingText" in {
      document
        .select(valuationHelpTabPropertyDetailsChangeHeadingTextSelector)
        .text shouldBe valuationHelpTabPropertyDetailsChangeHeadingText
    }

    s"has correct content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document
        .select(valuationHelpTabPropertyDetailsChangeTextSelector)
        .text shouldBe valuationHelpTabPropertyDetailsChangeText
    }

    s"has a section heading of $valuationHelpTabRvTooHighHeadingText" in {
      document.select(valuationHelpTabRvTooHighHeadingTextSelector).text shouldBe valuationHelpTabRvTooHighHeadingText
    }

    s"has correct p1 content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document.select(valuationHelpTabRvTooHighP1TextSelector).text shouldBe valuationHelpTabRvTooHighP1Text
    }

    s"has correct p2 content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document.select(valuationHelpTabRvTooHighP2TextSelector).text shouldBe valuationHelpTabRvTooHighP2Text
    }

    s"has a section heading of $valuationHelpTabValuationQuestionsHeadingText" in {
      document
        .select(valuationHelpTabValuationQuestionsHeadingTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsHeadingText
    }

    s"has correct p1 content within $valuationHelpTabValuationQuestionsHeadingText section" in {
      document
        .select(valuationHelpTabValuationQuestionsTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsText
    }

    s"has correct link content within $valuationHelpTabValuationQuestionsHeadingText section" in {
      document
        .select(valuationHelpTabValuationQuestionsLinkTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsLinkText
    }

    s"has link for $link1Text" in {
      document.select(link1Selector).text shouldBe link1Text
      document.select(link1Selector).attr("href") shouldBe link1Href
    }

    s"has link for $link2Text" in {
      document.select(link2Selector).text shouldBe link2Text
      document.select(link2Selector).attr("href") shouldBe link2Href
    }

    s"has link for $link3Text" in {
      document.select(link3Selector).text shouldBe link3Text
      document.select(link3Selector).attr("href") shouldBe link3Href
    }

    s"has link for $link4Text" in {
      document.select(link4Selector).text shouldBe link4Text
      document.select(link4Selector).attr("href") shouldBe link4Href
    }
  }

  "myOrganisationAlreadyRequestedDetailValuation displays the correct content for viewing a draft valuation - Welsh (English Property)" which {
    lazy val document: Document = getPage(Welsh)

    s"has a title of $titleText" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption of $captionText" in {
      document.select(captionTextSelector).text shouldBe captionTextWelsh
    }

    s"has a heading of $h1Text" in {
      document.select(h1TextSelector).text shouldBe h1TextWelsh
    }

    s"has text and value for $councilRefText" in {
      document.select(councilRefTextSelector).text shouldBe councilRefTextWelsh
      document.select(councilRefValueSelector).text shouldBe "AMBER-VALLEY-REF"
    }

    s"has a $valuationTabText tab" in {
      document.select(valuationTabTextSelector).text shouldBe valuationTabTextWelsh
    }

    s"has a tab heading of $valuationTabHeading" in {
      document.select(valuationTabHeadingSelector).text shouldBe valuationTabHeadingWelsh
    }

    s"has a tab caption of $valuationTabCaption" in {
      document.select(valuationTabCaptionSelector).text shouldBe valuationTabCaptionWelsh
    }

    s"has a paragraph of $valuationP1" in {
      document.select(valuationP1Selector).text shouldBe valuationP1TextWelsh
    }

    s"has a rateable value of $rateableValue" in {
      document.select(rateableValueSelector).text shouldBe rateableValue
    }

    "has correct inset text" in {
      document.select(valuationTabInsetTextSelector).text shouldBe valuationTabInsetTextWelsh
    }

    s"has correct link text of $valuationTabInsetLinkText" in {
      document.select(valuationTabInsetLinkTextSelector).text shouldBe valuationTabInsetLinkTextWelsh
      document.select(valuationTabInsetLinkTextSelector).attr("href") shouldBe valuationTabInsetLinkHref
    }

    "has correct p1 content" in {
      document.select(valuationTabP1Selector).text shouldBe valuationTabP1Welsh
    }

    "has correct p2 content" in {
      document.select(valuationTabP2Selector).text shouldBe valuationTabP2Welsh
    }

    s"has a $valuationHelpTabText tab" in {
      document.select(valuationHelpTabTextSelector).text shouldBe valuationHelpTabTextWelsh
    }

    s"has a tab heading of $valuationHelpTabHeadingText" in {
      document.select(valuationHelpTabHeadingTextSelector).text shouldBe valuationHelpTabHeadingTextWelsh
    }

    s"has a section heading of $valuationHelpTabRvMayChangeHeadingText" in {
      document
        .select(valuationHelpTabRvMayChangeHeadingTextSelector)
        .text shouldBe valuationHelpTabRvMayChangeHeadingTextWelsh
    }

    s"has correct content within $valuationHelpTabRvMayChangeHeadingText section" in {
      document.select(valuationHelpTabRvMayChangeTextSelector).text shouldBe valuationHelpTabRvMayChangeTextWelsh
    }

    s"has a section heading of $valuationHelpTabPropertyDetailsChangeHeadingText" in {
      document
        .select(valuationHelpTabPropertyDetailsChangeHeadingTextSelector)
        .text shouldBe valuationHelpTabPropertyDetailsChangeHeadingTextWelsh
    }

    s"has correct content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document
        .select(valuationHelpTabPropertyDetailsChangeTextSelector)
        .text shouldBe valuationHelpTabPropertyDetailsChangeTextWelsh
    }

    s"has a section heading of $valuationHelpTabRvTooHighHeadingText" in {
      document
        .select(valuationHelpTabRvTooHighHeadingTextSelector)
        .text shouldBe valuationHelpTabRvTooHighHeadingTextWelsh
    }

    s"has correct p1 content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document.select(valuationHelpTabRvTooHighP1TextSelector).text shouldBe valuationHelpTabRvTooHighP1TextWelsh
    }

    s"has correct p2 content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document.select(valuationHelpTabRvTooHighP2TextSelector).text shouldBe valuationHelpTabRvTooHighP2TextWelsh
    }

    s"has a section heading of $valuationHelpTabValuationQuestionsHeadingText" in {
      document
        .select(valuationHelpTabValuationQuestionsHeadingTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsHeadingTextWelsh
    }

    s"has correct p1 content within $valuationHelpTabValuationQuestionsHeadingText section" in {
      document
        .select(valuationHelpTabValuationQuestionsTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsTextWelsh
    }

    s"has correct link content within $valuationHelpTabValuationQuestionsHeadingText section" in {
      document
        .select(valuationHelpTabValuationQuestionsLinkTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsLinkTextWelsh
    }

    s"has link for $link1Text" in {
      document.select(link1Selector).text shouldBe link1TextWelsh
      document.select(link1Selector).attr("href") shouldBe link1Href
    }

    s"has link for $link2Text" in {
      document.select(link2Selector).text shouldBe link2TextWelsh
      document.select(link2Selector).attr("href") shouldBe link2Href
    }

    s"has link for $link3Text" in {
      document.select(link3Selector).text shouldBe link3TextWelsh
      document.select(link3Selector).attr("href") shouldBe link3Href
    }

    s"has link for $link4Text" in {
      document.select(link4Selector).text shouldBe link4TextWelsh
      document.select(link4Selector).attr("href") shouldBe link4Href
    }
  }

  "myOrganisationAlreadyRequestedDetailValuation displays the correct content for viewing a draft valuation - English (Welsh Property)" which {
    lazy val document: Document = getPage(English, true)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption of $captionText" in {
      document.select(captionTextSelector).text shouldBe captionText
    }

    s"has a heading of $h1Text" in {
      document.select(h1TextSelector).text shouldBe h1Text
    }

    s"has text and value for $councilRefText" in {
      document.select(councilRefTextSelector).text shouldBe councilRefText
      document.select(councilRefValueSelector).text shouldBe "AMBER-VALLEY-REF"
    }

    s"has a $valuationTabText tab" in {
      document.select(valuationTabTextSelector).text shouldBe valuationTabText
    }

    s"has a tab heading of $valuationTabHeading" in {
      document.select(valuationTabHeadingSelector).text shouldBe valuationTabHeading
    }

    s"has a tab caption of $valuationTabCaption" in {
      document.select(valuationTabCaptionSelector).text shouldBe valuationTabCaption
    }


    s"has a rateable value of $rateableValue" in {
      document.select(rateableValueSelector).text shouldBe rateableValue
    }

    "has correct inset text" in {
      document.select(valuationTabInsetTextSelector).text shouldBe valuationTabInsetText
    }

    s"has correct link text of $valuationTabInsetLinkText" in {
      document.select(valuationTabInsetLinkTextSelector).text shouldBe valuationTabInsetLinkText
      document.select(valuationTabInsetLinkTextSelector).attr("href") shouldBe valuationTabInsetLinkHref
    }

    "has correct p1 content" in {
      document.select(valuationTabP1Selector).text shouldBe valuationTabP1
    }

    "has correct p2 content" in {
      document.select(valuationTabP2Selector).text shouldBe valuationTabP2
    }

    s"has a $valuationHelpTabText tab" in {
      document.select(valuationHelpTabTextSelector).text shouldBe valuationHelpTabText
    }

    s"has a tab heading of $valuationHelpTabHeadingText" in {
      document.select(valuationHelpTabHeadingTextSelector).text shouldBe valuationHelpTabHeadingText
    }

    s"has a section heading of $valuationHelpTabRvMayChangeHeadingText" in {
      document
        .select(valuationHelpTabRvMayChangeHeadingTextSelector)
        .text shouldBe valuationHelpTabRvMayChangeHeadingText
    }

    s"has correct content within $valuationHelpTabRvMayChangeHeadingText section" in {
      document.select(valuationHelpTabRvMayChangeTextSelector).text shouldBe valuationHelpTabRvMayChangeText
    }

    s"has a section heading of $valuationHelpTabPropertyDetailsChangeHeadingText" in {
      document
        .select(valuationHelpTabPropertyDetailsChangeHeadingTextSelector)
        .text shouldBe valuationHelpTabPropertyDetailsChangeHeadingText
    }

    s"has correct content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document
        .select(valuationHelpTabPropertyDetailsChangeTextSelector)
        .text shouldBe valuationHelpTabPropertyDetailsChangeText
    }

    s"has a section heading of $valuationHelpTabRvTooHighHeadingText" in {
      document.select(valuationHelpTabRvTooHighHeadingTextSelector).text shouldBe valuationHelpTabRvTooHighHeadingText
    }

    s"has correct p1 content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document.select(valuationHelpTabRvTooHighP1TextSelector).text shouldBe valuationHelpTabRvTooHighP1Text
    }

    s"has correct p2 content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document.select(valuationHelpTabRvTooHighP2TextSelector).text shouldBe valuationHelpTabRvTooHighP2Text
    }

    s"has a section heading of $valuationHelpTabValuationQuestionsHeadingText" in {
      document
        .select(valuationHelpTabValuationQuestionsHeadingTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsHeadingText
    }

    s"has correct p1 content within $valuationHelpTabValuationQuestionsHeadingText section" in {
      document
        .select(valuationHelpTabValuationQuestionsTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsText
    }

    s"has correct link content within $valuationHelpTabValuationQuestionsHeadingText section" in {
      document
        .select(valuationHelpTabValuationQuestionsLinkTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsLinkText
    }

    s"has link for $link1Text" in {
      document.select(link1Selector).text shouldBe link1Text
      document.select(link1Selector).attr("href") shouldBe link1Href
    }

    s"has link for $link2Text" in {
      document.select(link2Selector).text shouldBe link2Text
      document.select(link2Selector).attr("href") shouldBe link2Href
    }

    s"has link for $link3Text" in {
      document.select(link3Selector).text shouldBe link3Text
      document.select(link3Selector).attr("href") shouldBe link3Href
    }

    s"has link for $link4Text" in {
      document.select(link4Selector).text shouldBe link4Text
      document.select(link4Selector).attr("href") shouldBe link4Href
    }
  }

  "myOrganisationAlreadyRequestedDetailValuation displays the correct content for viewing a draft valuation - Welsh (Welsh Property)" which {
    lazy val document: Document = getPage(Welsh, true)

    s"has a title of $titleText" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption of $captionText" in {
      document.select(captionTextSelector).text shouldBe captionTextWelsh
    }

    s"has a heading of $h1Text" in {
      document.select(h1TextSelector).text shouldBe h1TextWelsh
    }

    s"has text and value for $councilRefText" in {
      document.select(councilRefTextSelector).text shouldBe councilRefTextWelsh
      document.select(councilRefValueSelector).text shouldBe "AMBER-VALLEY-REF"
    }

    s"has a $valuationTabText tab" in {
      document.select(valuationTabTextSelector).text shouldBe valuationTabTextWelsh
    }

    s"has a tab heading of $valuationTabHeading" in {
      document.select(valuationTabHeadingSelector).text shouldBe valuationTabHeadingWelsh
    }

    s"has a tab caption of $valuationTabCaption" in {
      document.select(valuationTabCaptionSelector).text shouldBe valuationTabCaptionWelsh
    }

    s"has a rateable value of $rateableValue" in {
      document.select(rateableValueSelector).text shouldBe rateableValue
    }

    "has correct inset text" in {
      document.select(valuationTabInsetTextSelector).text shouldBe valuationTabInsetTextWelsh
    }

    s"has correct link text of $valuationTabInsetLinkText" in {
      document.select(valuationTabInsetLinkTextSelector).text shouldBe valuationTabInsetLinkTextWelsh
      document.select(valuationTabInsetLinkTextSelector).attr("href") shouldBe valuationTabInsetLinkHref
    }

    "has correct p1 content" in {
      document.select(valuationTabP1Selector).text shouldBe valuationTabP1Welsh
    }

    "has correct p2 content" in {
      document.select(valuationTabP2Selector).text shouldBe valuationTabP2Welsh
    }

    s"has a $valuationHelpTabText tab" in {
      document.select(valuationHelpTabTextSelector).text shouldBe valuationHelpTabTextWelsh
    }

    s"has a tab heading of $valuationHelpTabHeadingText" in {
      document.select(valuationHelpTabHeadingTextSelector).text shouldBe valuationHelpTabHeadingTextWelsh
    }

    s"has a section heading of $valuationHelpTabRvMayChangeHeadingText" in {
      document
        .select(valuationHelpTabRvMayChangeHeadingTextSelector)
        .text shouldBe valuationHelpTabRvMayChangeHeadingTextWelsh
    }

    s"has correct content within $valuationHelpTabRvMayChangeHeadingText section" in {
      document.select(valuationHelpTabRvMayChangeTextSelector).text shouldBe valuationHelpTabRvMayChangeTextWelsh
    }

    s"has a section heading of $valuationHelpTabPropertyDetailsChangeHeadingText" in {
      document
        .select(valuationHelpTabPropertyDetailsChangeHeadingTextSelector)
        .text shouldBe valuationHelpTabPropertyDetailsChangeHeadingTextWelsh
    }

    s"has correct content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document
        .select(valuationHelpTabPropertyDetailsChangeTextSelector)
        .text shouldBe valuationHelpTabPropertyDetailsChangeTextWelsh
    }

    s"has a section heading of $valuationHelpTabRvTooHighHeadingText" in {
      document
        .select(valuationHelpTabRvTooHighHeadingTextSelector)
        .text shouldBe valuationHelpTabRvTooHighHeadingTextWelsh
    }

    s"has correct p1 content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document.select(valuationHelpTabRvTooHighP1TextSelector).text shouldBe valuationHelpTabRvTooHighP1TextWelsh
    }

    s"has correct p2 content within $valuationHelpTabPropertyDetailsChangeHeadingText section" in {
      document.select(valuationHelpTabRvTooHighP2TextSelector).text shouldBe valuationHelpTabRvTooHighP2TextWelsh
    }

    s"has a section heading of $valuationHelpTabValuationQuestionsHeadingText" in {
      document
        .select(valuationHelpTabValuationQuestionsHeadingTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsHeadingTextWelsh
    }

    s"has correct p1 content within $valuationHelpTabValuationQuestionsHeadingText section" in {
      document
        .select(valuationHelpTabValuationQuestionsTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsTextWelsh
    }

    s"has correct link content within $valuationHelpTabValuationQuestionsHeadingText section" in {
      document
        .select(valuationHelpTabValuationQuestionsLinkTextSelector)
        .text shouldBe valuationHelpTabValuationQuestionsLinkTextWelsh
    }

    s"has link for $link1Text" in {
      document.select(link1Selector).text shouldBe link1TextWelsh
      document.select(link1Selector).attr("href") shouldBe link1Href
    }

    s"has link for $link2Text" in {
      document.select(link2Selector).text shouldBe link2TextWelsh
      document.select(link2Selector).attr("href") shouldBe link2Href
    }

    s"has link for $link3Text" in {
      document.select(link3Selector).text shouldBe link3TextWelsh
      document.select(link3Selector).attr("href") shouldBe link3Href
    }

    s"has link for $link4Text" in {
      document.select(link4Selector).text shouldBe link4TextWelsh
      document.select(link4Selector).attr("href") shouldBe link4Href
    }
  }

  def authStubs: StubMapping = {
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

  private def getPage(language: Language, welshProperty: Boolean = false, allowedEstimatorScatCode: Boolean = false): Document = {

    getRequestStubs(welshProperty ,allowedEstimatorScatCode)

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/property-link/$submissionId/valuations/$valuationId/exists"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(
          HeaderNames.COOKIE -> "sessionId",
          "Csrf-Token"       -> "nocheck",
          "Content-Type"     -> "application/x-www-form-urlencoded"
        )
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  def getRequestStubs(welshProperty: Boolean = false, allowedEstimatorScatCode: Boolean = false): StubMapping = {

    val stubbedPropertyHistory = if(allowedEstimatorScatCode) testPropertyHistory.copy(history = Seq(testPropertyValuation.copy(scatCode = Some("303")))) else testPropertyHistory
    val stubApiAssessment = testApiAssessments(assessments = List(if (welshProperty) draftApiAssessmentWelsh else draftApiAssessment))

    authStubs

    stubFor {
      get(s"/vmv/rating-listing/api/properties/$uarn")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(stubbedPropertyHistory).toString())
        }
    }

    stubFor {
      get(s"/property-linking/dashboard/owner/assessments/$submissionId")
        .willReturn {
          aResponse
            .withStatus(OK)
            .withBody(Json.toJson(stubApiAssessment).toString())
        }
    }

    stubFor {
      get(s"/property-linking/dvr-record?organisationId=$propertyLinkId&assessmentRef=$valuationId")
        .willReturn {
          aResponse.withStatus(NOT_FOUND)
        }
    }
  }
}
