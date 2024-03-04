package controllers.propertyLinking

import base.ISpecBase
import binders.propertylinks.ClaimPropertyReturnToPage
import com.github.tomakehurst.wiremock.client.WireMock._
import models.upscan._
import models._
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.PropertyLinkingSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class UploadControllerISpec extends ISpecBase {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockPropertyLinkingSessionRepository: PropertyLinkingSessionRepository = app.injector.instanceOf[PropertyLinkingSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  // Business rates bill page text
  val ratesBillHeadingText = "Upload your business rates bill"
  val ratesBillHeadingTextWelsh = "Lanlwytho eich bil ardrethi busnes"
  val ratesBillHeadingTextAgent = "Upload your client’s business rates bill"
  val ratesBillHeadingTextAgentWelsh = "Lanlwytho bil ardrethi busnes eich cleient"

  // Lease page text
  val leaseHeadingText = "Upload your lease"
  val leaseHeadingTextWelsh = "Lanlwytho eich prydles"
  val leaseHeadingTextAgent = "Upload your client’s lease"
  val leaseHeadingTextAgentWelsh = "Lanlwytho prydles eich cleient"

  // Licence to occupy page text
  val licenceHeadingText = "Upload your licence to occupy"
  val licenceHeadingTextWelsh = "Lanlwytho eich trwydded i feddiannu"
  val licenceHeadingTextAgent = "Upload your client’s licence to occupy"
  val licenceHeadingTextAgentWelsh = "Lanlwytho trwydded eich cleient i feddiannu"

  // Service charge statement
  val serviceChargeHeadingText = "Upload your service charge statement"
  val serviceChargeHeadingTextWelsh = "Lanlwytho eich datganiad tâl gwasanaeth"
  val serviceChargeHeadingTextAgent = "Upload your service charge statement"
  val serviceChargeHeadingTextAgentWelsh = "Lanlwytho eich datganiad tâl gwasanaeth"

  // Stamp duty statement
  val stampDutyHeadingText = "Upload your Stamp Duty Land Tax form"
  val stampDutyHeadingTextWelsh = "Lanlwytho eich Ffurflen Treth Dir y Dreth Stamp"
  val stampDutyHeadingTextAgent = "Upload your Stamp Duty Land Tax form"
  val stampDutyHeadingTextAgentWelsh = "Lanlwytho eich Ffurflen Treth Dir y Dreth Stamp"

  // Land registry statement
  val landRegistryHeadingText = "Upload your Land Registry title"
  val landRegistryHeadingTextWelsh = "Lanlwytho eich teitl gofrestrfa tir"
  val landRegistryHeadingTextAgent = "Upload your Land Registry title"
  val landRegistryHeadingTextAgentWelsh = "Lanlwytho eich teitl gofrestrfa tir"

  // Water rate demand
  val waterRateHeadingText = "Upload your water rate demand"
  val waterRateHeadingTextWelsh = "Lanlwytho eich archeb trethi dŵr"
  val waterRateHeadingTextAgent = "Upload your water rate demand"
  val waterRateHeadingTextAgentWelsh = "Lanlwytho eich archeb trethi dŵr"

  // Utility bill
  val utilityBillHeadingText = "Upload your utility bill"
  val utilityBillHeadingTextWelsh = "Lanlwytho eich bil cyfleustodau"
  val utilityBillHeadingTextAgent = "Upload your utility bill"
  val utilityBillHeadingTextAgentWelsh = "Lanlwytho eich bil cyfleustodau"

  // Other evidence page text
  val otherEvidenceTitleText = "What evidence can you provide? - Valuation Office Agency - GOV.UK"
  val otherEvidenceHeadingText = "What evidence can you provide?"
  val otherEvidenceHintText = "Your evidence should be for Test Address, Test Lane, T35 T3R"
  val businessRatesBillText = "Business rates bill"
  val leaseText = "Lease"
  val licenceToOccupyText = "Licence to occupy"
  val serviceChargeStatementText = "Service charge statement"
  val stampDutyLandTaxText = "Stamp Duty Land Tax form"
  val landRegistryText = "Land Registry title"
  val waterRateText = "Water rate demand"
  val utilityBillText = "Utility bill"
  val orText = "or"
  val cannotProvideEvidenceText = "I cannot provide evidence"
  val otherEvidenceErrorMessageText = "Select evidence type"

  val otherEvidenceTitleTextWelsh = "Pa dystiolaeth allwch chi ei darparu? - Valuation Office Agency - GOV.UK"
  val otherEvidenceHeadingTextWelsh = "Pa dystiolaeth allwch chi ei darparu?"
  val otherEvidenceHintTextWelsh = "Dylai eich tystiolaeth fod ar gyfer Test Address, Test Lane, T35 T3R"
  val businessRatesBillTextWelsh = "Bil ardrethi busnes"
  val leaseTextWelsh = "Prydles"
  val licenceToOccupyTextWelsh = "Trwydded i feddiannu"
  val serviceChargeStatementTextWelsh = "Datganiad tâl gwasanaeth"
  val stampDutyLandTaxTextWelsh = "Ffurflen Treth Dir y Dreth Stamp"
  val landRegistryTextWelsh = "Teitl y Gofrestrfa Tir"
  val waterRateTextWelsh = "Archeb trethi dŵr"
  val utilityBillTextWelsh = "Bil cyfleustodau"
  val orTextWelsh = "neu"
  val cannotProvideEvidenceTextWelsh = "Ni allaf ddarparu tystiolaeth"
  val otherEvidenceErrorMessageTextWelsh = "Dewiswch y math o dystiolaeth"

  // Cannot provide evidence page text
  val cannotProvideEvidenceTitleText = "If you cannot provide evidence - Valuation Office Agency - GOV.UK"
  val cannotProvideEvidenceHeadingText = "If you cannot provide evidence"
  val cannotProvideEvidenceP1 = "If you can’t provide evidence to prove your link to the property, you won’t be able to claim it or view the detailed valuation."
  val cannotProvideEvidenceP2 = "You’ll need to contact us to discuss other documents that prove your link to this property."
  val cannotProvideEvidenceP3 = "Email: ccaservice@voa.gov.uk"
  val cannotProvideEvidenceP1Agent = "If you can’t provide evidence to prove your client’s link to the property, you won’t be able to claim it or view the detailed valuation."
  val cannotProvideEvidenceP2Agent = "You’ll need to contact us to discuss other documents that prove your client’s link to this property."
  val cannotProvideEvidenceEmailText = "ccaservice@voa.gov.uk"

  val cannotProvideEvidenceTitleTextWelsh = "Os na allwch ddarparu - Valuation Office Agency - GOV.UK"
  val cannotProvideEvidenceHeadingTextWelsh = "Os na allwch ddarparu"
  val cannotProvideEvidenceP1Welsh = "Os na allwch ddarparu tystiolaeth i brofi eich cysylltiad â’r eiddo, ni fyddwch yn gallu ei hawlio na gweld y prisiad manwl."
  val cannotProvideEvidenceP2Welsh = "Bydd angen i chi gysylltu â ni i drafod dogfennau eraill sy’n profi eich cysylltiad â’r eiddo hwn."
  val cannotProvideEvidenceP1AgentWelsh = "Os na allwch ddarparu tystiolaeth i brofi eich cysylltiad â’r eiddo, ni fyddwch yn gallu ei hawlio na gweld y prisiad manwl."
  val cannotProvideEvidenceP2AgentWelsh = "Bydd angen i chi gysylltu â ni i drafod dogfennau eraill sy’n profi cysylltiad eich cleient â’r eiddo hwn."
  val cannotProvideEvidenceP3Welsh = "E-bost: ccaservice@voa.gov.uk"

  // Common page text
  val backLinkText = "Back"
  val captionText = "Add a property"
  val hintText = "The file must be a Word document, Excel spreadsheet, PDF or image (PNG or JPG) and be less than 10MB."
  val continueButtonText = "Continue"
  val fileNameText = "test_file.pdf"
  val errorText = "Error:"
  val errorSummaryText = "There is a problem"
  val errorMessageText = "Select a file"

  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Ychwanegu eiddo"
  val hintTextWelsh = "Rhaid i’r ffeil fod yn ddogfen Word, taenlen Excel, PDF neu ddelwedd (PNG neu JPG) a bod yn llai na 10MB."
  val continueButtonTextWelsh = "Parhau"
  val errorTextWelsh = "Gwall:"
  val errorSummaryTextWelsh = "Mae yna broblem"
  val errorMessageTextWelsh = "Dewis bil ardrethi"

  // Locators
  val headingLocator = "h1"
  val captionLocator = "#main-content > div > div > span"
  val backLinkLocator = "#back-link"
  val hintTextLocator = "choose-file-hint"
  val fileUploadComponentLocator = "choose-file"
  val evidenceLocator = "#main-content > div > div > dl > div > dt"
  val fileNameLocator = "#main-content > div > div > dl > div > dd.govuk-summary-list__value"
  val removeFileLocator = "#remove-file"
  val continueButtonLocator = "#continue"
  val errorSummaryTitleLocator = "#main-content > div > div > div.govuk-error-summary > div > h2"
  val errorSummaryMessageLocator = "#main-content > div > div > div.govuk-error-summary > div > div > ul > li > a"
  val errorMessageLocator = "#choose-file-error"

  val otherEvidenceHintLocator = "#evidenceType-hint"
  val otherEvidenceRadioLocator: Int => String = (radioNumber: Int) => s"#upload-evidence-options > div:nth-child($radioNumber) > input"
  val otherEvidenceRadioLabelLocator: Int => String = (radioNumber: Int) => s"#upload-evidence-options > div:nth-child($radioNumber) > label"
  val otherEvidenceSummaryLocator = "#main-content > div > div > div.govuk-error-summary > div > h2"
  val otherEvidenceSummaryMessageLocator = "#main-content > div > div > div.govuk-error-summary > div > div > ul > li > a"
  val otherEvidenceErrorMessageLocator = "#evidenceType-error"
  val orLocator = "#upload-evidence-options > div.govuk-radios__divider"

  val cannotProvideEvidenceP1Locator = "#main-content > div > div > p:nth-child(3)"
  val cannotProvideEvidenceP2Locator = "#main-content > div > div > p:nth-child(4)"
  val cannotProvideEvidenceP3Locator = "#main-content > div > div > p:nth-child(5)"
  val cannotProvideEvidenceEmailLocator = "#main-content > div > div > p:nth-child(5) > a"

  // Links
  val emailHref = "mailto:ccaservice@voa.gov.uk"
  val backToEvidenceQuestionHref = "/business-rates-property-linking/my-organisation/claim/property-links/evidence"
  val backToOtherEvidenceHref = "/business-rates-property-linking/my-organisation/claim/property-links/evidence/OTHER/upload"
  val errorMessageHref = "#choose-file"
  val otherEvidenceErrorMessageHref = "#evidenceType"
  val evidenceTypeRedirectUrl: String => String = (evidenceType: String) => s"/business-rates-property-linking/my-organisation/claim/property-links/evidence/$evidenceType/upload"
  def UploadResultsRedirectUrl(evidenceType: String) = s"/business-rates-property-linking/my-organisation/claim/property-links/evidence/$evidenceType/upload"

  "UploadController show method" should {

    // IP - English
    "display the correct content in English for an IP (Owner) that is uploading a business rates bill" which {
      commonEvidencePageTests(language = English,
        evidenceType = "RATES_BILL",
        userIsAgent = false,
        relationship = "Owner",
        backLink = backToEvidenceQuestionHref,
        fromCya = false)
    }

    "display the correct content in English for an IP (Occupier) that is uploading a lease" which {
      commonEvidencePageTests(language = English,
        evidenceType = "LEASE",
        userIsAgent = false,
        relationship = "Occupier",
        backLink = backToEvidenceQuestionHref,
        fromCya = false)
    }

    "display the correct content in English for an IP (Occupier) that is uploading a licence to occupy" which {
      commonEvidencePageTests(language = English,
        evidenceType = "LICENSE",
        userIsAgent = false,
        relationship = "Occupier",
        backLink = backToEvidenceQuestionHref,
        fromCya = false)
    }

    "display the correct content in English for an IP (Occupier) that is uploading a service charge statement" which {
      commonEvidencePageTests(language = English,
        evidenceType = "SERVICE_CHARGE",
        userIsAgent = false,
        relationship = "Occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in English for an IP (Owner) that is uploading a stamp duty land tax form" which {
      commonEvidencePageTests(language = English,
        evidenceType = "STAMP_DUTY",
        userIsAgent = false,
        relationship = "Owner",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in English for an IP (Owner and occupier) that is uploading a land registry title" which {
      commonEvidencePageTests(language = English,
        evidenceType = "LAND_REGISTRY",
        userIsAgent = false,
        relationship = "Owner and occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in English for an IP (Occupier) that is uploading a water rate demand" which {
      commonEvidencePageTests(language = English,
        evidenceType = "WATER_RATE",
        userIsAgent = false,
        relationship = "Occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in English for an IP (Owner) that is uploading a utility bill" which {
      commonEvidencePageTests(language = English,
        evidenceType = "UTILITY_RATE",
        userIsAgent = false,
        relationship = "Owner",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in English for an IP (Occupier) that is uploading other evidence (Occupier)" which {
      otherEvidencePageTests(language = English, relationship = "Occupier", userIsAgent = false, fromCya = false)
    }

    "display the correct content in English for an IP (Owner) that is uploading other evidence (Owner)" which {
      otherEvidencePageTests(language = English, relationship = "Owner", userIsAgent = false, fromCya = false)
    }

    "display the correct content in English for an IP (Owner and occupier) that is uploading other evidence (Owner and occupier)" which {
      otherEvidencePageTests(language = English, relationship = "Owner and occupier", userIsAgent = false, fromCya = false)
    }

    "display the correct content in English for an IP (Occupier) that has uploaded a Lease (from CYA)" which {
      commonEvidencePageTests(language = English,
        evidenceType = "LEASE",
        userIsAgent = false,
        relationship = "Occupier",
        backLink = backToEvidenceQuestionHref,
        fromCya = true)
    }

    "display the correct content in English for an IP (Owner) that has uploaded a business rates bill (from CYA)" which {
      commonEvidencePageTests(language = English,
        evidenceType = "RATES_BILL",
        userIsAgent = false,
        relationship = "Owner",
        backLink = backToEvidenceQuestionHref,
        fromCya = true)
    }

    "display the correct content in English for an IP (Owner) that uploaded a lease and accesses the 'Other evidence page' (from CYA)" which {
      otherEvidencePageTests(language = English, relationship = "Owner", userIsAgent = false, fromCya = true)
    }

    // Agent - English
    "display the correct content in English for an Agent (Owner) that is uploading a business rates bill" which {
      commonEvidencePageTests(language = English,
        evidenceType = "RATES_BILL",
        userIsAgent = true,
        relationship = "Owner",
        backLink = backToEvidenceQuestionHref,
        fromCya = false)
    }

    "display the correct content in English for an Agent (Occupier) that is uploading a lease" which {
      commonEvidencePageTests(language = English,
        evidenceType = "LEASE",
        userIsAgent = true,
        relationship = "Occupier",
        backLink = backToEvidenceQuestionHref,
        fromCya = false)
    }

    "display the correct content in English for an Agent (Occupier) that is uploading a licence to occupy" which {
      commonEvidencePageTests(language = English,
        evidenceType = "LICENSE",
        userIsAgent = true,
        relationship = "Occupier",
        backLink = backToEvidenceQuestionHref,
        fromCya = false)
    }

    "display the correct content in English for an Agent (Occupier) that is uploading a service charge statement" which {
      commonEvidencePageTests(language = English,
        evidenceType = "SERVICE_CHARGE",
        userIsAgent = true,
        relationship = "Occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in English for an Agent (Owner) that is uploading a stamp duty land tax form" which {
      commonEvidencePageTests(language = English,
        evidenceType = "STAMP_DUTY",
        userIsAgent = true,
        relationship = "Owner",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in English for an Agent (Owner and occupier) that is uploading a land registry title" which {
      commonEvidencePageTests(language = English,
        evidenceType = "LAND_REGISTRY",
        userIsAgent = true,
        relationship = "Owner and occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in English for an Agent (Occupier) that is uploading a water rate demand" which {
      commonEvidencePageTests(language = English,
        evidenceType = "WATER_RATE",
        userIsAgent = true,
        relationship = "Occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in English for an Agent (Owner) that is uploading a utility bill" which {
      commonEvidencePageTests(language = English,
        evidenceType = "UTILITY_RATE",
        userIsAgent = true,
        relationship = "Owner",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in English for an Agent (Occupier) that is uploading other evidence (Occupier)" which {
      otherEvidencePageTests(language = English, relationship = "Occupier", userIsAgent = true, fromCya = false)
    }

    "display the correct content in English for an Agent (Owner) that is uploading other evidence (Owner)" which {
      otherEvidencePageTests(language = English, relationship = "Owner", userIsAgent = true, fromCya = false)
    }

    "display the correct content in English for an Agent (Owner and occupier) that is uploading other evidence (Owner and occupier)" which {
      otherEvidencePageTests(language = English, relationship = "Owner and occupier", userIsAgent = true, fromCya = false)
    }

    "display the correct content in English for an Agent (Occupier) that has uploaded a licence to occupy (from CYA)" which {
      commonEvidencePageTests(language = English,
        evidenceType = "LICENSE",
        userIsAgent = true,
        relationship = "Occupier",
        backLink = backToEvidenceQuestionHref,
        fromCya = true)
    }

    "display the correct content in English for an Agent (Owner) that has uploaded a water rate demand (from CYA)" which {
      commonEvidencePageTests(language = English,
        evidenceType = "WATER_RATE",
        userIsAgent = true,
        relationship = "Owner",
        backLink = backToOtherEvidenceHref,
        fromCya = true)
    }

    "display the correct content in English for an Agent (Owner) that uploaded a lease and accesses the 'Other evidence page' (from CYA)" which {
      otherEvidencePageTests(language = English, relationship = "Owner", userIsAgent = true, fromCya = true)
    }

    // IP - Welsh
    "display the correct content in Welsh for an IP (Owner) that is uploading a business rates bill" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "RATES_BILL",
        userIsAgent = false,
        relationship = "Owner",
        backLink = backToEvidenceQuestionHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an IP (Occupier) that is uploading a lease" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "LEASE",
        userIsAgent = false,
        relationship = "Occupier",
        backLink = backToEvidenceQuestionHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an IP (Occupier) that is uploading a licence to occupy" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "LICENSE",
        userIsAgent = false,
        relationship = "Occupier",
        backLink = backToEvidenceQuestionHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an IP (Occupier) that is uploading a service charge statement" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "SERVICE_CHARGE",
        userIsAgent = false,
        relationship = "Occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an IP (Owner) that is uploading a stamp duty land tax form" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "STAMP_DUTY",
        userIsAgent = false,
        relationship = "Owner",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an IP (Owner and occupier) that is uploading a land registry title" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "LAND_REGISTRY",
        userIsAgent = false,
        relationship = "Owner and occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an IP (Occupier) that is uploading a water rate demand" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "WATER_RATE",
        userIsAgent = false,
        relationship = "Occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an IP (Owner) that is uploading a utility bill" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "UTILITY_RATE",
        userIsAgent = false,
        relationship = "Owner",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an IP (Occupier) that is uploading other evidence (Occupier)" which {
      otherEvidencePageTests(language = Welsh, relationship = "Occupier", userIsAgent = false, fromCya = false)
    }

    "display the correct content in Welsh for an IP (Owner) that is uploading other evidence (Owner)" which {
      otherEvidencePageTests(language = Welsh, relationship = "Owner", userIsAgent = false, fromCya = false)
    }

    "display the correct content in Welsh for an IP (Owner and occupier) that is uploading other evidence (Owner and occupier)" which {
      otherEvidencePageTests(language = Welsh, relationship = "Owner and occupier", userIsAgent = false, fromCya = false)
    }

    "display the correct content in Welsh for an IP (Occupier) that has uploaded a Lease (from CYA)" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "LEASE",
        userIsAgent = false,
        relationship = "Occupier",
        backLink = backToEvidenceQuestionHref,
        fromCya = true)
    }

    "display the correct content in Welsh for an IP (Owner) that has uploaded a business rates bill (from CYA)" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "RATES_BILL",
        userIsAgent = false,
        relationship = "Owner",
        backLink = backToEvidenceQuestionHref,
        fromCya = true)
    }

    "display the correct content in Welsh for an IP (Owner and occupier) that uploaded a lease and accesses the 'Other evidence page' (from CYA)" which {
      otherEvidencePageTests(language = Welsh, relationship = "Owner and occupier", userIsAgent = false, fromCya = true)
    }

    // Agent - Welsh
    "display the correct content in Welsh for an Agent (Owner) that is uploading a business rates bill" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "RATES_BILL",
        userIsAgent = true,
        relationship = "Owner",
        backLink = backToEvidenceQuestionHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an Agent (Occupier) that is uploading a lease" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "LEASE",
        userIsAgent = true,
        relationship = "Occupier",
        backLink = backToEvidenceQuestionHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an Agent (Occupier) that is uploading a licence to occupy" which {
        commonEvidencePageTests(language = Welsh,
          evidenceType = "LICENSE",
          userIsAgent = true,
          relationship = "Occupier",
          backLink = backToEvidenceQuestionHref,
          fromCya = false)
    }

    "display the correct content in Welsh for an Agent (Occupier) that is uploading a service charge statement" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "SERVICE_CHARGE",
        userIsAgent = true,
        relationship = "Occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an Agent (Owner) that is uploading a stamp duty land tax form" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "STAMP_DUTY",
        userIsAgent = true,
        relationship = "Owner",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an Agent (Owner and occupier) that is uploading a land registry title" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "LAND_REGISTRY",
        userIsAgent = true,
        relationship = "Owner and occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an Agent (Occupier) that is uploading a water rate demand" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "WATER_RATE",
        userIsAgent = true,
        relationship = "Occupier",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an Agent (Owner) that is uploading a utility bill" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "UTILITY_RATE",
        userIsAgent = true,
        relationship = "Owner",
        backLink = backToOtherEvidenceHref,
        fromCya = false)
    }

    "display the correct content in Welsh for an Agent (Occupier) that is uploading other evidence (Occupier)" which {
      otherEvidencePageTests(language = Welsh, relationship = "Occupier", userIsAgent = true, fromCya = false)
    }

    "display the correct content in Welsh for an Agent (Owner) that is uploading other evidence (Owner)" which {
      otherEvidencePageTests(language = Welsh, relationship = "Owner", userIsAgent = true, fromCya = false)
    }

    "display the correct content in Welsh for an Agent (Owner and occupier) that is uploading other evidence (Owner and occupier)" which {
      otherEvidencePageTests(language = Welsh, relationship = "Owner and occupier", userIsAgent = true, fromCya = false)
    }

    "display the correct content in Welsh for an Agent (Occupier) that has uploaded a licence to occupy (from CYA)" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "LICENSE",
        userIsAgent = true,
        relationship = "Occupier",
        backLink = backToEvidenceQuestionHref,
        fromCya = true)
    }

    "display the correct content in Welsh for an Agent (Owner) that has uploaded a water rate demand (from CYA)" which {
      commonEvidencePageTests(language = Welsh,
        evidenceType = "WATER_RATE",
        userIsAgent = true,
        relationship = "Owner",
        backLink = backToOtherEvidenceHref,
        fromCya = true)
    }

    "display the correct content in Welsh for an Agent (Owner and occupier) that uploaded a lease and accesses the 'Other evidence page' (from CYA)" which {
      otherEvidencePageTests(language = Welsh, relationship = "Owner and occupier", userIsAgent = true, fromCya = true)
    }
  }

  //TODO: update these tests as this method is only used for other evidence types
  "UploadController continue method" should {

    "display an error message in English when an IP (Owner) attempts to continue without selecting a radio on the 'Other evidence' page" which {
      otherEvidencePageTests(language = English, relationship = "Owner", userIsAgent = false, fromCya = false, errorPage = true)
    }

    "display the 'Cannot provide evidence' page in English (Owner) when an IP selects 'I cannot provide evidence'" which {
      unableToProvideEvidencePageTests(language = English, userIsAgent = false)
    }

    "display an error message in English when an Agent (Owner) attempts to continue without selecting a radio on the 'Other evidence' page" which {
      otherEvidencePageTests(language = English, relationship = "Owner", userIsAgent = true, fromCya = false, errorPage = true)
    }

    "display the 'Cannot provide evidence' page in English when an Agent (Owner) selects 'I cannot provide evidence'" which {
      unableToProvideEvidencePageTests(language = English, userIsAgent = true)
    }

    "display an error message in Welsh when an IP (Owner) attempts to continue without selecting a radio on the 'Other evidence' page" which {
      otherEvidencePageTests(language = Welsh, relationship = "Owner", userIsAgent = false, fromCya = false, errorPage = true)
    }

    "display the 'Cannot provide evidence' page in Welsh when an IP (Owner) selects 'I cannot provide evidence'" which {
      unableToProvideEvidencePageTests(language = Welsh, userIsAgent = false)
    }

    "display an error message in Welsh when an Agent (Owner) attempts to continue without selecting a radio on the 'Other evidence' page" which {
      otherEvidencePageTests(language = Welsh, relationship = "Owner", userIsAgent = true, fromCya = false, errorPage = true)
    }

    "display the 'Cannot provide evidence' page in Welsh when an Agent (Owner) selects 'I cannot provide evidence'" which {
      unableToProvideEvidencePageTests(language = Welsh, userIsAgent = true)
    }

    // Success - Other evidence page
    "return a 303 and redirects the user (Owner) to the 'Upload evidence' page when selecting 'Lease' and clicking 'Continue' on the 'Other evidence' page" which {
      lazy val res = postUploadEvidencePage(language = English, "OTHER", userIsAgent = false, errorPage = false, selectedEvidenceType = Some("Lease"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(evidenceTypeRedirectUrl("LEASE"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload evidence' page when selecting 'Licence to occupy' and clicking 'Continue' on the 'Other evidence' page" which {
      lazy val res = postUploadEvidencePage(language = English, "OTHER", userIsAgent = false, errorPage = false, selectedEvidenceType = Some("License"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(evidenceTypeRedirectUrl("LICENSE"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload evidence' page when selecting 'Stamp duty' and clicking 'Continue' on the 'Other evidence' page" which {
      lazy val res = postUploadEvidencePage(language = English, "OTHER", userIsAgent = false, errorPage = false, selectedEvidenceType = Some("StampDutyLandTaxForm"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(evidenceTypeRedirectUrl("STAMP_DUTY"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload evidence' page when selecting 'Land Registry Title' and clicking 'Continue' on the 'Other evidence' page" which {
      lazy val res = postUploadEvidencePage(language = English, "OTHER", userIsAgent = false, errorPage = false, selectedEvidenceType = Some("LandRegistryTitle"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(evidenceTypeRedirectUrl("LAND_REGISTRY"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload evidence' page when selecting 'Water rate demand' and clicking 'Continue' on the 'Other evidence' page" which {
      lazy val res = postUploadEvidencePage(language = English, "OTHER", userIsAgent = false, errorPage = false, selectedEvidenceType = Some("WaterRateDemand"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(evidenceTypeRedirectUrl("WATER_RATE"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload evidence' page when selecting 'Utility bill' and clicking 'Continue' on the 'Other evidence' page" which {
      lazy val res = postUploadEvidencePage(language = English, "OTHER", userIsAgent = false, errorPage = false, selectedEvidenceType = Some("OtherUtilityBill"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(evidenceTypeRedirectUrl("UTILITY_RATE"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload evidence' page when selecting 'Service charge' and clicking 'Continue' on the 'Other evidence' page" which {
      lazy val res = postUploadEvidencePage(language = English, "OTHER", userIsAgent = false, errorPage = false, selectedEvidenceType = Some("ServiceCharge"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(evidenceTypeRedirectUrl("SERVICE_CHARGE"))
      }
    }

    // Success - Individual evidence page

    "return a 303 and redirects the user (Owner) to the 'Upload Results' page when uploading a 'Lease' and clicking 'Continue'" which {
      lazy val res = postUploadEvidencePage(language = English, "LEASE", userIsAgent = true, errorPage = false, selectedEvidenceType = Some("Lease"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(UploadResultsRedirectUrl("LEASE"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload Results' page when uploading a 'Licence to occupy' and clicking 'Continue'" which {
      lazy val res = postUploadEvidencePage(language = English, "LICENSE", userIsAgent = false, errorPage = false, selectedEvidenceType = Some("License"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(UploadResultsRedirectUrl("LICENSE"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload Results' page when uploading a 'Service charge statement' and clicking 'Continue'" which {
      lazy val res = postUploadEvidencePage(language = English, "SERVICE_CHARGE", userIsAgent = true, errorPage = false, selectedEvidenceType = Some("ServiceCharge"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(UploadResultsRedirectUrl("SERVICE_CHARGE"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload Results' page when uploading a 'Stamp Duty Land Tax form' and clicking 'Continue'" which {
      lazy val res = postUploadEvidencePage(language = English, "STAMP_DUTY", userIsAgent = false, errorPage = false, selectedEvidenceType = Some("StampDutyLandTaxForm"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(UploadResultsRedirectUrl("STAMP_DUTY"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload Results' page when uploading a 'Land Registry title' and clicking 'Continue'" which {
      lazy val res = postUploadEvidencePage(language = English, "LAND_REGISTRY", userIsAgent = true, errorPage = false, selectedEvidenceType = Some("LandRegistryTitle"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(UploadResultsRedirectUrl("LAND_REGISTRY"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload Results' page when uploading a 'Water rate demand' and clicking 'Continue'" which {
      lazy val res = postUploadEvidencePage(language = English, "WATER_RATE", userIsAgent = false, errorPage = false, selectedEvidenceType = Some("WaterRateDemand"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(UploadResultsRedirectUrl("WATER_RATE"))
      }
    }

    "return a 303 and redirects the user (Owner) to the 'Upload Results' page when uploading a 'Utility bill' and clicking 'Continue'" which {
      lazy val res = postUploadEvidencePage(language = English, "UTILITY_RATE", userIsAgent = true, errorPage = false, selectedEvidenceType = Some("OtherUtilityBill"))

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("location") shouldBe Some(UploadResultsRedirectUrl("UTILITY_RATE"))
      }
    }
  }

  private def commonSetup(evidenceType: String, relationship: String, userIsAgent: Boolean, fromCya: Boolean, fileUploaded: Boolean = false): Unit = {
    val authAccounts = if (userIsAgent) testAccounts else testIpAccounts
    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(authAccounts).toString())
        }
    }

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

    stubFor {
      post("/business-rates-attachments/initiate-upload")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(PreparedUpload(Reference("12345678910111213"), UploadFormTemplate("http://localhost/upscan", Map()))
          ).toString())
        }
    }

    val (hasRatesBill, occupierEvidenceType): (Option[Boolean], Option[EvidenceType]) = evidenceType match {
      case "Business rates bill" => (Some(true), None)
      case "No business rates bill" => (Some(false), None)
      case "Lease" => (None, Some(Lease))
      case "Licence to occupy" => (None, Some(License))
      case "No lease or licence to occupy" => (None, Some(NoLeaseOrLicense))
      case _ => (None, None)
    }

    val propertyRelationship: PropertyRelationship = relationship.toUpperCase match {
      case "OWNER" => PropertyRelationship(capacity = Owner, uarn = 1)
      case "OCCUPIER" => PropertyRelationship(capacity = Occupier, uarn = 1)
      case "OWNER AND OCCUPIER" => PropertyRelationship(capacity = OwnerOccupier, uarn = 1)
    }

    // File information
    val evidenceFlag = if (evidenceType == "Business rates bill") RatesBillFlag else OtherEvidenceFlag
    val FILE_REFERENCE: String = "123456789"
    val preparedUpload = PreparedUpload(Reference(FILE_REFERENCE), UploadFormTemplate("http://localhost/upscan", Map()))
    val fileMetadata = FileMetadata("PL1ZDREF-test_file.pdf", "application/pdf")
    val uploadedFileDetails = UploadedFileDetails(fileMetadata, preparedUpload)

    val uploadEvidenceData = if (fromCya || fileUploaded)
      UploadEvidenceData(
        linkBasis = evidenceFlag,
        fileInfo = Some(CompleteFileInfo("test_file.pdf", Lease)),
        attachments = Some(Map(FILE_REFERENCE -> uploadedFileDetails)))
    else UploadEvidenceData.empty


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
          uploadEvidenceData = uploadEvidenceData,
          clientDetails = if (userIsAgent) Some(ClientDetails(123, "Client Name")) else None,
          localAuthorityReference = "2050466366770",
          rtp = ClaimPropertyReturnToPage.FMBR,
          fromCya = Some(fromCya),
          isSubmitted = None
        )))
  }

  private def getUploadEvidencePage(language: Language, evidenceType: String, relationship: String, userIsAgent: Boolean, fromCya: Boolean) = {
    commonSetup(evidenceType, relationship, userIsAgent, fromCya)

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/evidence/$evidenceType/upload")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def postUploadEvidencePage(language: Language, evidenceType: String, userIsAgent: Boolean, errorPage: Boolean, selectedEvidenceType: Option[String] = None, relationship: String = "Owner") = {
    commonSetup(evidenceType, relationship = relationship, userIsAgent, fromCya = false, fileUploaded = !errorPage)

    val body: JsObject = if (errorPage) Json.obj("evidenceType" -> "") else Json.obj("evidenceType" -> JsString(selectedEvidenceType.getOrElse("")))

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/evidence/upload")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .withFollowRedirects(follow = false)
        .post(body)
    )
  }

  private def otherEvidencePageTests(language: Language, relationship: String, userIsAgent: Boolean, fromCya: Boolean, errorPage: Boolean = false): Unit = {

    val evidenceType = if (relationship == "Occupier") "NO_LEASE_OR_LICENSE" else "OTHER"

    lazy val document = if (errorPage) {
      lazy val res = postUploadEvidencePage(language, evidenceType, userIsAgent, errorPage)
      res.status shouldBe BAD_REQUEST
      Jsoup.parse(res.body)
    } else {
      getUploadEvidencePage(language, evidenceType, relationship, userIsAgent, fromCya)
    }

    val error = if (language == English) errorText else errorTextWelsh
    val title = {
      val title = if (language == English) otherEvidenceTitleText else otherEvidenceTitleTextWelsh
      if (errorPage) s"$error $title" else title
    }
    val heading = if (language == English) otherEvidenceHeadingText else otherEvidenceHeadingTextWelsh
    val back = if (language == English) backLinkText else backLinkTextWelsh
    val hint = if (language == English) otherEvidenceHintText else otherEvidenceHintTextWelsh
    val continue = if (language == English) continueButtonText else continueButtonTextWelsh
    val busRates = if (language == English) businessRatesBillText else businessRatesBillTextWelsh
    val lease = if (language == English) leaseText else leaseTextWelsh
    val licence = if (language == English) licenceToOccupyText else licenceToOccupyTextWelsh
    val service = if (language == English) serviceChargeStatementText else serviceChargeStatementTextWelsh
    val stamp = if (language == English) stampDutyLandTaxText else stampDutyLandTaxTextWelsh
    val land = if (language == English) landRegistryText else landRegistryTextWelsh
    val water = if (language == English) waterRateText else waterRateTextWelsh
    val utility = if (language == English) utilityBillText else utilityBillTextWelsh
    val cantProvide = if (language == English) cannotProvideEvidenceText else cannotProvideEvidenceTextWelsh
    val or = if (language == English) orText else orTextWelsh
    val errorSummary = if (language == English) errorSummaryText else errorSummaryTextWelsh
    val errorMessage = if (language == English) otherEvidenceErrorMessageText else otherEvidenceErrorMessageTextWelsh

    s"has a title of $title" in {
      document.title() shouldBe title
    }

    s"has a heading of $heading" in {
      document.getElementsByTag(headingLocator).text shouldBe heading
    }

    s"has a back link that goes back to the evidence question page" in {
      document.select(backLinkLocator).text shouldBe back
      document.select(backLinkLocator).attr("href") shouldBe backToEvidenceQuestionHref
    }

    s"has hint text of $hint" in {
      document.select(otherEvidenceHintLocator).text shouldBe hint
    }

    s"has a $continue button" in {
      document.select(continueButtonLocator).text shouldBe continue
    }

    if (relationship == "Occupier") {
      s"has an unselected $busRates radio button" in {
        document.select(otherEvidenceRadioLabelLocator(1)).text shouldBe busRates
        document.select(otherEvidenceRadioLocator(1)).hasAttr("checked") shouldBe false
      }

      s"has an unselected $service radio button" in {
        document.select(otherEvidenceRadioLabelLocator(2)).text shouldBe service
        document.select(otherEvidenceRadioLocator(2)).hasAttr("checked") shouldBe false
      }

      s"has an unselected $stamp radio button" in {
        document.select(otherEvidenceRadioLabelLocator(3)).text shouldBe stamp
        document.select(otherEvidenceRadioLocator(3)).hasAttr("checked") shouldBe false
      }

      s"has an unselected $land radio button" in {
        document.select(otherEvidenceRadioLabelLocator(4)).text shouldBe land
        document.select(otherEvidenceRadioLocator(4)).hasAttr("checked") shouldBe false
      }

      s"has an unselected $water radio button" in {
        document.select(otherEvidenceRadioLabelLocator(5)).text shouldBe water
        document.select(otherEvidenceRadioLocator(5)).hasAttr("checked") shouldBe false
      }

      s"has an unselected $utility radio button" in {
        document.select(otherEvidenceRadioLabelLocator(6)).text shouldBe utility
        document.select(otherEvidenceRadioLocator(6)).hasAttr("checked") shouldBe false
      }

      // A radio hasn't been missed - This is child 7 of otherEvidenceRadioLocator
      "has text of 'or'" in {
        document.select(orLocator).text shouldBe or
      }

      s"has an unselected $cantProvide radio button" in {
        document.select(otherEvidenceRadioLabelLocator(8)).text shouldBe cantProvide
        document.select(otherEvidenceRadioLocator(8)).hasAttr("checked") shouldBe false
      }
    } else {
      s"has an unselected $lease radio button" in {
        document.select(otherEvidenceRadioLabelLocator(1)).text shouldBe lease
        document.select(otherEvidenceRadioLocator(1)).hasAttr("checked") shouldBe fromCya
      }

      s"has an unselected $licence radio button" in {
        document.select(otherEvidenceRadioLabelLocator(2)).text shouldBe licence
        document.select(otherEvidenceRadioLocator(2)).hasAttr("checked") shouldBe false
      }

      s"has an unselected $service radio button" in {
        document.select(otherEvidenceRadioLabelLocator(3)).text shouldBe service
        document.select(otherEvidenceRadioLocator(3)).hasAttr("checked") shouldBe false
      }

      s"has an unselected $stamp radio button" in {
        document.select(otherEvidenceRadioLabelLocator(4)).text shouldBe stamp
        document.select(otherEvidenceRadioLocator(4)).hasAttr("checked") shouldBe false
      }

      s"has an unselected $land radio button" in {
        document.select(otherEvidenceRadioLabelLocator(5)).text shouldBe land
        document.select(otherEvidenceRadioLocator(5)).hasAttr("checked") shouldBe false
      }

      s"has an unselected $water radio button" in {
        document.select(otherEvidenceRadioLabelLocator(6)).text shouldBe water
        document.select(otherEvidenceRadioLocator(6)).hasAttr("checked") shouldBe false
      }

      s"has an unselected $utility radio button" in {
        document.select(otherEvidenceRadioLabelLocator(7)).text shouldBe utility
        document.select(otherEvidenceRadioLocator(7)).hasAttr("checked") shouldBe false
      }

      // A radio hasn't been missed - This is child 8 of otherEvidenceRadioLocator
      "has text of 'or'" in {
        document.select(orLocator).text shouldBe or
      }

      s"has an unselected $cantProvide radio button" in {
        document.select(otherEvidenceRadioLabelLocator(9)).text shouldBe cantProvide
        document.select(otherEvidenceRadioLocator(9)).hasAttr("checked") shouldBe false
      }
    }

    if (errorPage) {
      s"has an error summary with a message of $errorMessage" in {
        document.select(otherEvidenceSummaryLocator).text shouldBe errorSummary
        document.select(otherEvidenceSummaryMessageLocator).text shouldBe errorMessage
        document.select(otherEvidenceSummaryMessageLocator).attr("href") shouldBe otherEvidenceErrorMessageHref
      }

      s"has an error message of $errorMessage above the radio buttons" in {
        document.select(otherEvidenceErrorMessageLocator).text shouldBe s"$error $errorMessage"
      }
    }

  }

  private def unableToProvideEvidencePageTests(language: Language, userIsAgent: Boolean): Unit = {
    def getUnableToProvideEvidencePage() = {
      commonSetup(evidenceType = "OTHER", relationship = "Owner", userIsAgent, fromCya = false)

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/evidence/cannot-provide-evidence")
          .withCookies(languageCookie(language), getSessionCookie(testSessionId))
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .withFollowRedirects(follow = false)
          .get()
      )

      res.status shouldBe OK
      Jsoup.parse(res.body)
    }

    def getParagraph1and2(): (String, String) = {
      val paragraph1 = (userIsAgent, language) match {
        case (true, English)  => cannotProvideEvidenceP1Agent
        case (true, Welsh)    => cannotProvideEvidenceP1AgentWelsh
        case (false, English) => cannotProvideEvidenceP1
        case (false, Welsh)   => cannotProvideEvidenceP1Welsh
      }

      val paragraph2 = (userIsAgent, language) match {
        case (true, English)  => cannotProvideEvidenceP2Agent
        case (true, Welsh)    => cannotProvideEvidenceP2AgentWelsh
        case (false, English) => cannotProvideEvidenceP2
        case (false, Welsh)   => cannotProvideEvidenceP2Welsh
      }

      (paragraph1, paragraph2)
    }

    lazy val document = getUnableToProvideEvidencePage()
    val title = if (language == English) cannotProvideEvidenceTitleText else cannotProvideEvidenceTitleTextWelsh
    val heading = if (language == English) cannotProvideEvidenceHeadingText else cannotProvideEvidenceHeadingTextWelsh
    val caption = if (language == English) captionText else captionTextWelsh
    val back = if (language == English) backLinkText else backLinkTextWelsh
    val (p1, p2) = getParagraph1and2()
    val p3 = if (language == English) cannotProvideEvidenceP3 else cannotProvideEvidenceP3Welsh

    s"has a title of $title" in {
      document.title shouldBe title
    }

    s"has a heading of $heading" in {
      document.getElementsByTag(headingLocator).text shouldBe heading
    }

    s"has a caption of $caption" in {
      document.select(captionLocator).text shouldBe caption
    }

    s"has a back link that goes back to the evidence question page" in {
      document.select(backLinkLocator).text shouldBe back
      document.select(backLinkLocator).attr("href") shouldBe backToOtherEvidenceHref
    }

    s"has a first paragraph of $p1" in {
      document.select(cannotProvideEvidenceP1Locator).text shouldBe p1
    }

    s"has a second paragraph of $p2" in {
      document.select(cannotProvideEvidenceP2Locator).text shouldBe p2
    }

    s"has a third paragraph of $p3" in {
      document.select(cannotProvideEvidenceP3Locator).text shouldBe p3
      document.select(cannotProvideEvidenceEmailLocator).text shouldBe cannotProvideEvidenceEmailText
      document.select(cannotProvideEvidenceEmailLocator).attr("href") shouldBe emailHref
    }
  }

  private def commonEvidencePageTests(language: Language, evidenceType: String, userIsAgent: Boolean, relationship: String, backLink: String, fromCya: Boolean, errorPage: Boolean = false): Unit = {

    def getTitleAndHeading(): (String, String) = {
      val heading = (evidenceType, userIsAgent) match {
        case ("RATES_BILL", false) => if (language == English) ratesBillHeadingText else ratesBillHeadingTextWelsh
        case ("RATES_BILL", true) => if (language == English) ratesBillHeadingTextAgent else ratesBillHeadingTextAgentWelsh
        case ("LEASE", false) => if (language == English) leaseHeadingText else leaseHeadingTextWelsh
        case ("LEASE", true) => if (language == English) leaseHeadingTextAgent else leaseHeadingTextAgentWelsh
        case ("LICENSE", false) => if (language == English) licenceHeadingText else licenceHeadingTextWelsh
        case ("LICENSE", true) => if (language == English) licenceHeadingTextAgent else licenceHeadingTextAgentWelsh
        case ("SERVICE_CHARGE", false) => if (language == English) serviceChargeHeadingText else serviceChargeHeadingTextWelsh
        case ("SERVICE_CHARGE", true) => if (language == English) serviceChargeHeadingTextAgent else serviceChargeHeadingTextAgentWelsh
        case ("STAMP_DUTY", false) => if (language == English) stampDutyHeadingText else stampDutyHeadingTextWelsh
        case ("STAMP_DUTY", true) => if (language == English) stampDutyHeadingTextAgent else stampDutyHeadingTextAgentWelsh
        case ("LAND_REGISTRY", false) => if (language == English) landRegistryHeadingText else landRegistryHeadingTextWelsh
        case ("LAND_REGISTRY", true) => if (language == English) landRegistryHeadingTextAgent else landRegistryHeadingTextAgentWelsh
        case ("WATER_RATE", false) => if (language == English) waterRateHeadingText else waterRateHeadingTextWelsh
        case ("WATER_RATE", true) => if (language == English) waterRateHeadingTextAgent else waterRateHeadingTextAgentWelsh
        case ("UTILITY_RATE", false) => if (language == English) utilityBillHeadingText else utilityBillHeadingTextWelsh
        case ("UTILITY_RATE", true) => if (language == English) utilityBillHeadingTextAgent else utilityBillHeadingTextAgentWelsh
      }

      val title = {
        val errorPrefix = if (language == English) errorText else errorTextWelsh
        val titleSuffix = "- Valuation Office Agency - GOV.UK"
        if (errorPage) s"$errorPrefix $heading $titleSuffix" else s"$heading $titleSuffix"
      }

      (title, heading)
    }

    val caption = if (language == English) captionText else captionTextWelsh
    val back = if (language == English) backLinkText else backLinkTextWelsh
    val hint = if (language == English) hintText else hintTextWelsh
    val continue = if (language == English) continueButtonText else continueButtonTextWelsh
    val error = if (language == English) errorText else errorTextWelsh
    val errorMessage = if (language == English) errorMessageText else errorMessageTextWelsh
    val errorSummary = if (language == English) errorSummaryText else errorSummaryTextWelsh
    val (title, heading) = getTitleAndHeading()

    lazy val document = if (errorPage) {
      lazy val res = postUploadEvidencePage(language, evidenceType, userIsAgent, errorPage)
      res.status shouldBe BAD_REQUEST
      Jsoup.parse(res.body)
    } else {
      getUploadEvidencePage(language, evidenceType, relationship, userIsAgent, fromCya)
    }

    s"has a title of $title" in {
      document.title() shouldBe title
    }

    s"has a heading of $heading" in {
      document.getElementsByTag(headingLocator).text shouldBe heading
    }

    s"has a caption of $caption" in {
      document.select(captionLocator).text shouldBe caption
    }

    s"has a back link with the correct href" in {
      document.select(backLinkLocator).text shouldBe back
      document.select(backLinkLocator).attr("href") shouldBe backLink
    }

    s"has hint text of $hint" in {
      document.getElementById(hintTextLocator).text shouldBe hint
    }

    s"has a $continue button" in {
      document.select(continueButtonLocator).text shouldBe continue
    }

    "has a file upload component" in {
      document.getElementById(fileUploadComponentLocator).hasClass("govuk-file-upload") shouldBe true
    }

    if (errorPage) {
      s"has an error summary with a message of $errorMessage" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummary
        document.select(errorSummaryMessageLocator).text shouldBe errorMessage
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorMessageHref
      }

      s"has an error message of $errorMessage above the radio buttons" in {
        //File upload component doesn't appear to translate to welsh
        document.select(errorMessageLocator).text shouldBe s"Error: $errorMessage"
      }
    }
  }

  private def commonOtherEvidencePageTests(language: Language, evidenceType: String, userIsAgent: Boolean, relationship: String, backLink: String, fromCya: Boolean, errorPage: Boolean = false): Unit = {

    def getTitleAndHeading(): (String, String) = {
      val heading = (evidenceType, userIsAgent) match {
        case ("RATES_BILL", false) => if (language == English) ratesBillHeadingText else ratesBillHeadingTextWelsh
        case ("RATES_BILL", true) => if (language == English) ratesBillHeadingTextAgent else ratesBillHeadingTextAgentWelsh
        case ("LEASE", false) => if (language == English) leaseHeadingText else leaseHeadingTextWelsh
        case ("LEASE", true) => if (language == English) leaseHeadingTextAgent else leaseHeadingTextAgentWelsh
        case ("LICENSE", false) => if (language == English) licenceHeadingText else licenceHeadingTextWelsh
        case ("LICENSE", true) => if (language == English) licenceHeadingTextAgent else licenceHeadingTextAgentWelsh
        case ("SERVICE_CHARGE", false) => if (language == English) serviceChargeHeadingText else serviceChargeHeadingTextWelsh
        case ("SERVICE_CHARGE", true) => if (language == English) serviceChargeHeadingTextAgent else serviceChargeHeadingTextAgentWelsh
        case ("STAMP_DUTY", false) => if (language == English) stampDutyHeadingText else stampDutyHeadingTextWelsh
        case ("STAMP_DUTY", true) => if (language == English) stampDutyHeadingTextAgent else stampDutyHeadingTextAgentWelsh
        case ("LAND_REGISTRY", false) => if (language == English) landRegistryHeadingText else landRegistryHeadingTextWelsh
        case ("LAND_REGISTRY", true) => if (language == English) landRegistryHeadingTextAgent else landRegistryHeadingTextAgentWelsh
        case ("WATER_RATE", false) => if (language == English) waterRateHeadingText else waterRateHeadingTextWelsh
        case ("WATER_RATE", true) => if (language == English) waterRateHeadingTextAgent else waterRateHeadingTextAgentWelsh
        case ("UTILITY_RATE", false) => if (language == English) utilityBillHeadingText else utilityBillHeadingTextWelsh
        case ("UTILITY_RATE", true) => if (language == English) utilityBillHeadingTextAgent else utilityBillHeadingTextAgentWelsh
      }

      val title = {
        val errorPrefix = if (language == English) errorText else errorTextWelsh
        val titleSuffix = "- Valuation Office Agency - GOV.UK"
        if (errorPage) s"$errorPrefix $heading $titleSuffix" else s"$heading $titleSuffix"
      }

      (title, heading)
    }

    val caption = if (language == English) captionText else captionTextWelsh
    val back = if (language == English) backLinkText else backLinkTextWelsh
    val hint = if (language == English) hintText else hintTextWelsh
    val continue = if (language == English) continueButtonText else continueButtonTextWelsh
    val error = if (language == English) errorText else errorTextWelsh
    val errorMessage = if (language == English) errorMessageText else errorMessageTextWelsh
    val errorSummary = if (language == English) errorSummaryText else errorSummaryTextWelsh
    val (title, heading) = getTitleAndHeading()

    lazy val document = if (errorPage) {
      lazy val res = postUploadEvidencePage(language, evidenceType, userIsAgent, errorPage)
      res.status shouldBe BAD_REQUEST
      Jsoup.parse(res.body)
    } else {
      getUploadEvidencePage(language, evidenceType, relationship, userIsAgent, fromCya)
    }

    s"has a title of $title" in {
      document.title() shouldBe title
    }

    s"has a heading of $heading" in {
      document.getElementsByTag(headingLocator).text shouldBe heading
    }

    s"has a caption of $caption" in {
      document.select(captionLocator).text shouldBe caption
    }

    s"has a back link with the correct href" in {
      document.select(backLinkLocator).text shouldBe back
      document.select(backLinkLocator).attr("href") shouldBe backLink
    }

    s"has hint text of $hint" in {
      document.getElementById(hintTextLocator).text shouldBe hint
    }

    s"has a $continue button" in {
      document.select(continueButtonLocator).text shouldBe continue
    }

    "has a file upload component" in {
      document.getElementById(fileUploadComponentLocator).hasClass("govuk-file-upload") shouldBe true
    }

    if (errorPage) {
      s"has an error summary with a message of $errorMessage" in {
        document.select(errorSummaryTitleLocator).text shouldBe errorSummary
        document.select(errorSummaryMessageLocator).text shouldBe errorMessage
        document.select(errorSummaryMessageLocator).attr("href") shouldBe errorMessageHref
      }

      s"has an error message of $errorMessage above the radio buttons" in {
        //File upload component doesn't appear to translate to welsh
        document.select(errorMessageLocator).text shouldBe s"Error: $errorMessage"
      }
    }
  }

  "UploadController show method" should {
    val ariaLiveId = "ariaLiveRegion"
    val ariaFileRemovedWithNameText = "File image.png removed"
    val ariaFileRemovedWithNameTextWelsh = "File image.png removed"
    val ariaFileRemovedWithoutNameText = "File removed"
    val ariaFileRemovedWithoutNameTextWelsh = "File removed"

    "display the aria correct content in English for when a user has used the remove file link with file name present" in {
      val document = testAriaLiveContent(fileRemoved = true, withFileName = true, English)
      document.getElementsByTag(headingLocator).text shouldBe ratesBillHeadingText
      document.getElementById(ariaLiveId).text() shouldBe ariaFileRemovedWithNameText
    }

    "display the aria correct content in Welsh for when a user has used the remove file link with file name present" in {
      val document = testAriaLiveContent(fileRemoved = true, withFileName = true, Welsh)
      document.getElementsByTag(headingLocator).text shouldBe ratesBillHeadingTextWelsh
      document.getElementById(ariaLiveId).text() shouldBe ariaFileRemovedWithNameTextWelsh
    }

    "display the aria correct content in English for when a user has used the remove file link with no file name present" in {
      val document = testAriaLiveContent(fileRemoved = true, withFileName = false, English)
      document.getElementsByTag(headingLocator).text shouldBe ratesBillHeadingText
      document.getElementById(ariaLiveId).text() shouldBe ariaFileRemovedWithoutNameText
    }

    "display the aria correct content in Welsh for when a user has used the remove file link with no file name present" in {
      val document = testAriaLiveContent(fileRemoved = true, withFileName = false, Welsh)
      document.getElementsByTag(headingLocator).text shouldBe ratesBillHeadingTextWelsh
      document.getElementById(ariaLiveId).text() shouldBe ariaFileRemovedWithoutNameTextWelsh
    }
  }

  def testAriaLiveContent(fileRemoved: Boolean = false, withFileName: Boolean = false, language: Language) = {
    commonSetup(evidenceType = "RATES_BILL", relationship = "Owner", false, fromCya = false)
    val optFileName = if(withFileName) "&removedFileName=image.png" else ""
    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/evidence/RATES_BILL/upload?fileRemoved=$fileRemoved$optFileName")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )
    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

}
