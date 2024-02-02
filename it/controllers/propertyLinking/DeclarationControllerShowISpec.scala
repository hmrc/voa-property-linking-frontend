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

class DeclarationControllerShowISpec extends ISpecBase {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockPropertyLinkingSessionRepository: PropertyLinkingSessionRepository = app.injector.instanceOf[PropertyLinkingSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val addressValue = ""
  val connectionValue = ""
  val startedValue = ""
  val lastDayValue = ""
  val yesValue = "Yes"
  val yesValueWelsh = "Ydw"
  val noValue = "No"
  val noValueWelsh = "Nac ydw"
  val evidenceValue = ""

  val ownerText = "Owner"
  val occupierText = "Occupier"
  val ownerOccupierText = "Owner and occupier"
  val ownerTextWelsh = "Perchennog"
  val occupierTextWelsh = "Meddiannydd"
  val ownerOccupierTextWelsh = "Perchennog a meddiannydd"

  val titleText = "Check and confirm your details - Valuation Office Agency - GOV.UK"
  val captionText = "Add a property"
  val headerText = "Check and confirm your details"
  val addressText = "Address"
  val connectionToPropertyText = "Connection to property"
  val startedText = "Started"
  val stillOwnText = "Do you still own the property?"
  val stillOwnAgentText = "Does your client still own the property?"
  val stillOccupyText = "Do you still own the property?"
  val stillOccupyAgentText = "Does your client still own the property?"
  val stillOwnOccupyText = "Do you still own the property?"
  val stillOwnOccupyAgentText = "Does your client still own the property?"
  val changeText = "Change"
  val lastDayOwnerText = "Last day as owner"
  val lastDayOccupierText = "Last day as occupier"
  val lastDayOwnerOccupierText = "Last day as owner and occupier"
  val evidenceText = "Evidence"
  val declarationText = "Declaration"
  val youCouldBeText = "You could be taken to court if you knowingly submit false information."
  val iDeclareText = "I declare that the information I have given is correct and complete. The evidence I have uploaded contains proof of my connection to the property for dates that overlap with the period I have stated."
  val iDeclareAgentText = "I declare that the information I have given is correct and complete. The evidence I have uploaded contains proof of my client’s connection to the property for dates that overlap with the period I have stated."
  val confirmText = "Confirm and send"

  val titleTextWelsh = "Gwiriwch a chadarnhau eich manylion - Valuation Office Agency - GOV.UK"
  val captionTextWelsh = "Ychwanegu eiddo"
  val headerTextWelsh = "Gwiriwch a chadarnhau eich manylion"
  val addressTextWelsh = "Cyfeiriad"
  val connectionToPropertyTextWelsh = "Cysylltiad â’r eiddo"
  val startedTextWelsh = "Wedi dechrau"
  val stillOwnTextWelsh = "Ydych chi yn parhau yn berchen ar gyfer yr eiddo?"
  val stillOwnAgentTextWelsh = "Does your client still own the property?"
  val stillOccupyTextWelsh = "Ydych chi yn parhau yn meddiannu ar gyfer yr eiddo?"
  val stillOccupyAgentTextWelsh = "Does your client still occupy the property?"
  val stillOwnOccupyTextWelsh = "Ydych chi yn parhau yn berchen ac yn meddiannu ar gyfer yr eiddo?"
  val stillOwnOccupyAgentTextWelsh = "Does your client still own and occupy the property?"
  val changeTextWelsh = "Newid Tystiolaeth"
  val lastDayOwnerTextWelsh = "Diwrnod olaf fel perchennog"
  val lastDayOccupierTextWelsh = "Diwrnod olaf fel meddiannydd"
  val lastDayOwnerOccupierTextWelsh = "Diwrnod olaf fel perchennog a meddiannydd"
  val evidenceTextWelsh = "Tystiolaeth"
  val declarationTextWelsh = "Datganiad"
  val youCouldBeTextWelsh = "Gallwch gael eich anfon i’r llys os byddwch yn cyflwyno gwybodaeth ffug yn fwriadol."
  val iDeclareTextWelsh = "Rwy’n datgan bod y wybodaeth a roddais yn gywir ac yn gyflawn. Mae’r dystiolaeth yr wyf wedi’i lanlwytho yn cynnwys prawf o fy nghysylltiad â’r eiddo ar gyfer y ddyddiadau sy’n ymestyn dros y cyfnod yr wyf wedi’i nodi."
  val iDeclareAgentTextWelsh = "I declare that the information I have given is correct and complete. The evidence I have uploaded contains proof of my client’s connection to the property for dates that overlap with the period I have stated."
  val confirmTextWelsh = "Cadarnhau ac anfon"

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
  val yourClientPropertiesHref = "/business-rates-dashboard/selected-client-properties?clientOrganisationId=123&clientName=Client+Name"
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
        )))

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/summary")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
