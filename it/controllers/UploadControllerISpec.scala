package controllers

import base.{HtmlComponentHelpers, ISpecBase}
import binders.propertylinks.{ClaimPropertyReturnToPage, EvidenceChoices}
import binders.propertylinks.EvidenceChoices.EvidenceChoices
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.upscan.{PreparedUpload, Reference, UploadFormTemplate}
import models.{ClientDetails, EvidenceType, LandRegistryTitle, LinkingSession, Occupier, PropertyOccupancy, PropertyOwnership, PropertyRelationship, RatesBillType, UploadEvidenceData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.{ManageAgentSessionRepository, PropertyLinkingSessionRepository}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class UploadControllerISpec extends ISpecBase with HtmlComponentHelpers {

  lazy val mockRepository: PropertyLinkingSessionRepository = app.injector.instanceOf[PropertyLinkingSessionRepository]
  val testSessionId = s"stubbed-${UUID.randomUUID}"
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))
  val rateTitleText = "Upload your business rates bill - Valuation Office Agency - GOV.UK"
  val rateHeaderText = "Upload your business rates bill"
  val waterTitleText = "Upload your water rate demand - Valuation Office Agency - GOV.UK"
  val waterHeaderText = "Upload your water rate demand"
  val lRTitleText = "Upload your Land Registry title - Valuation Office Agency - GOV.UK"
  val lRHeaderText = "Upload your Land Registry title"
  val sCTitleText = "Upload your service charge statement - Valuation Office Agency - GOV.UK"
  val sCHeaderText = "Upload your service charge statement"
  val fileUploadText =
    "The file must be a Word document, Excel spreadsheet, PDF or image (PNG or JPG) and be less than 10MB."

  val rateTitleTextWelsh = "Lanlwytho eich bil ardrethi busnes - Valuation Office Agency - GOV.UK"
  val rateHeaderTextWelsh = "Lanlwytho eich bil ardrethi busnes"
  val waterTitleTextWelsh = "Lanlwytho eich archeb trethi dŵr - Valuation Office Agency - GOV.UK"
  val waterHeaderTextWelsh = "Lanlwytho eich archeb trethi dŵr"
  val lRTitleTextWelsh = "Lanlwytho eich teitl gofrestrfa tir - Valuation Office Agency - GOV.UK"
  val lRHeaderTextWelsh = "Lanlwytho eich teitl gofrestrfa tir"
  val sCTitleTextWelsh = "Lanlwytho eich datganiad tâl gwasanaeth - Valuation Office Agency - GOV.UK"
  val sCHeaderTextWelsh = "Lanlwytho eich datganiad tâl gwasanaeth"
  val fileUploadTextWelsh =
    "Rhaid i’r ffeil fod yn ddogfen Word, taenlen Excel, PDF neu ddelwedd (PNG neu JPG) a bod yn llai na 10MB."

  val otherEvidenceTitleText = "What evidence can you provide? - Valuation Office Agency - GOV.UK"
  val otherEvidenceHeaderText = "What evidence can you provide?"
  val otherEvidenceHintText = "Your evidence should be for LS"
  val otherEvidenceRadioOptions =
    "Business rates bill Service charge statement Stamp Duty Land Tax form Land Registry title Water rate demand Utility bill or I cannot provide evidence"

  val otherEvidenceTitleTextWelsh = "Pa dystiolaeth allwch chi ei darparu? - Valuation Office Agency - GOV.UK"
  val otherEvidenceHeaderTextWelsh = "Pa dystiolaeth allwch chi ei darparu?"
  val otherEvidenceHintTextWelsh = "Dylai eich tystiolaeth fod ar gyfer LS"
  val otherEvidenceRadioOptionsWelsh =
    "Bil ardrethi busnes Datganiad tâl gwasanaeth Ffurflen Treth Dir y Dreth Stamp Teitl y Gofrestrfa Tir Archeb trethi dŵr Bil cyfleustodau neu Ni allaf ddarparu tystiolaeth"

  val headerSelector = "h1"
  val chooseFileId = "choose-file"

  def thisAgentTextSingle(listYear: String) =
    s"This agent can act for you on your property valuations on the $listYear rating list, for properties that you assign to them or they add to your account"

  def thisAgentTextSingleWelsh(listYear: String) =
    s"Welsh This agent can act for you on your property valuations on the $listYear rating list, for properties that you assign to them or they add to your account"

  "UploadController" should {
    s"Show main upload page in english with ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getEvidencePage(English, EvidenceChoices.RATES_BILL)

      s"has a title of $rateTitleText" in {
        document.title() shouldBe s"$rateTitleText"
      }

      s"has a panel containing a header of '$rateHeaderText'" in {
        document.select(headerSelector).text shouldBe rateHeaderText
      }

      s"has hint text on the screen of '$fileUploadText'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadText
      }

      s"has a choose file button" in {
        document.getElementById(chooseFileId).className() shouldBe "govuk-file-upload"
      }
    }

    s"Show main upload page in welsh ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getEvidencePage(Welsh, EvidenceChoices.RATES_BILL)

      s"has a title of $rateTitleTextWelsh" in {
        document.title() shouldBe s"$rateTitleTextWelsh"
      }

      s"has a panel containing a header of '$rateHeaderTextWelsh'" in {
        document.select(headerSelector).text shouldBe rateHeaderTextWelsh
      }

      s"has hint text on the screen of '$fileUploadTextWelsh'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadTextWelsh
      }

      s"has a choose file button" in {
        document.getElementById(chooseFileId).className() shouldBe "govuk-file-upload"
      }
    }

    s"Show main upload page in english with ${EvidenceChoices.LAND_REGISTRY}" which {

      lazy val document = getEvidencePage(English, EvidenceChoices.LAND_REGISTRY)

      s"has a title of $lRTitleText" in {
        document.title() shouldBe s"$lRTitleText"
      }

      s"has a panel containing a header of '$lRHeaderText'" in {
        document.select(headerSelector).text shouldBe lRHeaderText
      }

      s"has hint text on the screen of '$fileUploadText'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadText
      }

      s"has a choose file button" in {
        document.getElementById(chooseFileId).className() shouldBe "govuk-file-upload"
      }
    }

    s"Show main upload page in welsh ${EvidenceChoices.LAND_REGISTRY}" which {

      lazy val document = getEvidencePage(Welsh, EvidenceChoices.LAND_REGISTRY)

      s"has a title of $lRTitleTextWelsh" in {
        document.title() shouldBe s"$lRTitleTextWelsh"
      }

      s"has a panel containing a header of '$lRHeaderTextWelsh'" in {
        document.select(headerSelector).text shouldBe lRHeaderTextWelsh
      }

      s"has hint text on the screen of '$fileUploadTextWelsh'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadTextWelsh
      }

      s"has a choose file button" in {
        document.getElementById(chooseFileId).className() shouldBe "govuk-file-upload"
      }
    }

    s"Show main upload page in english with ${EvidenceChoices.SERVICE_CHARGE}" which {

      lazy val document = getEvidencePage(English, EvidenceChoices.SERVICE_CHARGE)

      s"has a title of $sCTitleText" in {
        document.title() shouldBe s"$sCTitleText"
      }

      s"has a panel containing a header of '$sCHeaderText'" in {
        document.select(headerSelector).text shouldBe sCHeaderText
      }

      s"has hint text on the screen of '$fileUploadText'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadText
      }

      s"has a choose file button" in {
        document.getElementById(chooseFileId).className() shouldBe "govuk-file-upload"
      }
    }

    s"Show main upload page in welsh ${EvidenceChoices.SERVICE_CHARGE}" which {

      lazy val document = getEvidencePage(Welsh, EvidenceChoices.SERVICE_CHARGE)

      s"has a title of $sCTitleTextWelsh" in {
        document.title() shouldBe s"$sCTitleTextWelsh"
      }

      s"has a panel containing a header of '$sCHeaderTextWelsh'" in {
        document.select(headerSelector).text shouldBe sCHeaderTextWelsh
      }

      s"has hint text on the screen of '$fileUploadTextWelsh'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadTextWelsh
      }

      s"has a choose file button" in {
        document.getElementById(chooseFileId).className() shouldBe "govuk-file-upload"
      }
    }

    s"Show main upload page in english with ${EvidenceChoices.WATER_RATE}" which {

      lazy val document = getEvidencePage(English, EvidenceChoices.WATER_RATE)

      s"has a title of $waterTitleText" in {
        document.title() shouldBe s"$waterTitleText"
      }

      s"has a panel containing a header of '$waterHeaderText'" in {
        document.select(headerSelector).text shouldBe waterHeaderText
      }

      s"has hint text on the screen of '$fileUploadText'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadText
      }

      s"has a choose file button" in {
        document.getElementById(chooseFileId).className() shouldBe "govuk-file-upload"
      }
    }

    s"Show main upload page in welsh ${EvidenceChoices.WATER_RATE}" which {

      lazy val document = getEvidencePage(Welsh, EvidenceChoices.WATER_RATE)

      s"has a title of $waterTitleTextWelsh" in {
        document.title() shouldBe s"$waterTitleTextWelsh"
      }

      s"has a panel containing a header of '$waterHeaderTextWelsh'" in {
        document.select(headerSelector).text shouldBe waterHeaderTextWelsh
      }

      s"has hint text on the screen of '$fileUploadTextWelsh'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadTextWelsh
      }

      s"has a choose file button" in {
        document.getElementById(chooseFileId).className() shouldBe "govuk-file-upload"
      }
    }

    "Show other evidence upload page in english" which {

      lazy val document = getEvidencePage(English, EvidenceChoices.OTHER)

      s"has a title of $otherEvidenceTitleText" in {
        document.title() shouldBe s"$otherEvidenceTitleText"
      }

      s"has a panel containing a header of '$otherEvidenceHeaderText'" in {
        document.select(headerSelector).text shouldBe otherEvidenceHeaderText
      }

      s"has hint text on the screen of '$otherEvidenceHintText'" in {
        document.getElementById("evidenceType-hint").text() shouldBe otherEvidenceHintText
      }

      s"has a choose file button '$otherEvidenceRadioOptions'" in {
        document.getElementById("upload-evidence-options").text() shouldBe otherEvidenceRadioOptions
      }
    }

    "Show other evidence upload page in welsh" which {

      lazy val document = getEvidencePage(Welsh, EvidenceChoices.OTHER)

      s"has a title of $otherEvidenceTitleTextWelsh" in {
        document.title() shouldBe s"$otherEvidenceTitleTextWelsh"
      }

      s"has a panel containing a header of '$otherEvidenceHeaderTextWelsh'" in {
        document.select(headerSelector).text shouldBe otherEvidenceHeaderTextWelsh
      }

      s"has hint text on the screen of '$otherEvidenceHintTextWelsh'" in {
        document.getElementById("evidenceType-hint").text() shouldBe otherEvidenceHintTextWelsh
      }

      s"has a choose file button '$otherEvidenceRadioOptionsWelsh'" in {
        document.getElementById("upload-evidence-options").text() shouldBe otherEvidenceRadioOptionsWelsh
      }
    }

    "direct too main upload page from continue button" which {

      "from other evidence upload page" in {
        val res = continueOtherEvidencePage(English, EvidenceChoices.OTHER, LandRegistryTitle)
        res.status shouldBe SEE_OTHER
      }

    }

  }

  private def getEvidencePage(language: Language, evidenceChoice: EvidenceChoices): Document = {
    val relationshipCapacity = Some(Occupier)
    val userIsAgent = false
    val propertyOwnership: Option[PropertyOwnership] = Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1)))
    val propertyOccupancy: Option[PropertyOccupancy] = Some(
      PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None))
    await(
      mockRepository.saveOrUpdate[LinkingSession](
        LinkingSession(
          address = "LS",
          uarn = 1L,
          submissionId = "PL-123456",
          personId = 1L,
          earliestStartDate = LocalDate.of(2017, 4, 1),
          propertyRelationship = relationshipCapacity.map(capacity => PropertyRelationship(capacity, 1L)),
          propertyOwnership = propertyOwnership,
          propertyOccupancy = propertyOccupancy,
          hasRatesBill = Some(true),
          uploadEvidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
          evidenceType = Some(RatesBillType),
          clientDetails = if (userIsAgent) Some(ClientDetails(100, "ABC")) else None,
          localAuthorityReference = "12341531531",
          rtp = ClaimPropertyReturnToPage.FMBR,
          fromCya = None,
          isSubmitted = None
        ))
    )

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
      post("/business-rates-attachments/initiate-upload")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(PreparedUpload(Reference("12345678910111213"), UploadFormTemplate("http://localhost/upscan", Map()))
          ).toString())
        }
    }

    val res = await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/evidence/$evidenceChoice/upload")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
  private def continueOtherEvidencePage(
        language: Language,
        evidenceChoice: EvidenceChoices,
        evidencetype: EvidenceType) = {
    val relationshipCapacity = Some(Occupier)
    val userIsAgent = false
    val propertyOwnership: Option[PropertyOwnership] = Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1)))
    val propertyOccupancy: Option[PropertyOccupancy] = Some(
      PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None))
    await(
      mockRepository.saveOrUpdate[LinkingSession](
        LinkingSession(
          address = "LS",
          uarn = 1L,
          submissionId = "PL-123456",
          personId = 1L,
          earliestStartDate = LocalDate.of(2017, 4, 1),
          propertyRelationship = relationshipCapacity.map(capacity => PropertyRelationship(capacity, 1L)),
          propertyOwnership = propertyOwnership,
          propertyOccupancy = propertyOccupancy,
          hasRatesBill = Some(true),
          uploadEvidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
          evidenceType = Some(RatesBillType),
          clientDetails = if (userIsAgent) Some(ClientDetails(100, "ABC")) else None,
          localAuthorityReference = "12341531531",
          rtp = ClaimPropertyReturnToPage.FMBR,
          fromCya = None,
          isSubmitted = None
        ))
    )

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
      post("/business-rates-attachments/initiate-upload")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(PreparedUpload(Reference("12345678910111213"), UploadFormTemplate("http://localhost/upscan", Map()))
          ).toString())
        }
    }

    await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/evidence/$evidenceChoice/upload")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(Json.obj("evidenceType" -> evidencetype.name))
    )
  }
}
