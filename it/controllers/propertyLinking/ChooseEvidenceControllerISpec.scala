package controllers.propertyLinking

import base.ISpecBase
import binders.propertylinks.ClaimPropertyReturnToPage
import com.github.tomakehurst.wiremock.client.WireMock._
import models.{ClientDetails, EvidenceType, Lease, License, LinkingSession, NoLeaseOrLicense, Occupier, Owner, OwnerOccupier, PropertyOccupancy, PropertyOwnership, PropertyRelationship}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.PropertyLinkingSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class ChooseEvidenceControllerISpec extends ISpecBase {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockPropertyLinkingSessionRepository: PropertyLinkingSessionRepository = app.injector.instanceOf[PropertyLinkingSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Do you have a business rates bill for this property? - Valuation Office Agency - GOV.UK"
  val occupierTitleText = "Do you have a lease or a licence to occupy for this property? - Valuation Office Agency - GOV.UK"
  val clientTitleText = "Do you have your client’s business rates bill for this property? - Valuation Office Agency - GOV.UK"
  val clientOccupierTitleText = "Do you have your client’s lease or licence to occupy for this property? - Valuation Office Agency - GOV.UK"
  val headingText = "Do you have a business rates bill for this property?"
  val occupierHeadingText = "Do you have a lease or a licence to occupy for this property?"
  val clientHeadingText = "Do you have your client’s business rates bill for this property?"
  val clientOccupierHeadingText = "Do you have your client’s lease or licence to occupy for this property?"
  val captionText = "Add a property"
  val backLinkText = "Back"
  val localCouncilReferenceText = "Local council reference: 2050466366770"
  val propertyText = "Property: Test Address, Test Lane, T35 T3R"
  val hintText = "The bill should be for Test Address, Test Lane, T35 T3R."
  val occupierHintText = "The lease or licence to occupy should be for Test Address, Test Lane, T35 T3R."
  val yesRadioText = "Yes"
  val noRadioText = "No"
  val leaseRadioText = "I have a lease"
  val licenceToOccupyRadioText = "I have a licence to occupy"
  val noLeaseOrLicenceRadioText = "I do not have a lease or licence to occupy"
  val continueButtonText = "Continue"
  val errorText = "Error:"
  val errorSummaryText = "There is a problem"
  val errorMessageText = "Select an option"
  val occupierErrorMessageText = "Select if you have a lease or licence to occupy for this property"

  val titleTextWelsh = "Oes gennych chi fil ardrethi busnes ar gyfer yr eiddo hwn? - Valuation Office Agency - GOV.UK"
  val occupierTitleTextWelsh = "A oes gennych brydles neu drwydded i feddiannu’r eiddo hwn? - Valuation Office Agency - GOV.UK"
  val clientTitleTextWelsh = "Oes gennych chi gopi o fil ardrethi busnes eich cleient ar gyfer yr eiddo hwn? - Valuation Office Agency - GOV.UK"
  val clientOccupierTitleTextWelsh = "Oes gennych chi brydles neu drwydded eich cleient i feddiannu ar gyfer yr eiddo hwn? - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Oes gennych chi fil ardrethi busnes ar gyfer yr eiddo hwn?"
  val occupierHeadingTextWelsh = "A oes gennych brydles neu drwydded i feddiannu’r eiddo hwn?"
  val clientHeadingTextWelsh = "Oes gennych chi gopi o fil ardrethi busnes eich cleient ar gyfer yr eiddo hwn?"
  val clientOccupierHeadingTextWelsh = "Oes gennych chi brydles neu drwydded eich cleient i feddiannu ar gyfer yr eiddo hwn?"
  val captionTextWelsh = "Ychwanegu eiddo"
  val backLinkTextWelsh = "Yn ôl"
  val localCouncilReferenceTextWelsh = "Cyfeirnod yr awdurdod lleol: 2050466366770"
  val propertyTextWelsh = "Eiddo: Test Address, Test Lane, T35 T3R"
  val hintTextWelsh = "Dylai’r bil ardrethi busnes hwn fod y bil mwyaf diweddar sydd gennych ar gyfer yr eiddo, ar gyfer y cyfnod mai chi oedd y perchennog neu’r meddiannydd." //VTCCA-6436 raised as this doesn't match the English text
  val occupierHintTextWelsh = "Dylai’r brydles neu’r drwydded i feddiannu fod ar gyfer Test Address, Test Lane, T35 T3R."
  val yesRadioTextWelsh = "Oes"
  val noRadioTextWelsh = "Nac oes"
  val leaseRadioTextWelsh = "Mae gen i brydles"
  val licenceToOccupyRadioTextWelsh = "Mae gen i drwydded i feddiannu"
  val noLeaseOrLicenceRadioTextWelsh = "Nid oes gennyf brydles na thrwydded i feddiannu"
  val continueButtonTextWelsh = "Parhau"
  val errorTextWelsh = "Gwall:"
  val errorSummaryTextWelsh = "Mae yna broblem"
  val errorMessageTextWelsh = "Dewis opsiwn"
  val occupierErrorMessageTextWelsh = "Dewiswch os oes gennych brydles neu drwydded i feddiannu ar gyfer yr eiddo hwn"

  val headingLocator = "h1"
  val captionLocator = "#main-content > div > div > h2"
  val backLinkLocator = "#back-link"
  val localCouncilReferenceLocator = "#local-authority-reference"
  val propertyLocator = "#address"
  val hintTextLocator = "#hasRatesBill-hint"
  val occupierHintTextLocator = "#occupierEvidenceType-hint"
  val yesRadioLocator = "#hasRatesBill-yes"
  val yesRadioLabelLocator = "#hasRatesBill > div:nth-child(1) > label"
  val noRadioLocator = "#hasRatesBill-no"
  val noRadioLabelLocator = "#hasRatesBill > div:nth-child(2) > label"
  val leaseRadioLocator = "#occupierEvidenceType"
  val leaseRadioLabelLocator = "#occupier-evidence-type > div:nth-child(1) > label"
  val licenceToOccupyRadioLocator = "#occupierEvidenceType-2"
  val licenceToOccupyRadioLabelLocator = "#occupier-evidence-type > div:nth-child(2) > label"
  val noLeaseOrLicenceRadioLocator = "#occupierEvidenceType-3"
  val noLeaseOrLicenceRadioLabelLocator = "#occupier-evidence-type > div:nth-child(3) > label"
  val continueButtonLocator = "#continue"
  val errorSummaryLocator = "#main-content > div > div > div.govuk-error-summary > div > h2"
  val errorMessageLocator = "#main-content > div > div > div.govuk-error-summary > div > div > ul > li > a"
  val radioErrorMessageLocator = "#hasRatesBill-error"
  val occupierRadioErrorMessageLocator = "#occupierEvidenceType-error"

  val backLinkHref = "/business-rates-property-linking/my-organisation/claim/property-links/occupancy"
  val errorMessageHref = "#hasRatesBill-yes"
  val occupierErrorMessageHref = "#occupierEvidenceType"
  val uploadEvidenceRedirectUrl = (evidenceType: String) => s"/business-rates-property-linking/my-organisation/claim/property-links/evidence/$evidenceType/upload"

  // GET
  "ChooseEvidenceController.show displays the correct content in English for an IP that is the Owner" which {
    lazy val document = getEvidencePage(language = English, userIsAgent = false, relationship = "Owner")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $hintText" in {
      document.select(hintTextLocator).text shouldBe hintText
    }

    s"has an unselected $yesRadioText radio button" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioText
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioText
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.show displays the correct content in English for an IP that is the Occupier" which {
    lazy val document = getEvidencePage(language = English, userIsAgent = false, relationship = "Occupier")

    s"has a title of $occupierTitleText" in {
      document.title() shouldBe occupierTitleText
    }

    s"has a heading of $occupierHeadingText" in {
      document.getElementsByTag(headingLocator).text shouldBe occupierHeadingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $occupierHintText" in {
      document.select(occupierHintTextLocator).text shouldBe occupierHintText
    }

    s"has an unselected $leaseRadioText radio button" in {
      document.select(leaseRadioLabelLocator).text shouldBe leaseRadioText
      document.select(leaseRadioLocator).hasAttr("checked") shouldBe false
      document.select(leaseRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $licenceToOccupyRadioText radio button" in {
      document.select(licenceToOccupyRadioLabelLocator).text shouldBe licenceToOccupyRadioText
      document.select(licenceToOccupyRadioLocator).hasAttr("checked") shouldBe false
      document.select(licenceToOccupyRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noLeaseOrLicenceRadioText radio button" in {
      document.select(noLeaseOrLicenceRadioLabelLocator).text shouldBe noLeaseOrLicenceRadioText
      document.select(noLeaseOrLicenceRadioLocator).hasAttr("checked") shouldBe false
      document.select(noLeaseOrLicenceRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.show displays the correct content in English for an IP that is the Owner and occupier" which {
    lazy val document = getEvidencePage(language = English, userIsAgent = false, relationship = "Owner and occupier")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $hintText" in {
      document.select(hintTextLocator).text shouldBe hintText
    }

    s"has an unselected $yesRadioText radio button" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioText
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioText
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.show displays the correct content in English for an Agent that is the Owner" which {
    lazy val document = getEvidencePage(language = English, userIsAgent = true, relationship = "Owner")

    s"has a title of $clientTitleText" in {
      document.title() shouldBe clientTitleText
    }

    s"has a heading of $clientHeadingText" in {
      document.getElementsByTag(headingLocator).text shouldBe clientHeadingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $hintText" in {
      document.select(hintTextLocator).text shouldBe hintText
    }

    s"has an unselected $yesRadioText radio button" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioText
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioText
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.show displays the correct content in English for an Agent that is the Occupier" which {
    lazy val document = getEvidencePage(language = English, userIsAgent = true, relationship = "Occupier")

    s"has a title of $clientOccupierTitleText" in {
      document.title() shouldBe clientOccupierTitleText
    }

    s"has a heading of $clientOccupierHeadingText" in {
      document.getElementsByTag(headingLocator).text shouldBe clientOccupierHeadingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $occupierHintText" in {
      document.select(occupierHintTextLocator).text shouldBe occupierHintText
    }

    s"has an unselected $leaseRadioText radio button" in {
      document.select(leaseRadioLabelLocator).text shouldBe leaseRadioText
      document.select(leaseRadioLocator).hasAttr("checked") shouldBe false
      document.select(leaseRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $licenceToOccupyRadioText radio button" in {
      document.select(licenceToOccupyRadioLabelLocator).text shouldBe licenceToOccupyRadioText
      document.select(licenceToOccupyRadioLocator).hasAttr("checked") shouldBe false
      document.select(licenceToOccupyRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noLeaseOrLicenceRadioText radio button" in {
      document.select(noLeaseOrLicenceRadioLabelLocator).text shouldBe noLeaseOrLicenceRadioText
      document.select(noLeaseOrLicenceRadioLocator).hasAttr("checked") shouldBe false
      document.select(noLeaseOrLicenceRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.show displays the correct content in English for an Agent that is the Owner and occupier" which {
    lazy val document = getEvidencePage(language = English, userIsAgent = true, relationship = "Owner and occupier")

    s"has a title of $clientTitleText" in {
      document.title() shouldBe clientTitleText
    }

    s"has a heading of $clientHeadingText" in {
      document.getElementsByTag(headingLocator).text shouldBe clientHeadingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $hintText" in {
      document.select(hintTextLocator).text shouldBe hintText
    }

    s"has an unselected $yesRadioText radio button" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioText
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioText
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.show displays the correct content in Welsh for an IP that is the Owner" which {
    lazy val document = getEvidencePage(language = Welsh, userIsAgent = false, relationship = "Owner")

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $hintText in Welsh" in {
      document.select(hintTextLocator).text shouldBe hintTextWelsh
    }

    s"has an unselected $yesRadioText radio button in Welsh" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioTextWelsh
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button in Welsh" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioTextWelsh
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ChooseEvidenceController.show displays the correct content in Welsh for an IP that is the Occupier" which {
    lazy val document = getEvidencePage(language = Welsh, userIsAgent = false, relationship = "Occupier")

    s"has a title of $occupierTitleText in Welsh" in {
      document.title() shouldBe occupierTitleTextWelsh
    }

    s"has a heading of $occupierHeadingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe occupierHeadingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $occupierHintText in Welsh" in {
      document.select(occupierHintTextLocator).text shouldBe occupierHintTextWelsh
    }

    s"has an unselected $leaseRadioText radio button in Welsh" in {
      document.select(leaseRadioLabelLocator).text shouldBe leaseRadioTextWelsh
      document.select(leaseRadioLocator).hasAttr("checked") shouldBe false
      document.select(leaseRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $licenceToOccupyRadioText radio button in Welsh" in {
      document.select(licenceToOccupyRadioLabelLocator).text shouldBe licenceToOccupyRadioTextWelsh
      document.select(licenceToOccupyRadioLocator).hasAttr("checked") shouldBe false
      document.select(licenceToOccupyRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noLeaseOrLicenceRadioText radio button in Welsh" in {
      document.select(noLeaseOrLicenceRadioLabelLocator).text shouldBe noLeaseOrLicenceRadioTextWelsh
      document.select(noLeaseOrLicenceRadioLocator).hasAttr("checked") shouldBe false
      document.select(noLeaseOrLicenceRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ChooseEvidenceController.show displays the correct content in Welsh for an IP that is the Owner and occupier" which {
    lazy val document = getEvidencePage(language = Welsh, userIsAgent = false, relationship = "Owner and occupier")

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $hintText in Welsh" in {
      document.select(hintTextLocator).text shouldBe hintTextWelsh
    }

    s"has an unselected $yesRadioText radio button in Welsh" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioTextWelsh
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button in Welsh" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioTextWelsh
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ChooseEvidenceController.show displays the correct content in Welsh for an Agent that is the Owner" which {
    lazy val document = getEvidencePage(language = Welsh, userIsAgent = true, relationship = "Owner")

    s"has a title of $clientTitleText in Welsh" in {
      document.title() shouldBe clientTitleTextWelsh
    }

    s"has a heading of $clientHeadingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe clientHeadingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $hintText in Welsh" in {
      document.select(hintTextLocator).text shouldBe hintTextWelsh
    }

    s"has an unselected $yesRadioText radio button in Welsh" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioTextWelsh
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button in Welsh" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioTextWelsh
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ChooseEvidenceController.show displays the correct content in Welsh for an Agent that is the Occupier" which {
    lazy val document = getEvidencePage(language = Welsh, userIsAgent = true, relationship = "Occupier")

    s"has a title of $clientOccupierTitleText in Welsh" in {
      document.title() shouldBe clientOccupierTitleTextWelsh
    }

    s"has a heading of $clientOccupierHeadingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe clientOccupierHeadingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $occupierHintText in Welsh" in {
      document.select(occupierHintTextLocator).text shouldBe occupierHintTextWelsh
    }

    s"has an unselected $leaseRadioText radio button in Welsh" in {
      document.select(leaseRadioLabelLocator).text shouldBe leaseRadioTextWelsh
      document.select(leaseRadioLocator).hasAttr("checked") shouldBe false
      document.select(leaseRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $licenceToOccupyRadioText radio button in Welsh" in {
      document.select(licenceToOccupyRadioLabelLocator).text shouldBe licenceToOccupyRadioTextWelsh
      document.select(licenceToOccupyRadioLocator).hasAttr("checked") shouldBe false
      document.select(licenceToOccupyRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noLeaseOrLicenceRadioText radio button in Welsh" in {
      document.select(noLeaseOrLicenceRadioLabelLocator).text shouldBe noLeaseOrLicenceRadioTextWelsh
      document.select(noLeaseOrLicenceRadioLocator).hasAttr("checked") shouldBe false
      document.select(noLeaseOrLicenceRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ChooseEvidenceController.show displays the correct content in Welsh for an Agent that is the Owner and occupier" which {
    lazy val document = getEvidencePage(language = Welsh, userIsAgent = true, relationship = "Owner and occupier")

    s"has a title of $clientTitleText in Welsh" in {
      document.title() shouldBe clientTitleTextWelsh
    }

    s"has a heading of $clientHeadingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe clientHeadingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $hintText in Welsh" in {
      document.select(hintTextLocator).text shouldBe hintTextWelsh
    }

    s"has an unselected $yesRadioText radio button in Welsh" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioTextWelsh
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button in Welsh" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioTextWelsh
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  // From CYA
  "ChooseEvidenceController.show displays the correct content in English if a user comes back from the Check Your Answers page (Owner - Yes)" which {
    lazy val document = getEvidencePage(language = English, userIsAgent = false, relationship = "Owner", selectedOptionFromCya = Some("Business rates bill"))

    s"sets fromCya to false in the session" in {
      await(mockPropertyLinkingSessionRepository.get[LinkingSession]).map(_.fromCya shouldBe Some(false))
    }

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $hintText" in {
      document.select(hintTextLocator).text shouldBe hintText
    }

    s"has an unselected $yesRadioText radio button" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioText
      document.select(yesRadioLocator).hasAttr("checked") shouldBe true
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioText
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.show displays the correct content in English if a user comes back from the Check Your Answers page (Occupier - Lease)" which {
    lazy val document = getEvidencePage(language = English, userIsAgent = true, relationship = "Occupier", selectedOptionFromCya = Some("Lease"))

    s"sets fromCya to false in the session" in {
      await(mockPropertyLinkingSessionRepository.get[LinkingSession]).map(_.fromCya shouldBe Some(false))
    }

    s"has a title of $clientOccupierTitleText" in {
      document.title() shouldBe clientOccupierTitleText
    }

    s"has a heading of $clientOccupierHeadingText" in {
      document.getElementsByTag(headingLocator).text shouldBe clientOccupierHeadingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $occupierHintText" in {
      document.select(occupierHintTextLocator).text shouldBe occupierHintText
    }

    s"has an unselected $leaseRadioText radio button" in {
      document.select(leaseRadioLabelLocator).text shouldBe leaseRadioText
      document.select(leaseRadioLocator).hasAttr("checked") shouldBe true
      document.select(leaseRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $licenceToOccupyRadioText radio button" in {
      document.select(licenceToOccupyRadioLabelLocator).text shouldBe licenceToOccupyRadioText
      document.select(licenceToOccupyRadioLocator).hasAttr("checked") shouldBe false
      document.select(licenceToOccupyRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noLeaseOrLicenceRadioText radio button" in {
      document.select(noLeaseOrLicenceRadioLabelLocator).text shouldBe noLeaseOrLicenceRadioText
      document.select(noLeaseOrLicenceRadioLocator).hasAttr("checked") shouldBe false
      document.select(noLeaseOrLicenceRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.show displays the correct content in English if a user comes back from the Check Your Answers page (Occupier - Licence to occupy)" which {
    lazy val document = getEvidencePage(language = English, userIsAgent = true, relationship = "Occupier", selectedOptionFromCya = Some("Licence to occupy"))

    s"sets fromCya to false in the session" in {
      await(mockPropertyLinkingSessionRepository.get[LinkingSession]).map(_.fromCya shouldBe Some(false))
    }

    s"has a title of $clientOccupierTitleText" in {
      document.title() shouldBe clientOccupierTitleText
    }

    s"has a heading of $clientOccupierHeadingText" in {
      document.getElementsByTag(headingLocator).text shouldBe clientOccupierHeadingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $occupierHintText" in {
      document.select(occupierHintTextLocator).text shouldBe occupierHintText
    }

    s"has an unselected $leaseRadioText radio button" in {
      document.select(leaseRadioLabelLocator).text shouldBe leaseRadioText
      document.select(leaseRadioLocator).hasAttr("checked") shouldBe false
      document.select(leaseRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $licenceToOccupyRadioText radio button" in {
      document.select(licenceToOccupyRadioLabelLocator).text shouldBe licenceToOccupyRadioText
      document.select(licenceToOccupyRadioLocator).hasAttr("checked") shouldBe true
      document.select(licenceToOccupyRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noLeaseOrLicenceRadioText radio button" in {
      document.select(noLeaseOrLicenceRadioLabelLocator).text shouldBe noLeaseOrLicenceRadioText
      document.select(noLeaseOrLicenceRadioLocator).hasAttr("checked") shouldBe false
      document.select(noLeaseOrLicenceRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.show displays the correct content in Welsh if a user comes back from the Check Your Answers page (Owner and occupier - No)" which {
    lazy val document = getEvidencePage(language = Welsh, userIsAgent = true, relationship = "Owner and occupier", selectedOptionFromCya = Some("No business rates bill"))

    s"sets fromCya to false in the session" in {
      await(mockPropertyLinkingSessionRepository.get[LinkingSession]).map(_.fromCya shouldBe Some(false))
    }

    s"has a title of $clientTitleText in Welsh" in {
      document.title() shouldBe clientTitleTextWelsh
    }

    s"has a heading of $clientHeadingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe clientHeadingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $hintText in Welsh" in {
      document.select(hintTextLocator).text shouldBe hintTextWelsh
    }

    s"has an unselected $yesRadioText radio button in Welsh" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioTextWelsh
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button in Welsh" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioTextWelsh
      document.select(noRadioLocator).hasAttr("checked") shouldBe true
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ChooseEvidenceController.show displays the correct content in Welsh if a user comes back from the Check Your Answers page (Occupier - No lease or licence)" which {
    lazy val document = getEvidencePage(language = Welsh, userIsAgent = false, relationship = "Occupier", selectedOptionFromCya = Some("No lease or licence to occupy"))

    s"sets fromCya to false in the session" in {
      await(mockPropertyLinkingSessionRepository.get[LinkingSession]).map(_.fromCya shouldBe Some(false))
    }

    s"has a title of $occupierTitleText in Welsh" in {
      document.title() shouldBe occupierTitleTextWelsh
    }

    s"has a heading of $occupierHeadingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe occupierHeadingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $occupierHintText in Welsh" in {
      document.select(occupierHintTextLocator).text shouldBe occupierHintTextWelsh
    }

    s"has an unselected $leaseRadioText radio button in Welsh" in {
      document.select(leaseRadioLabelLocator).text shouldBe leaseRadioTextWelsh
      document.select(leaseRadioLocator).hasAttr("checked") shouldBe false
      document.select(leaseRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $licenceToOccupyRadioText radio button in Welsh" in {
      document.select(licenceToOccupyRadioLabelLocator).text shouldBe licenceToOccupyRadioTextWelsh
      document.select(licenceToOccupyRadioLocator).hasAttr("checked") shouldBe false
      document.select(licenceToOccupyRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noLeaseOrLicenceRadioText radio button in Welsh" in {
      document.select(noLeaseOrLicenceRadioLabelLocator).text shouldBe noLeaseOrLicenceRadioTextWelsh
      document.select(noLeaseOrLicenceRadioLocator).hasAttr("checked") shouldBe true
      document.select(noLeaseOrLicenceRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  // POST
  "ChooseEvidenceController.submit displays an error message in English when an IP doesn't select a radio" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = false, relationship = "Owner", selectedOption = None)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorText $titleText" in {
      document.title() shouldBe s"$errorText $titleText"
    }

    s"has an error summary with an error of  $errorMessageText" in {
      document.select(errorSummaryLocator).text shouldBe errorSummaryText
      document.select(errorMessageLocator).text shouldBe errorMessageText
      document.select(errorMessageLocator).attr("href") shouldBe errorMessageHref
    }

    s"has an error of $errorText $errorMessageText above the radio buttons" in {
      document.select(radioErrorMessageLocator).text shouldBe s"$errorText $errorMessageText"
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $hintText" in {
      document.select(hintTextLocator).text shouldBe hintText
    }

    s"has an unselected $yesRadioText radio button" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioText
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioText
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.submit displays an error message in Welsh when an IP doesn't select a radio" which {
    lazy val res = postEvidencePage(language = Welsh, userIsAgent = false, relationship = "Owner and occupier", selectedOption = None)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorText $titleText in Welsh" in {
      document.title() shouldBe s"$errorTextWelsh $titleTextWelsh"
    }

    s"has an error summary with an error of  $errorMessageText in Welsh" in {
      document.select(errorSummaryLocator).text shouldBe errorSummaryTextWelsh
      document.select(errorMessageLocator).text shouldBe errorMessageTextWelsh
      document.select(errorMessageLocator).attr("href") shouldBe errorMessageHref
    }

    s"has an error of $errorText $errorMessageText above the radio buttons in Welsh" in {
      document.select(radioErrorMessageLocator).text shouldBe s"$errorTextWelsh $errorMessageTextWelsh"
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $hintText in Welsh" in {
      document.select(hintTextLocator).text shouldBe hintTextWelsh
    }

    s"has an unselected $yesRadioText radio button in Welsh" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioTextWelsh
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button in Welsh" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioTextWelsh
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ChooseEvidenceController.submit displays an error message in English when an Agent doesn't select a radio" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = true, relationship = "Owner and occupier", selectedOption = None)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorText $clientTitleText" in {
      document.title() shouldBe s"$errorText $clientTitleText"
    }

    s"has an error summary with an error of  $errorMessageText" in {
      document.select(errorSummaryLocator).text shouldBe errorSummaryText
      document.select(errorMessageLocator).text shouldBe errorMessageText
      document.select(errorMessageLocator).attr("href") shouldBe errorMessageHref
    }

    s"has an error of $errorText $errorMessageText above the radio buttons" in {
      document.select(radioErrorMessageLocator).text shouldBe s"$errorText $errorMessageText"
    }

    s"has a heading of $clientHeadingText" in {
      document.getElementsByTag(headingLocator).text shouldBe clientHeadingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $hintText" in {
      document.select(hintTextLocator).text shouldBe hintText
    }

    s"has an unselected $yesRadioText radio button" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioText
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioText
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.submit displays an error message in Welsh when an Agent doesn't select a radio" which {
    lazy val res = postEvidencePage(language = Welsh, userIsAgent = true, relationship = "Owner", selectedOption = None)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorText $clientTitleText in Welsh" in {
      document.title() shouldBe s"$errorTextWelsh $clientTitleTextWelsh"
    }

    s"has an error summary with an error of  $errorMessageText in Welsh" in {
      document.select(errorSummaryLocator).text shouldBe errorSummaryTextWelsh
      document.select(errorMessageLocator).text shouldBe errorMessageTextWelsh
      document.select(errorMessageLocator).attr("href") shouldBe errorMessageHref
    }

    s"has an error of $errorText $errorMessageText above the radio buttons in Welsh" in {
      document.select(radioErrorMessageLocator).text shouldBe s"$errorTextWelsh $errorMessageTextWelsh"
    }

    s"has a heading of $clientHeadingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe clientHeadingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $hintText in Welsh" in {
      document.select(hintTextLocator).text shouldBe hintTextWelsh
    }

    s"has an unselected $yesRadioText radio button in Welsh" in {
      document.select(yesRadioLabelLocator).text shouldBe yesRadioTextWelsh
      document.select(yesRadioLocator).hasAttr("checked") shouldBe false
      document.select(yesRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noRadioText radio button in Welsh" in {
      document.select(noRadioLabelLocator).text shouldBe noRadioTextWelsh
      document.select(noRadioLocator).hasAttr("checked") shouldBe false
      document.select(noRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ChooseEvidenceController.submit returns a 303 with a redirect location of the Upload business rates bill page when an IP selects the 'Yes' radio" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = false, relationship = "Owner and occupier", selectedOption = Some("Yes"))

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(uploadEvidenceRedirectUrl("RATES_BILL"))
    }
  }

  "ChooseEvidenceController.submit returns a 303 with a redirect location of the Upload other evidence page when an Agent selects the 'No' radio" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = true, relationship = "Owner", selectedOption = Some("No"))

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(uploadEvidenceRedirectUrl("OTHER"))
    }
  }

  "ChooseEvidenceController.submit returns a 303 with a redirect location of the Upload business rates bill page when an Agent selects the 'Yes' radio after coming from the CYA page" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = true, relationship = "Owner and occupier", selectedOption = Some("Yes"), fromCya = true)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(uploadEvidenceRedirectUrl("RATES_BILL"))
    }
  }

  "ChooseEvidenceController.submit returns a 303 with a redirect location of the Upload other evidence page when an IP selects the 'No' radio after coming from the CYA page" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = false, relationship = "Owner", selectedOption = Some("No"), fromCya = true)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(uploadEvidenceRedirectUrl("OTHER"))
    }
  }

  "ChooseEvidenceController.submitOccupierForm displays an error message in English when an IP doesn't select a radio" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = false, relationship = "Occupier", selectedOption = None)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorText $occupierTitleText" in {
      document.title() shouldBe s"$errorText $occupierTitleText"
    }

    s"has an error summary with an error of $occupierErrorMessageText" in {
      document.select(errorSummaryLocator).text shouldBe errorSummaryText
      document.select(errorMessageLocator).text shouldBe occupierErrorMessageText
      document.select(errorMessageLocator).attr("href") shouldBe occupierErrorMessageHref
    }

    s"has an error of $errorText $occupierErrorMessageText above the radio buttons" in {
      document.select(occupierRadioErrorMessageLocator).text shouldBe s"$errorText $occupierErrorMessageText"
    }

    s"has a heading of $occupierHeadingText" in {
      document.getElementsByTag(headingLocator).text shouldBe occupierHeadingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $occupierHintText" in {
      document.select(occupierHintTextLocator).text shouldBe occupierHintText
    }

    s"has an unselected $leaseRadioText radio button" in {
      document.select(leaseRadioLabelLocator).text shouldBe leaseRadioText
      document.select(leaseRadioLocator).hasAttr("checked") shouldBe false
      document.select(leaseRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $licenceToOccupyRadioText radio button" in {
      document.select(licenceToOccupyRadioLabelLocator).text shouldBe licenceToOccupyRadioText
      document.select(licenceToOccupyRadioLocator).hasAttr("checked") shouldBe false
      document.select(licenceToOccupyRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noLeaseOrLicenceRadioText radio button" in {
      document.select(noLeaseOrLicenceRadioLabelLocator).text shouldBe noLeaseOrLicenceRadioText
      document.select(noLeaseOrLicenceRadioLocator).hasAttr("checked") shouldBe false
      document.select(noLeaseOrLicenceRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.submitOccupierForm displays an error message in Welsh when an IP doesn't select a radio" which {
    lazy val res = postEvidencePage(language = Welsh, userIsAgent = false, relationship = "Occupier", selectedOption = None)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorText $occupierTitleText in Welsh" in {
      document.title() shouldBe s"$errorTextWelsh $occupierTitleTextWelsh"
    }

    s"has an error summary with an error of $occupierErrorMessageText in Welsh" in {
      document.select(errorSummaryLocator).text shouldBe errorSummaryTextWelsh
      document.select(errorMessageLocator).text shouldBe occupierErrorMessageTextWelsh
      document.select(errorMessageLocator).attr("href") shouldBe occupierErrorMessageHref
    }

    s"has an error of $errorText $occupierErrorMessageText above the radio buttons in Welsh" in {
      document.select(occupierRadioErrorMessageLocator).text shouldBe s"$errorTextWelsh $occupierErrorMessageTextWelsh"
    }

    s"has a heading of $occupierHeadingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe occupierHeadingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $occupierHintText in Welsh" in {
      document.select(occupierHintTextLocator).text shouldBe occupierHintTextWelsh
    }

    s"has an unselected $leaseRadioText radio button in Welsh" in {
      document.select(leaseRadioLabelLocator).text shouldBe leaseRadioTextWelsh
      document.select(leaseRadioLocator).hasAttr("checked") shouldBe false
      document.select(leaseRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $licenceToOccupyRadioText radio button in Welsh" in {
      document.select(licenceToOccupyRadioLabelLocator).text shouldBe licenceToOccupyRadioTextWelsh
      document.select(licenceToOccupyRadioLocator).hasAttr("checked") shouldBe false
      document.select(licenceToOccupyRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noLeaseOrLicenceRadioText radio button in Welsh" in {
      document.select(noLeaseOrLicenceRadioLabelLocator).text shouldBe noLeaseOrLicenceRadioTextWelsh
      document.select(noLeaseOrLicenceRadioLocator).hasAttr("checked") shouldBe false
      document.select(noLeaseOrLicenceRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ChooseEvidenceController.submitOccupierForm displays an error message in English when an Agent doesn't select a radio" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = true, relationship = "Occupier", selectedOption = None)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorText $clientOccupierTitleText" in {
      document.title() shouldBe s"$errorText $clientOccupierTitleText"
    }

    s"has an error summary with an error of $occupierErrorMessageText" in {
      document.select(errorSummaryLocator).text shouldBe errorSummaryText
      document.select(errorMessageLocator).text shouldBe occupierErrorMessageText
      document.select(errorMessageLocator).attr("href") shouldBe occupierErrorMessageHref
    }

    s"has an error of $errorText $occupierErrorMessageText above the radio buttons" in {
      document.select(occupierRadioErrorMessageLocator).text shouldBe s"$errorText $occupierErrorMessageText"
    }

    s"has a heading of $clientOccupierHeadingText" in {
      document.getElementsByTag(headingLocator).text shouldBe clientOccupierHeadingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link that goes to the occupancy page" in {
      document.select(backLinkLocator).text shouldBe backLinkText
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference" in {
      document.select(propertyLocator).text shouldBe propertyText
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceText
    }

    s"has hint text of $occupierHintText" in {
      document.select(occupierHintTextLocator).text shouldBe occupierHintText
    }

    s"has an unselected $leaseRadioText radio button" in {
      document.select(leaseRadioLabelLocator).text shouldBe leaseRadioText
      document.select(leaseRadioLocator).hasAttr("checked") shouldBe false
      document.select(leaseRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $licenceToOccupyRadioText radio button" in {
      document.select(licenceToOccupyRadioLabelLocator).text shouldBe licenceToOccupyRadioText
      document.select(licenceToOccupyRadioLocator).hasAttr("checked") shouldBe false
      document.select(licenceToOccupyRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noLeaseOrLicenceRadioText radio button" in {
      document.select(noLeaseOrLicenceRadioLabelLocator).text shouldBe noLeaseOrLicenceRadioText
      document.select(noLeaseOrLicenceRadioLocator).hasAttr("checked") shouldBe false
      document.select(noLeaseOrLicenceRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button" in {
      document.select(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ChooseEvidenceController.submitOccupierForm displays an error message in Welsh when an Agent doesn't select a radio" which {
    lazy val res = postEvidencePage(language = Welsh, userIsAgent = true, relationship = "Occupier", selectedOption = None)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorText $clientOccupierTitleText in Welsh" in {
      document.title() shouldBe s"$errorTextWelsh $clientOccupierTitleTextWelsh"
    }

    s"has an error summary with an error of $occupierErrorMessageText in Welsh" in {
      document.select(errorSummaryLocator).text shouldBe errorSummaryTextWelsh
      document.select(errorMessageLocator).text shouldBe occupierErrorMessageTextWelsh
      document.select(errorMessageLocator).attr("href") shouldBe occupierErrorMessageHref
    }

    s"has an error of $errorText $occupierErrorMessageText above the radio buttons in Welsh" in {
      document.select(occupierRadioErrorMessageLocator).text shouldBe s"$errorTextWelsh $occupierErrorMessageTextWelsh"
    }

    s"has a heading of $clientOccupierHeadingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe clientOccupierHeadingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link that goes to the occupancy page in Welsh" in {
      document.select(backLinkLocator).text shouldBe backLinkTextWelsh
      document.select(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    "has the address and local council reference in Welsh" in {
      document.select(propertyLocator).text shouldBe propertyTextWelsh
      document.select(localCouncilReferenceLocator).text shouldBe localCouncilReferenceTextWelsh
    }

    s"has hint text of $occupierHintText in Welsh" in {
      document.select(occupierHintTextLocator).text shouldBe occupierHintTextWelsh
    }

    s"has an unselected $leaseRadioText radio button in Welsh" in {
      document.select(leaseRadioLabelLocator).text shouldBe leaseRadioTextWelsh
      document.select(leaseRadioLocator).hasAttr("checked") shouldBe false
      document.select(leaseRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $licenceToOccupyRadioText radio button in Welsh" in {
      document.select(licenceToOccupyRadioLabelLocator).text shouldBe licenceToOccupyRadioTextWelsh
      document.select(licenceToOccupyRadioLocator).hasAttr("checked") shouldBe false
      document.select(licenceToOccupyRadioLocator).attr("type") shouldBe "radio"
    }

    s"has an unselected $noLeaseOrLicenceRadioText radio button in Welsh" in {
      document.select(noLeaseOrLicenceRadioLabelLocator).text shouldBe noLeaseOrLicenceRadioTextWelsh
      document.select(noLeaseOrLicenceRadioLocator).hasAttr("checked") shouldBe false
      document.select(noLeaseOrLicenceRadioLocator).attr("type") shouldBe "radio"
    }

    s"has a $continueButtonText button in Welsh" in {
      document.select(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ChooseEvidenceController.submitOccupierForm returns a 303 with a redirect location of the Upload evidence page when an IP selects the 'Lease' radio" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = false, relationship = "Occupier", selectedOption = Some("Lease"))

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(uploadEvidenceRedirectUrl("LEASE"))
    }
  }

  "ChooseEvidenceController.submitOccupierForm returns a 303 with a redirect location of the Upload evidence page when an Agent selects the 'Licence to occupy' radio" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = true, relationship = "Occupier", selectedOption = Some("Licence to occupy"))

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(uploadEvidenceRedirectUrl("LICENSE"))
    }
  }

  "ChooseEvidenceController.submitOccupierForm returns a 303 with a redirect location of the Upload other evidence page when an IP selects the 'No lease or licence to occupy' radio" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = false, relationship = "Occupier", selectedOption = Some("No lease or licence to occupy"))

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(uploadEvidenceRedirectUrl("NO_LEASE_OR_LICENSE"))
    }
  }

  "ChooseEvidenceController.submitOccupierForm returns a 303 with a redirect location of the Upload evidence page when an Agent selects the 'Lease' radio after coming from the CYA page" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = true, relationship = "Occupier", selectedOption = Some("Lease"), fromCya = true)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(uploadEvidenceRedirectUrl("LEASE"))
    }
  }

  "ChooseEvidenceController.submitOccupierForm returns a 303 with a redirect location of the Upload evidence page when an IP selects the 'Licence to occupy' radio after coming from the CYA page" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = false, relationship = "Occupier", selectedOption = Some("Licence to occupy"), fromCya = true)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(uploadEvidenceRedirectUrl("LICENSE"))
    }
  }

  "ChooseEvidenceController.submitOccupierForm returns a 303 with a redirect location of the Upload other evidence page when an Agent selects the 'No lease or licence to occupy' radio after coming from the CYA page" which {
    lazy val res = postEvidencePage(language = English, userIsAgent = true, relationship = "Occupier", selectedOption = Some("No lease or licence to occupy"), fromCya = true)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(uploadEvidenceRedirectUrl("NO_LEASE_OR_LICENSE"))
    }
  }

  private def commonSetup(userIsAgent: Boolean, relationship: String, selectedOptionFromCya: Option[String] = None): Unit = {
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

    val propertyRelationship: PropertyRelationship = relationship.toUpperCase match {
      case "OWNER" => PropertyRelationship(capacity = Owner, uarn = 1)
      case "OCCUPIER" => PropertyRelationship(capacity = Occupier, uarn = 1)
      case "OWNER AND OCCUPIER" => PropertyRelationship(capacity = OwnerOccupier, uarn = 1)
    }

    val (hasRatesBill, occupierEvidenceType): (Option[Boolean], Option[EvidenceType]) = selectedOptionFromCya match {
      case Some("Business rates bill") => (Some(true), None)
      case Some("No business rates bill") => (Some(false), None)
      case Some("Lease") => (None, Some(Lease))
      case Some("Licence to occupy") => (None, Some(License))
      case Some("No lease or licence to occupy") => (None, Some(NoLeaseOrLicense))
      case _ => (None, None)
    }

    await(
      mockPropertyLinkingSessionRepository.saveOrUpdate(
        LinkingSession(
          address = "Test Address, Test Lane, T35 T3R",
          uarn = 1L,
          submissionId = "PL-123456",
          personId = 1L,
          earliestStartDate = LocalDate.of(2017, 4, 1),
          propertyRelationship = Some(propertyRelationship),
          propertyOwnership = Some(PropertyOwnership(fromDate = LocalDate.of(2017, 4, 1))),
          propertyOccupancy = Some(PropertyOccupancy(stillOccupied = true)),
          hasRatesBill = hasRatesBill,
          occupierEvidenceType = occupierEvidenceType,
          clientDetails = if (userIsAgent) Some(ClientDetails(123, "Client Name")) else None,
          localAuthorityReference = "2050466366770",
          rtp = ClaimPropertyReturnToPage.FMBR,
          fromCya = Some(selectedOptionFromCya.isDefined),
          isSubmitted = None
        )))
  }

  private def getEvidencePage(language: Language, userIsAgent: Boolean, relationship: String, selectedOptionFromCya: Option[String] = None): Document = {
    commonSetup(userIsAgent, relationship, selectedOptionFromCya)

    val url = if (relationship.equalsIgnoreCase("Occupier")) "occupier-evidence" else "evidence"

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/$url")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def postEvidencePage(language: Language, userIsAgent: Boolean, relationship: String, selectedOption: Option[String], fromCya: Boolean = false) = {
    val evidence = if (fromCya) Some("Evidence type") else None
    commonSetup(userIsAgent, relationship, selectedOptionFromCya = evidence)

    val body = selectedOption match {
      case Some("Yes") => "hasRatesBill" -> Seq("true")
      case Some("No") => "hasRatesBill" -> Seq("false")
      case Some("Lease") => "occupierEvidenceType" -> Seq("lease")
      case Some("Licence to occupy") => "occupierEvidenceType" -> Seq("license")
      case Some("No lease or licence to occupy") => "occupierEvidenceType" -> Seq("noLeaseOrLicense")
      case None => if (relationship.equalsIgnoreCase("Occupier")) "occupierEvidenceType" -> Seq("") else "hasRatesBill" -> Seq("")
    }

    val url = if (relationship.equalsIgnoreCase("Occupier")) "occupier-evidence" else "evidence"

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/$url")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .withFollowRedirects(follow = false)
        .post(Map(body))
    )
  }
}
