/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.detailedvaluationrequest.draftListEnabled

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class ViewDvrValuationISpec extends ISpecBase {

  override lazy val extraConfig: Map[String, String] =
    Map("feature-switch.draftListEnabled" -> "true", "feature-switch.comparablePropertiesEnabled" -> "false")

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  override def submissionId = "PL1ZRPBP7"
  override def uarn: Long = 7651789000L
  override def valuationId: Long = 10028428L
  override def propertyLinkId: Long = 128L

  val titleText = "ADDRESS - Valuation Office Agency - GOV.UK"
  val backText = "Back"
  val captionText = "Your property"
  val headerText = "ADDRESS"
  val localAuthorityReferenceText = s"Local authority reference: $localAuthorityRef"
  val valuationTabText = "Valuation"
  val startCheckTabText = "Start a Check"
  val checksTabText = "Checks"
  val agentsTabText = "Agents (2)"
  val comparablePropertiesTabText = "Comparable properties"
  val valuationHeadingText = "Valuation"
  val currentRateableValueText = "Current rateable value (1 April 2023 to present)"
  val rateableValueText = "£2,000"
  val insetText =
    "This is the rateable value for the property. It is not what you pay in business rates or rent. Your local council uses the rateable value to calculate the business rates bill."
  val downloadDetailedValuationText = "Download the detailed valuation"
  val changeSomethingHeadingText = "If you want to change something in this valuation"
  val changeSomethingText =
    "If the property’s details are incorrect, or you believe the rateable value is wrong, you must complete a Check case form. Any changes will carry over to the future valuation."
  val downloadCheckFormText = "Download the Check case form"
  val afterCompletingText = "After completing the form, send it to us as part of a Check case."
  val warningText =
    "Warning Some older Check case forms may tell you to email or post your form. Please ignore this and use the 'Send my completed Check case form' button instead."
  val sendCompletedCheckFormText = "Send my completed Check case form"
  val printThisPageText = "Print this page"

  val titleTextWelsh = "ADDRESS - Valuation Office Agency - GOV.UK"
  val backTextWelsh = "Yn ôl"
  val captionTextWelsh = "Eich eiddo"
  val headerTextWelsh = "ADDRESS"
  val localAuthorityReferenceTextWelsh = s"Cyfeirnod yr awdurdod lleol: $localAuthorityRef"
  val valuationTabTextWelsh = "Prisiad"
  val startCheckTabTextWelsh = "Dechrau Gwiriad"
  val checksTabTextWelsh = "Gwiriadau"
  val agentsTabTextWelsh = "Asiantiaid (2)"
  val valuationHeadingTextWelsh = "Prisiad"
  val currentRateableValueTextWelsh = "Gwerth ardrethol cyfredol (1 Ebrill 2023 i presennol)"
  val insetTextWelsh =
    "Dyma’r gwerth ardrethol ar gyfer yr eiddo. Nid dyma’r swm rydych yn ei dalu mewn ardrethi busnes neu rent. Mae eich cyngor lleol yn defnyddio’r gwerth ardrethol er mwyn cyfrifo’r bil ardrethi busnes."
  val downloadDetailedValuationTextWelsh = "Lawrlwythwch y prisiad manwl"
  val changeSomethingHeadingTextWelsh = "Os ydych eisiau newid rhywbeth yn y prisiad hwn"
  val changeSomethingTextWelsh =
    "Os yw manylion yr eiddo yn anghywir, neu os ydych yn credu bod y gwerth ardrethol yn anghywir, mae’n rhaid i chi gwblhau ffurflen achos Gwirio. Bydd unrhyw newidiadau yn cael ei gario ymlaen i’r prisiad yn y dyfodol."
  val downloadCheckFormTextWelsh = "Lawrlwythwch y ffurflen achos Gwirio"
  val afterCompletingTextWelsh = "Ar ôl cwblhau’r ffurflen, anfonwch hi atom fel rhan o achos Gwirio."
  val warningTextWelsh =
    "Rhybudd Efallai bod rhai ffurflenni achos Gwirio hŷn yn dweud wrthych am e-bostio neu bostio eich ffurflen. Anwybyddwch hyn a defnyddiwch y botwm 'Anfon fy ffurflen achos Gwirio wedi’i chwblhau' yn lle."
  val sendCompletedCheckFormTextWelsh = "Anfon fy ffurflen achos Gwirio wedi’i chwblhau"
  val printThisPageTextWelsh = "Argraffu’r dudalen hon"

  val backHref = s"/business-rates-property-linking/property-link/$submissionId/assessments"
  val valuationTabHref = "#valuation-tab"
  val startCheckTabHref = "#start-check-tab"
  val checksTabHref = "#check-cases-tab"
  val agentsTabHref = "#agents-tab"
  val downloadValuationHref =
    s"/business-rates-property-linking/my-organisation/property-link/$submissionId/valuations/$valuationId/file/2L"
  val downloadCheckFormHref =
    s"/business-rates-property-linking/my-organisation/property-link/$submissionId/valuations/$valuationId/file/1L"
  val sendCheckFormHref = "#start-check-tab"
  val printPageHref = "javascript:window.print()"

  val backLocator = "#back-link"
  val captionLocator = "#client-name"
  val headerLocator = "#assessment-address"
  val localAuthorityReferenceLocator = "#main-content > div > div > div.govuk-grid-row > div > dl > div"
  val valuationTabLocator = "#view-valuation-tab"
  val startCheckTabLocator = "#start-check-case-tab"
  val checksTabLocator = "#check-tab"
  val agentsTabLocator = "#agent-tab"
  val valuationHeadingLocator = "#valuation-tab > div > div > h2.govuk-heading-l"
  val currentRateableValueLocator = "#rateable-value-caption"
  val rateableValueLocator = "#grand-total-top"
  val insetLocator = "#intro-text"
  val downloadDetailedValuationLocator = "#valuation-tab-download-link"
  val changeSomethingHeadingLocator = "#valuation-tab-change-something-heading"
  val changeSomethingLocator = "#valuation-tab-change-something-content > p:nth-child(1)"
  val downloadCheckFormLocator = "#valuation-tab-download-check-form"
  val afterCompletingLocator = "#valuation-tab-change-something-content > p:nth-child(3)"
  val warningLocator = "#valuation-tab-change-something-content > div > strong"
  val sendCompletedCheckFormLocator = "#valuation-tab-send-check-form"
  val printThisPageLocator = "#main-content > div > div > p > a"

  // todo write tests for flag on and off

  "DvrController myOrganisationRequestDetailValuationCheck method" should {
    "display the correct page for an IP in English" when {
      lazy val res = getDvrValuationPage(language = English, userIsAgent = false)
      lazy val page = Jsoup.parse(res.body)

      "has a status of 200 OK" in {
        res.status shouldBe OK
      }

      s"has a title of $titleText" in {
        page.title() shouldBe titleText
      }

      "has a back link with the correct href" in {
        page.select(backLocator).text() shouldBe backText
        page.select(backLocator).attr("href") shouldBe backHref
      }

      s"has a caption of $captionText" in {
        page.select(captionLocator).text() shouldBe captionText
      }

      s"has a heading of $headerText" in {
        page.select(headerLocator).text() shouldBe headerText
      }

      s"has text of $localAuthorityReferenceText" in {
        page.select(localAuthorityReferenceLocator).text() shouldBe localAuthorityReferenceText
      }

      s"has a $valuationTabText tab" in {
        page.select(valuationTabLocator).text() shouldBe valuationTabText
        page.select(valuationTabLocator).attr("href") shouldBe valuationTabHref
      }

      s"has a $startCheckTabText tab" in {
        page.select(startCheckTabLocator).text() shouldBe startCheckTabText
        page.select(startCheckTabLocator).attr("href") shouldBe startCheckTabHref
      }

      s"has a $checksTabText tab" in {
        page.select(checksTabLocator).text() shouldBe checksTabText
        page.select(checksTabLocator).attr("href") shouldBe checksTabHref
      }

      s"has a $agentsTabText tab" in {
        page.select(agentsTabLocator).text() shouldBe agentsTabText
        page.select(agentsTabLocator).attr("href") shouldBe agentsTabHref
      }

      s"has a heading of $valuationHeadingText" in {
        page.select(valuationHeadingLocator).text() shouldBe valuationHeadingText
      }

      s"has a rateable value caption of $currentRateableValueText" in {
        page.select(currentRateableValueLocator).text() shouldBe currentRateableValueText
      }

      s"has a rateable value of $rateableValueText" in {
        page.select(rateableValueLocator).text() shouldBe rateableValueText
      }

      s"has inset text of $insetText" in {
        page.select(insetLocator).text() shouldBe insetText
      }

      s"has a $downloadDetailedValuationText button" in {
        page.select(downloadDetailedValuationLocator).text() shouldBe downloadDetailedValuationText
        page.select(downloadDetailedValuationLocator).attr("href") shouldBe downloadValuationHref
      }

      s"has a subheading of $changeSomethingHeadingText" in {
        page.select(changeSomethingHeadingLocator).text() shouldBe changeSomethingHeadingText
      }

      s"has text of $changeSomethingText" in {
        page.select(changeSomethingLocator).text() shouldBe changeSomethingText
      }

      s"has a $downloadCheckFormText link with correct href" in {
        page.select(downloadCheckFormLocator).text() shouldBe downloadCheckFormText
      }

      s"has text of $afterCompletingText" in {
        page.select(afterCompletingLocator).text() shouldBe afterCompletingText
      }

      s"has warning text of $warningText" in {
        page.select(warningLocator).text() shouldBe warningText
      }

      s"has a $sendCompletedCheckFormText button with correct href" in {
        page.select(sendCompletedCheckFormLocator).text() shouldBe sendCompletedCheckFormText
        page.select(sendCompletedCheckFormLocator).attr("href") shouldBe sendCheckFormHref
      }

      s"has a $printThisPageText link with correct href" in {
        page.select(printThisPageLocator).text() shouldBe printThisPageText
        page.select(printThisPageLocator).attr("href") shouldBe printPageHref
      }
    }

    "display the correct page for an AGENT in English" when {
      lazy val res = getDvrValuationPage(language = English, userIsAgent = true)
      lazy val page = Jsoup.parse(res.body)

      "has a status of 200 OK" in {
        res.status shouldBe OK
      }

      s"has a title of $titleText" in {
        page.title() shouldBe titleText
      }

      "has a back link with the correct href" in {
        page.select(backLocator).text() shouldBe backText
        page.select(backLocator).attr("href") shouldBe backHref
      }

      s"has a caption of $captionText" in {
        page.select(captionLocator).text() shouldBe captionText
      }

      s"has a heading of $headerText" in {
        page.select(headerLocator).text() shouldBe headerText
      }

      s"has text of $localAuthorityReferenceText" in {
        page.select(localAuthorityReferenceLocator).text() shouldBe localAuthorityReferenceText
      }

      s"has a $valuationTabText tab" in {
        page.select(valuationTabLocator).text() shouldBe valuationTabText
        page.select(valuationTabLocator).attr("href") shouldBe valuationTabHref
      }

      s"has a $startCheckTabText tab" in {
        page.select(startCheckTabLocator).text() shouldBe startCheckTabText
        page.select(startCheckTabLocator).attr("href") shouldBe startCheckTabHref
      }

      s"has a $checksTabText tab" in {
        page.select(checksTabLocator).text() shouldBe checksTabText
        page.select(checksTabLocator).attr("href") shouldBe checksTabHref
      }

      s"has a $agentsTabText tab" in {
        page.select(agentsTabLocator).text() shouldBe agentsTabText
        page.select(agentsTabLocator).attr("href") shouldBe agentsTabHref
      }

      s"has a heading of $valuationHeadingText" in {
        page.select(valuationHeadingLocator).text() shouldBe valuationHeadingText
      }

      s"has a rateable value caption of $currentRateableValueText" in {
        page.select(currentRateableValueLocator).text() shouldBe currentRateableValueText
      }

      s"has a rateable value of $rateableValueText" in {
        page.select(rateableValueLocator).text() shouldBe rateableValueText
      }

      s"has inset text of $insetText" in {
        page.select(insetLocator).text() shouldBe insetText
      }

      s"has a $downloadDetailedValuationText button" in {
        page.select(downloadDetailedValuationLocator).text() shouldBe downloadDetailedValuationText
        page.select(downloadDetailedValuationLocator).attr("href") shouldBe downloadValuationHref
      }

      s"has a subheading of $changeSomethingHeadingText" in {
        page.select(changeSomethingHeadingLocator).text() shouldBe changeSomethingHeadingText
      }

      s"has text of $changeSomethingText" in {
        page.select(changeSomethingLocator).text() shouldBe changeSomethingText
      }

      s"has a $downloadCheckFormText link with correct href" in {
        page.select(downloadCheckFormLocator).text() shouldBe downloadCheckFormText
      }

      s"has text of $afterCompletingText" in {
        page.select(afterCompletingLocator).text() shouldBe afterCompletingText
      }

      s"has warning text of $warningText" in {
        page.select(warningLocator).text() shouldBe warningText
      }

      s"has a $sendCompletedCheckFormText button with correct href" in {
        page.select(sendCompletedCheckFormLocator).text() shouldBe sendCompletedCheckFormText
        page.select(sendCompletedCheckFormLocator).attr("href") shouldBe sendCheckFormHref
      }

      s"has a $printThisPageText link with correct href" in {
        page.select(printThisPageLocator).text() shouldBe printThisPageText
        page.select(printThisPageLocator).attr("href") shouldBe printPageHref
      }
    }

    "display the correct page for an IP in Welsh" when {
      lazy val res = getDvrValuationPage(language = Welsh, userIsAgent = false)
      lazy val page = Jsoup.parse(res.body)

      "has a status of 200 OK" in {
        res.status shouldBe OK
      }

      s"has a title of $titleText in Welsh" in {
        page.title() shouldBe titleText
      }

      "has a back link with the correct href in Welsh" in {
        page.select(backLocator).text() shouldBe backTextWelsh
        page.select(backLocator).attr("href") shouldBe backHref
      }

      s"has a caption of $captionText in Welsh" in {
        page.select(captionLocator).text() shouldBe captionTextWelsh
      }

      s"has a heading of $headerText in Welsh" in {
        page.select(headerLocator).text() shouldBe headerTextWelsh
      }

      s"has text of $localAuthorityReferenceText in Welsh" in {
        page.select(localAuthorityReferenceLocator).text() shouldBe localAuthorityReferenceTextWelsh
      }

      s"has a $valuationTabText tab in Welsh" in {
        page.select(valuationTabLocator).text() shouldBe valuationTabTextWelsh
        page.select(valuationTabLocator).attr("href") shouldBe valuationTabHref
      }

      s"has a $startCheckTabText tab in Welsh" in {
        page.select(startCheckTabLocator).text() shouldBe startCheckTabTextWelsh
        page.select(startCheckTabLocator).attr("href") shouldBe startCheckTabHref
      }

      s"has a $checksTabText tab in Welsh" in {
        page.select(checksTabLocator).text() shouldBe checksTabTextWelsh
        page.select(checksTabLocator).attr("href") shouldBe checksTabHref
      }

      s"has a $agentsTabText tab in Welsh" in {
        page.select(agentsTabLocator).text() shouldBe agentsTabTextWelsh
        page.select(agentsTabLocator).attr("href") shouldBe agentsTabHref
      }

      s"has a heading of $valuationHeadingText in Welsh" in {
        page.select(valuationHeadingLocator).text() shouldBe valuationHeadingTextWelsh
      }

      s"has a rateable value caption of $currentRateableValueText in Welsh" in {
        page.select(currentRateableValueLocator).text() shouldBe currentRateableValueTextWelsh
      }

      s"has a rateable value of $rateableValueText in Welsh" in {
        page.select(rateableValueLocator).text() shouldBe rateableValueText
      }

      s"has inset text of $insetText in Welsh" in {
        page.select(insetLocator).text() shouldBe insetTextWelsh
      }

      s"has a $downloadDetailedValuationText button in Welsh" in {
        page.select(downloadDetailedValuationLocator).text() shouldBe downloadDetailedValuationTextWelsh
        page.select(downloadDetailedValuationLocator).attr("href") shouldBe downloadValuationHref
      }

      s"has a subheading of $changeSomethingHeadingText in Welsh" in {
        page.select(changeSomethingHeadingLocator).text() shouldBe changeSomethingHeadingTextWelsh
      }

      s"has text of $changeSomethingText in Welsh" in {
        page.select(changeSomethingLocator).text() shouldBe changeSomethingTextWelsh
      }

      s"has a $downloadCheckFormText link with correct href in Welsh" in {
        page.select(downloadCheckFormLocator).text() shouldBe downloadCheckFormTextWelsh
      }

      s"has text of $afterCompletingText in Welsh" in {
        page.select(afterCompletingLocator).text() shouldBe afterCompletingTextWelsh
      }

      s"has warning text of $warningText in Welsh" in {
        page.select(warningLocator).text() shouldBe warningTextWelsh
      }

      s"has a $sendCompletedCheckFormText button with correct href in Welsh" in {
        page.select(sendCompletedCheckFormLocator).text() shouldBe sendCompletedCheckFormTextWelsh
        page.select(sendCompletedCheckFormLocator).attr("href") shouldBe sendCheckFormHref
      }

      s"has a $printThisPageText link with correct href in Welsh" in {
        page.select(printThisPageLocator).text() shouldBe printThisPageTextWelsh
        page.select(printThisPageLocator).attr("href") shouldBe printPageHref
      }
    }

    "display the correct page for an AGENT in Welsh" when {
      lazy val res = getDvrValuationPage(language = Welsh, userIsAgent = true)
      lazy val page = Jsoup.parse(res.body)

      "has a status of 200 OK" in {
        res.status shouldBe OK
      }

      s"has a title of $titleText in Welsh" in {
        page.title() shouldBe titleTextWelsh
      }

      "has a back link with the correct href in Welsh" in {
        page.select(backLocator).text() shouldBe backTextWelsh
        page.select(backLocator).attr("href") shouldBe backHref
      }

      s"has a caption of $captionText in Welsh" in {
        page.select(captionLocator).text() shouldBe captionTextWelsh
      }

      s"has a heading of $headerText in Welsh" in {
        page.select(headerLocator).text() shouldBe headerTextWelsh
      }

      s"has text of $localAuthorityReferenceText in Welsh" in {
        page.select(localAuthorityReferenceLocator).text() shouldBe localAuthorityReferenceTextWelsh
      }

      s"has a $valuationTabText tab in Welsh" in {
        page.select(valuationTabLocator).text() shouldBe valuationTabTextWelsh
        page.select(valuationTabLocator).attr("href") shouldBe valuationTabHref
      }

      s"has a $startCheckTabText tab in Welsh" in {
        page.select(startCheckTabLocator).text() shouldBe startCheckTabTextWelsh
        page.select(startCheckTabLocator).attr("href") shouldBe startCheckTabHref
      }

      s"has a $checksTabText tab in Welsh" in {
        page.select(checksTabLocator).text() shouldBe checksTabTextWelsh
        page.select(checksTabLocator).attr("href") shouldBe checksTabHref
      }

      s"has a $agentsTabText tab in Welsh" in {
        page.select(agentsTabLocator).text() shouldBe agentsTabTextWelsh
        page.select(agentsTabLocator).attr("href") shouldBe agentsTabHref
      }

      s"has a heading of $valuationHeadingText in Welsh" in {
        page.select(valuationHeadingLocator).text() shouldBe valuationHeadingTextWelsh
      }

      s"has a rateable value caption of $currentRateableValueText in Welsh" in {
        page.select(currentRateableValueLocator).text() shouldBe currentRateableValueTextWelsh
      }

      s"has a rateable value of $rateableValueText in Welsh" in {
        page.select(rateableValueLocator).text() shouldBe rateableValueText
      }

      s"has inset text of $insetText in Welsh" in {
        page.select(insetLocator).text() shouldBe insetTextWelsh
      }

      s"has a $downloadDetailedValuationText button in Welsh" in {
        page.select(downloadDetailedValuationLocator).text() shouldBe downloadDetailedValuationTextWelsh
        page.select(downloadDetailedValuationLocator).attr("href") shouldBe downloadValuationHref
      }

      s"has a subheading of $changeSomethingHeadingText in Welsh" in {
        page.select(changeSomethingHeadingLocator).text() shouldBe changeSomethingHeadingTextWelsh
      }

      s"has text of $changeSomethingText in Welsh" in {
        page.select(changeSomethingLocator).text() shouldBe changeSomethingTextWelsh
      }

      s"has a $downloadCheckFormText link with correct href in Welsh" in {
        page.select(downloadCheckFormLocator).text() shouldBe downloadCheckFormTextWelsh
      }

      s"has text of $afterCompletingText in Welsh" in {
        page.select(afterCompletingLocator).text() shouldBe afterCompletingTextWelsh
      }

      s"has warning text of $warningText in Welsh" in {
        page.select(warningLocator).text() shouldBe warningTextWelsh
      }

      s"has a $sendCompletedCheckFormText button with correct href in Welsh" in {
        page.select(sendCompletedCheckFormLocator).text() shouldBe sendCompletedCheckFormTextWelsh
        page.select(sendCompletedCheckFormLocator).attr("href") shouldBe sendCheckFormHref
      }

      s"has a $printThisPageText link with correct href in Welsh" in {
        page.select(printThisPageLocator).text() shouldBe printThisPageTextWelsh
        page.select(printThisPageLocator).attr("href") shouldBe printPageHref
      }
    }
  }

  private def getDvrValuationPage(language: Language, userIsAgent: Boolean): WSResponse = {

    getRequestStubs(userIsAgent)

    await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/property-link/$submissionId/valuations/$valuationId"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(
          HeaderNames.COOKIE -> "sessionId",
          "Csrf-Token"       -> "nocheck",
          "Content-Type"     -> "application/x-www-form-urlencoded"
        )
        .get()
    )
  }

  def getRequestStubs(userIsAgent: Boolean): StubMapping = {

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(if (userIsAgent) testAccounts else testIpAccounts).toString())
        }
    }

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

    stubFor {
      get(s"/business-rates-challenge/my-organisations/challenge-cases?submissionId=$submissionId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testChallengeCasesWithClient).toString())
        }
    }

    stubFor {
      get(s"/property-linking/dashboard/owner/assessments/$submissionId")
        .willReturn {
          aResponse
            .withStatus(OK)
            .withBody(Json.toJson(testApiAssessments(List(currentApiAssessment, previousApiAssessment))).toString())
        }
    }

    stubFor {
      get(s"/property-linking/properties/$uarn/valuation/$valuationId/files?propertyLinkId=$submissionId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(someDvrDocumentFiles).toString())
        }
    }

    stubFor {
      get(s"/property-linking/check-cases/$submissionId/client")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testCheckCasesWithClient).toString())
        }
    }

    stubFor {
      get(s"/business-rates-challenge/my-organisations/challenge-cases?submissionId=$submissionId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testChallengeCasesWithClient).toString())
        }
    }
  }
}
