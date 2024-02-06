package controllers.propertyLinking

import base.ISpecBase
import binders.propertylinks.ClaimPropertyReturnToPage
import com.github.tomakehurst.wiremock.client.WireMock._
import models._
import models.upscan.{FileMetadata, PreparedUpload, Reference, UploadFormTemplate, UploadedFileDetails}
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.PropertyLinkingSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.{Instant, LocalDate}
import java.util.UUID

class DeclarationControllerShowISpec extends ISpecBase {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockPropertyLinkingSessionRepository: PropertyLinkingSessionRepository = app.injector.instanceOf[PropertyLinkingSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val addressValue = "Test Address, Test Lane, T35 T3R"
  val startedValue = "2 April 2017"
  val lastDayValue = "3 April 2017"

  val titleText = "Check and confirm your details - Valuation Office Agency - GOV.UK"
  val captionText = "Add a property"
  val headerText = "Check and confirm your details"
  val addressText = "Address"
  val connectionToPropertyText = "Connection to property"
  val startedText = "Started"
  val stillOwnText = "Do you still own the property?"
  val stillOwnAgentText = "Does your client still own the property?"
  val stillOccupyText = "Do you still occupy the property?"
  val stillOccupyAgentText = "Does your client still occupy the property?"
  val stillOwnOccupyText = "Do you still own and occupy the property?"
  val stillOwnOccupyAgentText = "Does your client still own and occupy the property?"
  val changeText = "Change "
  val lastDayOwnerText = "Last day as owner"
  val lastDayOccupierText = "Last day as occupier"
  val lastDayOwnerOccupierText = "Last day as owner and occupier"
  val evidenceText = "Evidence"
  val declarationText = "Declaration"
  val youCouldBeText = "! Warning You could be taken to court if you knowingly submit false information."
  val iDeclareText = "I declare that the information I have given is correct and complete. The evidence I have uploaded contains proof of my connection to the property for dates that overlap with the period I have stated."
  val iDeclareAgentText = "I declare that the information I have given is correct and complete. The evidence I have uploaded contains proof of my client’s connection to the property for dates that overlap with the period I have stated."
  val confirmText = "Confirm and send"
  val ownerText = "Owner"
  val occupierText = "Occupier"
  val ownerOccupierText = "Owner and occupier"
  val yesText = "Yes"
  val noText = "No"
  val leaseEvidenceText = "Lease Test File"
  val licenseEvidenceText = "Licence to occupy Test File"
  val serviceChargeText = "Service charge statement Test File"
  val stampDutyLandTaxFormText = "Stamp Duty Land Tax form Test File"
  val waterRateDemandText = "Water rate demand Test File"
  val otherUtilityBillText = "Utility bill Test File"
  val ratesBillTypeText = "Business rates bill Test File"
  val landRegistryTitleText = "Land Registry title Test File"
  val thereIsAProblemText = "There is a problem"
  val errorText = "Error: "
  val errorMessageText = "You must agree to the declaration to continue"

  val titleTextWelsh = "Gwiriwch a chadarnhau eich manylion - Valuation Office Agency - GOV.UK"
  val captionTextWelsh = "Ychwanegu eiddo"
  val headerTextWelsh = "Gwiriwch a chadarnhau eich manylion"
  val addressTextWelsh = "Cyfeiriad"
  val connectionToPropertyTextWelsh = "Cysylltiad â’r eiddo"
  val startedTextWelsh = "Wedi dechrau"
  val stillOwnTextWelsh = "Ydych chi yn parhau yn berchen ar gyfer yr eiddo?"
  val stillOwnAgentTextWelsh = "Yw eich cleient yn parhau yn berchen ar gyfer yr eiddo?"
  val stillOccupyTextWelsh = "Ydych chi yn parhau yn meddiannu ar gyfer yr eiddo?"
  val stillOccupyAgentTextWelsh = "Yw eich cleient yn parhau yn meddiannu ar gyfer yr eiddo?"
  val stillOwnOccupyTextWelsh = "Ydych chi yn parhau yn berchen ac yn meddiannu ar gyfer yr eiddo?"
  val stillOwnOccupyAgentTextWelsh = "Yw eich cleient yn parhau yn berchen ac yn meddiannu ar gyfer yr eiddo?"
  val changeTextWelsh = "Newid "
  val lastDayOwnerTextWelsh = "Diwrnod olaf fel perchennog"
  val lastDayOccupierTextWelsh = "Diwrnod olaf fel meddiannydd"
  val lastDayOwnerOccupierTextWelsh = "Diwrnod olaf fel perchennog a meddiannydd"
  val evidenceTextWelsh = "Tystiolaeth"
  val declarationTextWelsh = "Datganiad"
  val youCouldBeTextWelsh = "! Rhybudd Gallwch gael eich anfon i’r llys os byddwch yn cyflwyno gwybodaeth ffug yn fwriadol."
  val iDeclareTextWelsh = "Rwy’n datgan bod y wybodaeth a roddais yn gywir ac yn gyflawn. Mae’r dystiolaeth yr wyf wedi’i lanlwytho yn cynnwys prawf o fy nghysylltiad â’r eiddo ar gyfer y ddyddiadau sy’n ymestyn dros y cyfnod yr wyf wedi’i nodi."
  val iDeclareAgentTextWelsh = "Rwy’n datgan bod y wybodaeth a roddais yn gywir ac yn gyflawn. Mae’r dystiolaeth yr wyf wedi’i lanlwytho yn cynnwys prawf o gysylltiad fy nghleient â’r eiddo ar gyfer y ddyddiadau sy’n ymestyn dros y cyfnod yr wyf wedi’i nodi."
  val confirmTextWelsh = "Cadarnhau ac anfon"
  val ownerTextWelsh = "Perchennog"
  val occupierTextWelsh = "Meddiannydd"
  val ownerOccupierTextWelsh = "Perchennog a meddiannydd"
  val yesTextWelsh = "Ydw"
  val noTextWelsh = "Nac ydw"
  val leaseEvidenceTextWelsh = "Prydles Test File"
  val licenseEvidenceTextWelsh = "Trwydded i feddiannu Test File"
  val serviceChargeTextWelsh = "Datganiad tâl gwasanaeth Test File"
  val stampDutyLandTaxFormTextWelsh = "Ffurflen Treth Dir y Dreth Stamp Test File"
  val waterRateDemandTextWelsh = "Archeb trethi dŵr Test File"
  val otherUtilityBillTextWelsh = "Bil cyfleustodau Test File"
  val ratesBillTypeTextWelsh = "Bil ardrethi busnes Test File"
  val landRegistryTitleTextWelsh = "Teitl y Gofrestrfa Tir Test File"
  val thereIsAProblemTextWelsh = "Mae yna broblem"
  val errorTextWelsh = "Gwall: "
  val errorMessageTextWelsh = "Rhaid i chi gytuno â’r datganiad er mwyn parhau"

  val captionSelector = "#caption"
  val headerSelector = "h1"
  val addressHeaderSelector = "#address-heading"
  val addressValueSelector = "#address-value"
  val connectionToPropertyHeaderSelector = "#relationship-heading"
  val connectionToPropertyValueSelector = "#relationship-value"
  val connectionToPropertyHrefSelector = "#relationship-change"
  val startedHeaderSelector = "#start-date-heading"
  val startedValueSelector = "#start-date-value"
  val startedHrefSelector = "#start-date-change"
  val doYouStillOwnHeaderSelector = "#still-owned-heading"
  val doYouStillOwnValueSelector = "#still-owned-value"
  val doYouStillOwnHrefSelector = "#still-owned-change"
  val evidenceHeaderSelector = "#evidence-heading"
  val evidenceValueSelector = "#evidence-value"
  val evidenceHrefSelector = "#evidence-change"
  val lastDayOwnerHeaderSelector = "#end-date-heading"
  val lastDayOwnerValueSelector = "#end-date-value"
  val lastDayOwnerHrefSelector = "#end-date-change"
  val declarationSelector = "#main-content > div > div > form > h2"
  val warningSelector = "#main-content > div > div > form > div.govuk-warning-text"
  val iDeclareTextSelector = "#main-content > div > div > form > div.govuk-form-group > div > div > label"
  val iDeclareCheckboxSelector = "#declaration"
  val confirmSelector = "#confirmAndSend"
  val errorSummaryTitleSelector = "#main-content > div > div > div > div > h2"
  val errorSummaryErrorSelector = "#main-content > div > div > div > div > div > ul > li > a"
  val errorAboveCheckboxSelector = "#declaration-error"

  val backHref = "/business-rates-property-linking/my-organisation/claim/property-links/summary/back"
  val changeConnectionHref = "/business-rates-property-linking/my-organisation/claim/property-links"
  val changeStartDateHref = "/business-rates-property-linking/my-organisation/claim/property-links/ownership"
  val changeStillOwnerHref = "/business-rates-property-linking/my-organisation/claim/property-links/occupancy"
  val changeEvidenceHref = "/business-rates-property-linking/my-organisation/claim/property-links/evidence"
  val changeEndDateHref = "/business-rates-property-linking/my-organisation/claim/property-links/occupancy"
  val errorSummaryHref = "#declaration"

  //TODO: Does there need to be scenarios for when !earliestStartDateInPast for when the rows dont show? Dont think you can do this
  //TODO: Does there need to be scenarios for the other evidence types that cant actually be chosen in the flow?
  //TODO: Does there need to be the scenario that triggers declaration.checkAnswers.startDate.onOrBefore?

  "DeclarationController show method displays the correct content in English for an IP who no longer owns and has a Lease" which {
    lazy val document = getShowPage(language = English, userIsAgent = false, relationshipChoice = "owner",
      stillOwnedOccupiedChoice = false, evidenceChoice = "Lease")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe ownerText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnText row with the $noText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnText
      document.select(doYouStillOwnValueSelector).text shouldBe noText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOwnText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOwnerText row with the $lastDayValue and $changeText link" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOwnerText
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeText + lastDayOwnerText
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $leaseEvidenceText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe leaseEvidenceText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in English for an IP who no longer occupies and has a License" which {
    lazy val document = getShowPage(language = English, userIsAgent = false, relationshipChoice = "occupier",
      stillOwnedOccupiedChoice = false, evidenceChoice = "License")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $occupierText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe occupierText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOccupyText row with the $noText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOccupyText
      document.select(doYouStillOwnValueSelector).text shouldBe noText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOccupyText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOccupierText row with the $lastDayValue and $changeText link" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOccupierText
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeText + lastDayOccupierText
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $licenseEvidenceText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe licenseEvidenceText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in English for an IP who no longer owns and occupies and has a Service Charge" which {
    lazy val document = getShowPage(language = English, userIsAgent = false, relationshipChoice = "both",
      stillOwnedOccupiedChoice = false, evidenceChoice = "ServiceCharge")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerOccupierText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe ownerOccupierText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnOccupyText row with the $noText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnOccupyText
      document.select(doYouStillOwnValueSelector).text shouldBe noText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOwnOccupyText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOwnerOccupierText row with the $lastDayValue and $changeText link" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOwnerOccupierText
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeText + lastDayOwnerOccupierText
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $serviceChargeText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe serviceChargeText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in English for an IP who still owns and has a StampDutyLandTaxForm" which {
    lazy val document = getShowPage(language = English, userIsAgent = false, relationshipChoice = "owner",
      stillOwnedOccupiedChoice = true, evidenceChoice = "StampDutyLandTaxForm")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe ownerText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnText row with the $noText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnText
      document.select(doYouStillOwnValueSelector).text shouldBe yesText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOwnText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $stampDutyLandTaxFormText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe stampDutyLandTaxFormText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in English for an IP who still occupier and has a WaterRateDemand" which {
    lazy val document = getShowPage(language = English, userIsAgent = false, relationshipChoice = "occupier",
      stillOwnedOccupiedChoice = true, evidenceChoice = "WaterRateDemand")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $occupierText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe occupierText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOccupyText row with the $yesText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOccupyText
      document.select(doYouStillOwnValueSelector).text shouldBe yesText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOccupyText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $waterRateDemandText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe waterRateDemandText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in English for an IP who still owns and occupies and has a OtherUtilityBill" which {
    lazy val document = getShowPage(language = English, userIsAgent = false, relationshipChoice = "both",
      stillOwnedOccupiedChoice = true, evidenceChoice = "OtherUtilityBill")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerOccupierText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe ownerOccupierText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnOccupyText row with the $yesText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnOccupyText
      document.select(doYouStillOwnValueSelector).text shouldBe yesText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOwnOccupyText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $otherUtilityBillText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe otherUtilityBillText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in English for an agent who no longer owns and has a RatesBill" which {
    lazy val document = getShowPage(language = English, userIsAgent = true, relationshipChoice = "owner",
      stillOwnedOccupiedChoice = false, evidenceChoice = "RatesBill")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe ownerText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnAgentText row with the $noText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnAgentText
      document.select(doYouStillOwnValueSelector).text shouldBe noText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOwnAgentText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOwnerText row with the $lastDayValue and $changeText link" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOwnerText
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeText + lastDayOwnerText
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $ratesBillTypeText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe ratesBillTypeText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareAgentText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in English for an agent who no longer occupies and has a LandRegistryTitle" which {
    lazy val document = getShowPage(language = English, userIsAgent = true, relationshipChoice = "occupier",
      stillOwnedOccupiedChoice = false, evidenceChoice = "LandRegistryTitle")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $occupierText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe occupierText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOccupyAgentText row with the $noText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOccupyAgentText
      document.select(doYouStillOwnValueSelector).text shouldBe noText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOccupyAgentText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOccupierText row with the $lastDayValue and $changeText link" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOccupierText
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeText + lastDayOccupierText
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $landRegistryTitleText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe landRegistryTitleText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareAgentText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in English for an agent who no longer owns and occupies and has a Service Charge" which {
    lazy val document = getShowPage(language = English, userIsAgent = true, relationshipChoice = "both",
      stillOwnedOccupiedChoice = false, evidenceChoice = "ServiceCharge")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerOccupierText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe ownerOccupierText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnOccupyAgentText row with the $noText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnOccupyAgentText
      document.select(doYouStillOwnValueSelector).text shouldBe noText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOwnOccupyAgentText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOwnerOccupierText row with the $lastDayValue and $changeText link" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOwnerOccupierText
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeText + lastDayOwnerOccupierText
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $serviceChargeText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe serviceChargeText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareAgentText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in English for an agent who still owns and has a StampDutyLandTaxForm" which {
    lazy val document = getShowPage(language = English, userIsAgent = true, relationshipChoice = "owner",
      stillOwnedOccupiedChoice = true, evidenceChoice = "StampDutyLandTaxForm")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe ownerText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnAgentText row with the $noText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnAgentText
      document.select(doYouStillOwnValueSelector).text shouldBe yesText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOwnAgentText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $stampDutyLandTaxFormText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe stampDutyLandTaxFormText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareAgentText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in English for an agent who still occupier and has a WaterRateDemand" which {
    lazy val document = getShowPage(language = English, userIsAgent = true, relationshipChoice = "occupier",
      stillOwnedOccupiedChoice = true, evidenceChoice = "WaterRateDemand")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $occupierText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe occupierText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOccupyAgentText row with the $yesText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOccupyAgentText
      document.select(doYouStillOwnValueSelector).text shouldBe yesText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOccupyAgentText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $waterRateDemandText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe waterRateDemandText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareAgentText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in English for an agent who still owns and occupies and has a OtherUtilityBill" which {
    lazy val document = getShowPage(language = English, userIsAgent = true, relationshipChoice = "both",
      stillOwnedOccupiedChoice = true, evidenceChoice = "OtherUtilityBill")

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerOccupierText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe ownerOccupierText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnOccupyAgentText row with the $yesText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnOccupyAgentText
      document.select(doYouStillOwnValueSelector).text shouldBe yesText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOwnOccupyAgentText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $otherUtilityBillText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe otherUtilityBillText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an $iDeclareAgentText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an IP who no longer owns and has a Lease" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = false, relationshipChoice = "owner",
      stillOwnedOccupiedChoice = false, evidenceChoice = "Lease")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe ownerTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnText row with the $noText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe noTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOwnTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOwnerText row with the $lastDayValue and $changeText link in welsh" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOwnerTextWelsh
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeTextWelsh + lastDayOwnerTextWelsh
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $leaseEvidenceText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe leaseEvidenceTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an IP who no longer occupies and has a License" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = false, relationshipChoice = "occupier",
      stillOwnedOccupiedChoice = false, evidenceChoice = "License")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $occupierText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe occupierTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOccupyText row with the $noText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOccupyTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe noTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOccupyTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOccupierText row with the $lastDayValue and $changeText link in welsh" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOccupierTextWelsh
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeTextWelsh + lastDayOccupierTextWelsh
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $licenseEvidenceText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe licenseEvidenceTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an IP who no longer owns and occupies and has a Service Charge" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = false, relationshipChoice = "both",
      stillOwnedOccupiedChoice = false, evidenceChoice = "ServiceCharge")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerOccupierText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe ownerOccupierTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnOccupyText row with the $noText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnOccupyTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe noTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOwnOccupyTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOwnerOccupierText row with the $lastDayValue and $changeText link in welsh" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOwnerOccupierTextWelsh
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeTextWelsh + lastDayOwnerOccupierTextWelsh
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $serviceChargeText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe serviceChargeTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an IP who still owns and has a StampDutyLandTaxForm" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = false, relationshipChoice = "owner",
      stillOwnedOccupiedChoice = true, evidenceChoice = "StampDutyLandTaxForm")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe ownerTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnText row with the $noText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe yesTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOwnTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row in welsh" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $stampDutyLandTaxFormText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe stampDutyLandTaxFormTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an IP who still occupier and has a WaterRateDemand" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = false, relationshipChoice = "occupier",
      stillOwnedOccupiedChoice = true, evidenceChoice = "WaterRateDemand")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $occupierText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe occupierTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOccupyText row with the $yesText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOccupyTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe yesTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOccupyTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row in welsh" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $waterRateDemandText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe waterRateDemandTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an IP who still owns and occupies and has a OtherUtilityBill" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = false, relationshipChoice = "both",
      stillOwnedOccupiedChoice = true, evidenceChoice = "OtherUtilityBill")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerOccupierText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe ownerOccupierTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnOccupyText row with the $yesText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnOccupyTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe yesTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOwnOccupyTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row in welsh" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $otherUtilityBillText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe otherUtilityBillTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an agent who no longer owns and has a RatesBill" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = true, relationshipChoice = "owner",
      stillOwnedOccupiedChoice = false, evidenceChoice = "RatesBill")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe ownerTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnAgentText row with the $noText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnAgentTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe noTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOwnAgentTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOwnerText row with the $lastDayValue and $changeText link in welsh" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOwnerTextWelsh
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeTextWelsh + lastDayOwnerTextWelsh
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $ratesBillTypeText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe ratesBillTypeTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareAgentText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an agent who no longer occupies and has a LandRegistryTitle" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = true, relationshipChoice = "occupier",
      stillOwnedOccupiedChoice = false, evidenceChoice = "LandRegistryTitle")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $occupierText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe occupierTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOccupyAgentText row with the $noText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOccupyAgentTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe noTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOccupyAgentTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOccupierText row with the $lastDayValue and $changeText link in welsh" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOccupierTextWelsh
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeTextWelsh + lastDayOccupierTextWelsh
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $landRegistryTitleText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe landRegistryTitleTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareAgentText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an agent who no longer owns and occupies and has a Service Charge" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = true, relationshipChoice = "both",
      stillOwnedOccupiedChoice = false, evidenceChoice = "ServiceCharge")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerOccupierText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe ownerOccupierTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnOccupyAgentText row with the $noText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnOccupyAgentTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe noTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOwnOccupyAgentTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOwnerOccupierText row with the $lastDayValue and $changeText link in welsh" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOwnerOccupierTextWelsh
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeTextWelsh + lastDayOwnerOccupierTextWelsh
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $serviceChargeText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe serviceChargeTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareAgentText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an agent who still owns and has a StampDutyLandTaxForm" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = true, relationshipChoice = "owner",
      stillOwnedOccupiedChoice = true, evidenceChoice = "StampDutyLandTaxForm")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe ownerTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnAgentText row with the $noText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnAgentTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe yesTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOwnAgentTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row in welsh" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $stampDutyLandTaxFormText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe stampDutyLandTaxFormTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareAgentText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an agent who still occupier and has a WaterRateDemand" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = true, relationshipChoice = "occupier",
      stillOwnedOccupiedChoice = true, evidenceChoice = "WaterRateDemand")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $occupierText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe occupierTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOccupyAgentText row with the $yesText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOccupyAgentTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe yesTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOccupyAgentTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row in welsh" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $waterRateDemandText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe waterRateDemandTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareAgentText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController show method displays the correct content in Welsh for an agent who still owns and occupies and has a OtherUtilityBill" which {
    lazy val document = getShowPage(language = Welsh, userIsAgent = true, relationshipChoice = "both",
      stillOwnedOccupiedChoice = true, evidenceChoice = "OtherUtilityBill")

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a caption above the heading of of $captionText in welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a header of of $headerText in welsh" in {
      document.select(headerSelector).text shouldBe headerTextWelsh
    }

    s"has an $addressText row with the $addressValue in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerOccupierText and $changeText link in welsh" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyTextWelsh
      document.select(connectionToPropertyValueSelector).text shouldBe ownerOccupierTextWelsh
      document.select(connectionToPropertyHrefSelector).text shouldBe changeTextWelsh + connectionToPropertyTextWelsh
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link in welsh" in {
      document.select(startedHeaderSelector).text shouldBe startedTextWelsh
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeTextWelsh + startedTextWelsh
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnOccupyAgentText row with the $yesText and $changeText link in welsh" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnOccupyAgentTextWelsh
      document.select(doYouStillOwnValueSelector).text shouldBe yesTextWelsh
      document.select(doYouStillOwnHrefSelector).text shouldBe changeTextWelsh + stillOwnOccupyAgentTextWelsh
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"should not have a $lastDayOwnerText row in welsh" in {
      document.select(lastDayOwnerHeaderSelector).size() shouldBe 0
      document.select(lastDayOwnerValueSelector).size() shouldBe 0
      document.select(lastDayOwnerHrefSelector).size() shouldBe 0
    }

    s"has an $evidenceText row with the $otherUtilityBillText and $changeText link in welsh" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceTextWelsh
      document.select(evidenceValueSelector).text shouldBe otherUtilityBillTextWelsh
      document.select(evidenceHrefSelector).text shouldBe changeTextWelsh + evidenceTextWelsh
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText in welsh" in {
      document.select(declarationSelector).text shouldBe declarationTextWelsh
    }

    s"has a warning on the screen of $youCouldBeText in welsh" in {
      document.select(warningSelector).text shouldBe youCouldBeTextWelsh
    }

    s"has an $iDeclareAgentText checkbox in welsh" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareAgentTextWelsh
    }

    s"has a $confirmText button in welsh" in {
      document.select(confirmSelector).text shouldBe confirmTextWelsh
    }

  }

  "DeclarationController submit method redirects to the correct page when a user confirms the declaration and clicks submit" in {
    val res = postSubmitPage(userIsAgent = true, relationshipChoice = "both",
      stillOwnedOccupiedChoice = true, evidenceChoice = "OtherUtilityBill")

    res.status shouldBe SEE_OTHER
    res.header("Location") shouldBe Some("/business-rates-property-linking/my-organisation/claim/property-links/confirmation")
  }

  "DeclarationController submit method returns a bad request and shows an error on the page when a user doesn't click confirm and then clicks submit" which {
    lazy val res = postSubmitPageWithError(language = English, userIsAgent = false, relationshipChoice = "owner",
      stillOwnedOccupiedChoice = false, evidenceChoice = "Lease")

    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of ${errorText + titleText}" in {
      document.title() shouldBe errorText + titleText
    }

    s"has an error summary that contains the correct error message $errorMessageText" in {
      document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
      document.select(errorSummaryErrorSelector).text() shouldBe errorMessageText
    }

    s"has a caption above the heading of of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a header of of $headerText" in {
      document.select(headerSelector).text shouldBe headerText
    }

    s"has an $addressText row with the $addressValue" in {
      document.select(addressHeaderSelector).text shouldBe addressText
      document.select(addressValueSelector).text shouldBe addressValue
    }

    s"has an $connectionToPropertyText row with the $ownerText and $changeText link" in {
      document.select(connectionToPropertyHeaderSelector).text shouldBe connectionToPropertyText
      document.select(connectionToPropertyValueSelector).text shouldBe ownerText
      document.select(connectionToPropertyHrefSelector).text shouldBe changeText + connectionToPropertyText
      document.select(connectionToPropertyHrefSelector).attr("href") shouldBe changeConnectionHref
    }

    s"has an $startedText row with the $startedValue and $changeText link" in {
      document.select(startedHeaderSelector).text shouldBe startedText
      document.select(startedValueSelector).text shouldBe startedValue
      document.select(startedHrefSelector).text shouldBe changeText + startedText
      document.select(startedHrefSelector).attr("href") shouldBe changeStartDateHref
    }

    s"has an $stillOwnText row with the $noText and $changeText link" in {
      document.select(doYouStillOwnHeaderSelector).text shouldBe stillOwnText
      document.select(doYouStillOwnValueSelector).text shouldBe noText
      document.select(doYouStillOwnHrefSelector).text shouldBe changeText + stillOwnText
      document.select(doYouStillOwnHrefSelector).attr("href") shouldBe changeStillOwnerHref
    }

    s"has an $lastDayOwnerText row with the $lastDayValue and $changeText link" in {
      document.select(lastDayOwnerHeaderSelector).text shouldBe lastDayOwnerText
      document.select(lastDayOwnerValueSelector).text shouldBe lastDayValue
      document.select(lastDayOwnerHrefSelector).text shouldBe changeText + lastDayOwnerText
      document.select(lastDayOwnerHrefSelector).attr("href") shouldBe changeEndDateHref
    }

    s"has an $evidenceText row with the $leaseEvidenceText and $changeText link" in {
      document.select(evidenceHeaderSelector).text shouldBe evidenceText
      document.select(evidenceValueSelector).text shouldBe leaseEvidenceText
      document.select(evidenceHrefSelector).text shouldBe changeText + evidenceText
      document.select(evidenceHrefSelector).attr("href") shouldBe changeEvidenceHref
    }

    s"has a subheading of $declarationText" in {
      document.select(declarationSelector).text shouldBe declarationText
    }

    s"has a warning on the screen of $youCouldBeText" in {
      document.select(warningSelector).text shouldBe youCouldBeText
    }

    s"has an error message above the check box of ${errorText + errorMessageText}" in {
      document.select(errorAboveCheckboxSelector).text() shouldBe errorText + errorMessageText
    }

    s"has an $iDeclareText checkbox" in {
      document.select(iDeclareCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(iDeclareTextSelector).text shouldBe iDeclareText
    }

    s"has a $confirmText button" in {
      document.select(confirmSelector).text shouldBe confirmText
    }
  }


  private def getShowPage(language: Language, userIsAgent: Boolean, relationshipChoice: String, stillOwnedOccupiedChoice: Boolean, evidenceChoice: String) = {
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

    val relationshipType: Option[PropertyRelationship] = relationshipChoice match {
      case "owner" => Some(PropertyRelationship(Owner, 1L))
      case "occupier" => Some(PropertyRelationship(Occupier, 1L))
      case "both" => Some(PropertyRelationship(OwnerOccupier, 1L))
      case _ => None
    }

    val stillOwnedOccupied: Option[PropertyOccupancy] = if (stillOwnedOccupiedChoice) {
      Some(PropertyOccupancy(stillOccupied = true, Some(LocalDate.of(2017, 4, 3))))
    } else {
      Some(PropertyOccupancy(stillOccupied = false, Some(LocalDate.of(2017, 4, 3))))
    }

    val evidenceType: UploadEvidenceData = evidenceChoice match {
      case "Lease" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", Lease)))
      case "License" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", License)))
      case "ServiceCharge" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", ServiceCharge)))
      case "StampDutyLandTaxForm" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", StampDutyLandTaxForm)))
      case "WaterRateDemand" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", WaterRateDemand)))
      case "OtherUtilityBill" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", OtherUtilityBill)))
      case "RatesBill" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", RatesBillType)))
      case "LandRegistryTitle" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", LandRegistryTitle)))
    }

    await(
      mockPropertyLinkingSessionRepository.saveOrUpdate(
        LinkingSession(
          address = "Test Address, Test Lane, T35 T3R",
          uarn = 1L,
          submissionId = "PL-123456",
          personId = 1L,
          earliestStartDate = LocalDate.of(2017, 4, 1),
          propertyRelationship = relationshipType,
          propertyOwnership = Some(PropertyOwnership(LocalDate.of(2017, 4, 2))),
          propertyOccupancy = stillOwnedOccupied,
          hasRatesBill = Some(true),
          uploadEvidenceData = evidenceType,
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

  //TODO: Theres scenarios around the evidence NotAllFilesReadyToUpload/MissingRequiredNumberOfFiles can a user ever actually do this?
  //TODO: Any need to do a POST for anything other than a generic user? Body only has declaration = true
  //TODO: Remember to do the error scenario of no declaration value
  //TODO: If you post declaration false, it still goes through fine - surely that's wrong (obviously a user cant actually do this)
  private def postSubmitPage(userIsAgent: Boolean, relationshipChoice: String, stillOwnedOccupiedChoice: Boolean, evidenceChoice: String) = {
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

    stubFor {
      post("/property-linking/clients/123/property-links")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

    val attachmentsResponseBody = s"""{
      "_id": "${UUID.randomUUID()}",
      "initiatedAt": "${Instant.now()}",
      "fileName": "fileName",
      "mimeType": "image/jpeg",
      "destination": "DESTINATION",
      "data": {},
      "state": "UploadAttachmentComplete",
      "history":[],
      "scanResult": null,
      "initiateResult": null,
      "principal": {
        "externalId": "gg-external-id",
        "groupId": "gg-group-id"
      }
    }"""

    stubFor {
      get("/business-rates-attachments/attachments/1862956069192540")
        .willReturn {
          aResponse.withStatus(OK).withBody(attachmentsResponseBody)
        }
    }

    val relationshipType: Option[PropertyRelationship] = relationshipChoice match {
      case "owner" => Some(PropertyRelationship(Owner, 1L))
      case "occupier" => Some(PropertyRelationship(Occupier, 1L))
      case "both" => Some(PropertyRelationship(OwnerOccupier, 1L))
      case _ => None
    }

    val stillOwnedOccupied: Option[PropertyOccupancy] = if (stillOwnedOccupiedChoice) {
      Some(PropertyOccupancy(stillOccupied = true, Some(LocalDate.of(2017, 4, 3))))
    } else {
      Some(PropertyOccupancy(stillOccupied = false, Some(LocalDate.of(2017, 4, 3))))
    }

    val evidenceType: UploadEvidenceData = evidenceChoice match {
      case "Lease" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", Lease)))
      case "License" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", License)))
      case "ServiceCharge" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", ServiceCharge)))
      case "StampDutyLandTaxForm" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", StampDutyLandTaxForm)))
      case "WaterRateDemand" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", WaterRateDemand)))
      case "OtherUtilityBill" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", OtherUtilityBill)))
      case "RatesBill" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", RatesBillType)))
      case "LandRegistryTitle" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", LandRegistryTitle)))
    }

    def fileInfo(evType: EvidenceType): CompleteFileInfo = CompleteFileInfo("test.pdf", evType)

    val FILE_REFERENCE: String = "1862956069192540"
    val preparedUpload = PreparedUpload(Reference(FILE_REFERENCE), UploadFormTemplate("http://localhost/upscan", Map()))
    val fileMetadata = FileMetadata(FILE_REFERENCE, "application/pdf")

    val uploadedFileDetails = UploadedFileDetails(fileMetadata, preparedUpload)

    def uploadEvidenceData(fileInfo: FileInfo, flag: LinkBasis = RatesBillFlag): UploadEvidenceData =
      UploadEvidenceData(flag, Some(fileInfo), Some(Map(FILE_REFERENCE -> uploadedFileDetails)))

    val uploadRatesBillData: UploadEvidenceData = uploadEvidenceData(fileInfo(RatesBillType))


    await(
      mockPropertyLinkingSessionRepository.saveOrUpdate(
        LinkingSession(
          address = "Test Address, Test Lane, T35 T3R",
          uarn = 1L,
          submissionId = "PL-123456",
          personId = 1L,
          earliestStartDate = LocalDate.of(2017, 4, 1),
          propertyRelationship = relationshipType,
          propertyOwnership = Some(PropertyOwnership(LocalDate.of(2017, 4, 2))),
          propertyOccupancy = stillOwnedOccupied,
          hasRatesBill = Some(true),
          uploadEvidenceData = uploadRatesBillData,
          evidenceType = Some(Lease),
          clientDetails = if (userIsAgent) Some(ClientDetails(123, "Client Name")) else None,
          localAuthorityReference = "2050466366770",
          rtp = ClaimPropertyReturnToPage.FMBR,
          fromCya = None,
          isSubmitted = None
        )))

    val requestBody: JsObject = Json.obj(
      "declaration" -> "true"
    )

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/summary")
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )
  }

  private def postSubmitPageWithError(language: Language, userIsAgent: Boolean, relationshipChoice: String, stillOwnedOccupiedChoice: Boolean, evidenceChoice: String) = {
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

    stubFor {
      post("/property-linking/clients/123/property-links")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

    val attachmentsResponseBody =
      s"""{
    "_id": "${UUID.randomUUID()}",
    "initiatedAt": "${Instant.now()}",
    "fileName": "fileName",
    "mimeType": "image/jpeg",
    "destination": "DESTINATION",
    "data": {},
    "state": "UploadAttachmentComplete",
    "history":[],
    "scanResult": null,
    "initiateResult": null,
    "principal": {
      "externalId": "gg-external-id",
      "groupId": "gg-group-id"
    }
  }"""

    stubFor {
      get("/business-rates-attachments/attachments/1862956069192540")
        .willReturn {
          aResponse.withStatus(OK).withBody(attachmentsResponseBody)
        }
    }

    val relationshipType: Option[PropertyRelationship] = relationshipChoice match {
      case "owner" => Some(PropertyRelationship(Owner, 1L))
      case "occupier" => Some(PropertyRelationship(Occupier, 1L))
      case "both" => Some(PropertyRelationship(OwnerOccupier, 1L))
      case _ => None
    }

    val stillOwnedOccupied: Option[PropertyOccupancy] = if (stillOwnedOccupiedChoice) {
      Some(PropertyOccupancy(stillOccupied = true, Some(LocalDate.of(2017, 4, 3))))
    } else {
      Some(PropertyOccupancy(stillOccupied = false, Some(LocalDate.of(2017, 4, 3))))
    }

    val evidenceType: UploadEvidenceData = evidenceChoice match {
      case "Lease" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", Lease)))
      case "License" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", License)))
      case "ServiceCharge" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", ServiceCharge)))
      case "StampDutyLandTaxForm" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", StampDutyLandTaxForm)))
      case "WaterRateDemand" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", WaterRateDemand)))
      case "OtherUtilityBill" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", OtherUtilityBill)))
      case "RatesBill" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", RatesBillType)))
      case "LandRegistryTitle" => UploadEvidenceData(fileInfo = Some(CompleteFileInfo("Test File", LandRegistryTitle)))
    }

    def fileInfo(evType: EvidenceType): CompleteFileInfo = CompleteFileInfo("Test File", evType)

    val FILE_REFERENCE: String = "1862956069192540"
    val preparedUpload = PreparedUpload(Reference(FILE_REFERENCE), UploadFormTemplate("http://localhost/upscan", Map()))
    val fileMetadata = FileMetadata(FILE_REFERENCE, "application/pdf")

    val uploadedFileDetails = UploadedFileDetails(fileMetadata, preparedUpload)

    def uploadEvidenceData(fileInfo: FileInfo, flag: LinkBasis = RatesBillFlag): UploadEvidenceData =
      UploadEvidenceData(flag, Some(fileInfo), Some(Map(FILE_REFERENCE -> uploadedFileDetails)))

    val uploadRatesBillData: UploadEvidenceData = uploadEvidenceData(fileInfo(Lease))


    await(
      mockPropertyLinkingSessionRepository.saveOrUpdate(
        LinkingSession(
          address = "Test Address, Test Lane, T35 T3R",
          uarn = 1L,
          submissionId = "PL-123456",
          personId = 1L,
          earliestStartDate = LocalDate.of(2017, 4, 1),
          propertyRelationship = relationshipType,
          propertyOwnership = Some(PropertyOwnership(LocalDate.of(2017, 4, 2))),
          propertyOccupancy = stillOwnedOccupied,
          hasRatesBill = Some(true),
          uploadEvidenceData = uploadRatesBillData,
          evidenceType = Some(Lease),
          clientDetails = if (userIsAgent) Some(ClientDetails(123, "Client Name")) else None,
          localAuthorityReference = "2050466366770",
          rtp = ClaimPropertyReturnToPage.FMBR,
          fromCya = None,
          isSubmitted = None
        )))

    val requestBody: JsObject = Json.obj(
      "declaration" -> ""
    )

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/summary")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )
  }

}
