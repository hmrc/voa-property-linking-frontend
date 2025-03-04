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
import binders.propertylinks.ClaimPropertyReturnToPage
import com.github.tomakehurst.wiremock.client.WireMock._
import models._
import org.jsoup.Jsoup
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.PropertyLinkingSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class DeclarationControllerConfirmationISpec extends ISpecBase {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockPropertyLinkingSessionRepository: PropertyLinkingSessionRepository =
    app.injector.instanceOf[PropertyLinkingSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Property claim submitted - Valuation Office Agency - GOV.UK"
  val localCouncilReferenceText = "Local council reference: 2050466366770"
  val propertyText = "Property: Test Address, Test Lane, T35 T3R"
  val headingText = "Property claim submitted Your submission number PL-123456"
  val makeANoteText = "Make a note of your reference number as you’ll need to provide it if you contact us."
  val whatHappensNextText = "What happens next"
  val weWillProcessText =
    "We will process your claim to this property as quickly as possible but this may take up to 15 working days."
  val youCanSeeText =
    "You can see the status of your claim in your properties. Once the claim is approved, the status will update."
  val youCanSeeAgentText =
    "You can see the status of your claim in your client’s properties. Once the claim is approved, the status will update."
  val yourPropertiesText = "your properties"
  val yourClientsPropertiesText = "your client’s properties"
  val weWillContactText = "We will contact you if we need more information."
  val goBackText = "Go back to your dashboard"

  val titleTextWelsh = "Cais i hawlio eiddo wedi’i gyflwyno - Valuation Office Agency - GOV.UK"
  val localCouncilReferenceTextWelsh = "Cyfeirnod yr awdurdod lleol: 2050466366770"
  val propertyTextWelsh = "Eiddo: Test Address, Test Lane, T35 T3R"
  val headingTextWelsh = "Cais i hawlio eiddo wedi’i gyflwyno Eich rhif cyflwyno PL-123456"
  val makeANoteTextWelsh =
    "Gwnewch nodyn o’ch cyfeirnod gan y bydd angen i chi ei ddarparu os byddwch yn cysylltu â ni."
  val whatHappensNextTextWelsh = "Beth sy’n digwydd nesaf"
  val weWillProcessTextWelsh =
    "Byddwn yn prosesu’ch cais i hawlio’r eiddo hwn cyn gynted â phosibl ond gall hyn gymryd hyd at 15 diwrnod gwaith."
  val youCanSeeTextWelsh =
    "Gallwch weld statws eich hawliad yn eich eiddo. Unwaith y bydd yr hawliad wedi’i gymeradwyo, bydd y statws yn cael ei ddiweddaru."
  val youCanSeeAgentTextWelsh =
    "Gallwch weld statws eich cais ar eiddo eich cleient. Unwaith y bydd y cais wedi’i gymeradwyo, bydd y statws yn diweddaru."
  val yourPropertiesTextWelsh = "eich eiddo"
  val yourClientsPropertiesTextWelsh = "eiddo eich cleient"
  val weWillContactTextWelsh = "Byddwn yn cysylltu â chi os bydd angen mwy o wybodaeth arnom."
  val goBackTextWelsh = "Ewch yn ôl i’ch dangosfwrdd"

  val localCouncilReferenceSelector = "#local-authority-reference"
  val propertySelector = "#address"
  val headingSelector = "div.govuk-panel.govuk-panel--confirmation"
  val makeANoteSelector = "#main-content > div > div > p:nth-child(3)"
  val whatHappensNextSelector = "#main-content > div > div > h2"
  val weWillProcessSelector = "#main-content > div > div > p:nth-child(5)"
  val youCanSeeSelector = "#main-content > div > div > p:nth-child(6)"
  val yourPropertiesSelector = "#main-content > div > div > p:nth-child(6) > a"
  val weWillContactSelector = "#main-content > div > div > p:nth-child(7)"
  val goBackSelector = "#main-content > div > div > p:nth-child(8) > a"

  val yourPropertiesHref = "/business-rates-dashboard/your-properties"
  val yourClientPropertiesHref =
    "/business-rates-dashboard/selected-client-properties?clientOrganisationId=123&clientName=Client+Name"
  val goBackHref = "/business-rates-dashboard/home"

  "DeclarationController confirmation displays the correct content in English for an IP" which {
    lazy val document = getConfirmationPage(language = English, userIsAgent = false)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has text of $localCouncilReferenceText" in {
      document.select(localCouncilReferenceSelector).text shouldBe localCouncilReferenceText
    }

    s"has text of $propertyText" in {
      document.select(propertySelector).text shouldBe propertyText
    }

    s"has a heading of $headingText" in {
      document.select(headingSelector).text shouldBe headingText
    }

    s"has text on the screen of $makeANoteText" in {
      document.select(makeANoteSelector).text shouldBe makeANoteText
    }

    s"has a subheading on the screen of $whatHappensNextText" in {
      document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
    }

    s"has text on the screen of $weWillProcessText" in {
      document.select(weWillProcessSelector).text shouldBe weWillProcessText
    }

    s"has text on the screen of $youCanSeeText" in {
      document.select(youCanSeeSelector).text shouldBe youCanSeeText
    }

    s"has a $yourPropertiesText link which takes you back to the list of properties" in {
      document.select(yourPropertiesSelector).text() shouldBe yourPropertiesText
      document.select(yourPropertiesSelector).attr("href") shouldBe yourPropertiesHref
    }

    s"has text on the screen of $weWillContactText" in {
      document.select(weWillContactSelector).text shouldBe weWillContactText
    }

    s"has a $goBackText link which takes you back home" in {
      document.select(goBackSelector).text() shouldBe goBackText
      document.select(goBackSelector).attr("href") shouldBe goBackHref
    }

  }

  "DeclarationController confirmation displays the correct content in English for an agent" which {
    lazy val document = getConfirmationPage(language = English, userIsAgent = true)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has text of $localCouncilReferenceText" in {
      document.select(localCouncilReferenceSelector).text shouldBe localCouncilReferenceText
    }

    s"has text of $propertyText" in {
      document.select(propertySelector).text shouldBe propertyText
    }

    s"has a heading of $headingText" in {
      document.select(headingSelector).text shouldBe headingText
    }

    s"has text on the screen of $makeANoteText" in {
      document.select(makeANoteSelector).text shouldBe makeANoteText
    }

    s"has a subheading on the screen of $whatHappensNextText" in {
      document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
    }

    s"has text on the screen of $weWillProcessText" in {
      document.select(weWillProcessSelector).text shouldBe weWillProcessText
    }

    s"has text on the screen of $youCanSeeAgentText" in {
      document.select(youCanSeeSelector).text shouldBe youCanSeeAgentText
    }

    s"has a $yourClientsPropertiesText link which takes you back to the list of properties" in {
      document.select(yourPropertiesSelector).text() shouldBe yourClientsPropertiesText
      document.select(yourPropertiesSelector).attr("href") shouldBe yourClientPropertiesHref
    }

    s"has text on the screen of $weWillContactText" in {
      document.select(weWillContactSelector).text shouldBe weWillContactText
    }

    s"has a $goBackText link which takes you back home" in {
      document.select(goBackSelector).text() shouldBe goBackText
      document.select(goBackSelector).attr("href") shouldBe goBackHref
    }

  }

  "DeclarationController confirmation displays the correct content in Welsh for an IP" which {
    lazy val document = getConfirmationPage(language = Welsh, userIsAgent = false)

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has text of $localCouncilReferenceText in welsh" in {
      document.select(localCouncilReferenceSelector).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has text of $propertyText in welsh" in {
      document.select(propertySelector).text shouldBe propertyTextWelsh
    }

    s"has a heading of $headingText in welsh" in {
      document.select(headingSelector).text shouldBe headingTextWelsh
    }

    s"has text on the screen of $makeANoteText in welsh" in {
      document.select(makeANoteSelector).text shouldBe makeANoteTextWelsh
    }

    s"has a subheading on the screen of $whatHappensNextText in welsh" in {
      document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
    }

    s"has text on the screen of $weWillProcessText in welsh" in {
      document.select(weWillProcessSelector).text shouldBe weWillProcessTextWelsh
    }

    s"has text on the screen of $youCanSeeText in welsh" in {
      document.select(youCanSeeSelector).text shouldBe youCanSeeTextWelsh
    }

    s"has a $yourPropertiesText link which takes you back to the list of properties in welsh" in {
      document.select(yourPropertiesSelector).text() shouldBe yourPropertiesTextWelsh
      document.select(yourPropertiesSelector).attr("href") shouldBe yourPropertiesHref
    }

    s"has text on the screen of $weWillContactText in welsh" in {
      document.select(weWillContactSelector).text shouldBe weWillContactTextWelsh
    }

    s"has a $goBackText link which takes you back home in welsh" in {
      document.select(goBackSelector).text() shouldBe goBackTextWelsh
      document.select(goBackSelector).attr("href") shouldBe goBackHref
    }

  }

  "DeclarationController confirmation displays the correct content in Welsh for an agent" which {
    lazy val document = getConfirmationPage(language = Welsh, userIsAgent = true)

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has text of $localCouncilReferenceText in welsh" in {
      document.select(localCouncilReferenceSelector).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has text of $propertyText in welsh" in {
      document.select(propertySelector).text shouldBe propertyTextWelsh
    }

    s"has a heading of $headingText in welsh" in {
      document.select(headingSelector).text shouldBe headingTextWelsh
    }

    s"has text on the screen of $makeANoteText in welsh" in {
      document.select(makeANoteSelector).text shouldBe makeANoteTextWelsh
    }

    s"has a subheading on the screen of $whatHappensNextText in welsh" in {
      document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
    }

    s"has text on the screen of $weWillProcessText in welsh" in {
      document.select(weWillProcessSelector).text shouldBe weWillProcessTextWelsh
    }

    s"has text on the screen of $youCanSeeAgentText in welsh" in {
      document.select(youCanSeeSelector).text shouldBe youCanSeeAgentTextWelsh
    }

    s"has a $yourClientsPropertiesText link which takes you back to the list of properties in welsh" in {
      document.select(yourPropertiesSelector).text() shouldBe yourClientsPropertiesTextWelsh
      document.select(yourPropertiesSelector).attr("href") shouldBe yourClientPropertiesHref
    }

    s"has text on the screen of $weWillContactText in welsh" in {
      document.select(weWillContactSelector).text shouldBe weWillContactTextWelsh
    }

    s"has a $goBackText link which takes you back home in welsh" in {
      document.select(goBackSelector).text() shouldBe goBackTextWelsh
      document.select(goBackSelector).attr("href") shouldBe goBackHref
    }

  }
  private def getConfirmationPage(language: Language, userIsAgent: Boolean) = {
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

    await(
      mockPropertyLinkingSessionRepository.saveOrUpdate(
        LinkingSession(
          address = "Test Address, Test Lane, T35 T3R",
          uarn = 1L,
          submissionId = "PL-123456",
          personId = 1L,
          earliestStartDate = LocalDate.of(2017, 4, 1),
          propertyRelationship = None,
          propertyOwnership = None,
          propertyOccupancy = None,
          hasRatesBill = None,
          clientDetails = if (userIsAgent) Some(ClientDetails(123, "Client Name")) else None,
          localAuthorityReference = "2050466366770",
          rtp = ClaimPropertyReturnToPage.FMBR,
          fromCya = None,
          isSubmitted = None
        )
      )
    )

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/confirmation"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
