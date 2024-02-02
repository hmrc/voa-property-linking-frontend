package controllers

import base.{HtmlComponentHelpers, ISpecBase}
import binders.propertylinks.EvidenceChoices.EvidenceChoices
import binders.propertylinks.{ClaimPropertyReturnToPage, EvidenceChoices}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.PropertyLinkingSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class UploadResultControllerISpec extends ISpecBase with HtmlComponentHelpers {

  //  TODO: Add in welsh translations
  lazy val mockRepository: PropertyLinkingSessionRepository = app.injector.instanceOf[PropertyLinkingSessionRepository]
  val testSessionId = s"stubbed-${UUID.randomUUID}"
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))
  val fileRef = "ad344c3c-0560-40be-842a-511b4b09b404"

  val rateTitleText = "Upload your business rates bill - Valuation Office Agency - GOV.UK"
  val rateHeaderText = "Upload your business rates bill"

  val captionText = "Add a property"
  val captionTextWelsh = ""

  val rateTitleTextWelsh = "Lanlwytho eich bil ardrethi busnes - Valuation Office Agency - GOV.UK"
  val rateHeaderTextWelsh = "Lanlwytho eich bil ardrethi busnes"

  val fileUploadTextWelsh = "Rhaid i’r ffeil fod yn ddogfen Word, taenlen Excel, PDF neu ddelwedd (PNG neu JPG) a bod yn llai na 10MB."
  val fileUploadText = "The selected file must be a Word document, Excel spreadsheet, PDF or image (PNG or JPG) and be smaller than 10MB."

  val uploadedStatusText = "uploaded"
  val uploadedStatusTextWelsh = ""

  val uploadingStatusText = "uploading"
  val uploadingStatusTextWelsh = ""

  val removeFileText = "Remove test-file.jpeg"
  val removeFileTextWelsh = ""

  val continueButton = "Continue"
  val continueButtonWelsh = ""

  val headerSelector = "h1"
  val fileUploadStatusSelector = "#main-content > div > div > dl > div > dd.govuk-summary-list__value > strong"
  val removeActionSelector = "#main-content > div > div > dl > div > dd.govuk-summary-list__actions > a"
  val fileNameSelector = "#main-content > div > div > dl > div > dt > a"


  "UploadResultController" should {
    s"Show uploaded file result page in english with ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getUpdateResultPage(English, EvidenceChoices.RATES_BILL)

      s"has a title of $rateTitleText" in {
        document.title() shouldBe s"$rateTitleText"
      }

      s"has a panel containing a header of '$rateHeaderText'" in {
        document.select(headerSelector).text shouldBe rateHeaderText
      }

      s"has hint text on the screen of '$fileUploadText'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadText
      }

      "has a file name" in {
        document.select(fileNameSelector).text shouldBe "test-file.jpeg"
      }

      "has a file upload status" in {
        document.select(fileUploadStatusSelector).text shouldBe uploadedStatusText
      }

      "has a remove file link" in {
        document.select(removeActionSelector).text shouldBe removeFileText
      }

      "has a continue button" in {
        document.getElementById("continue").text shouldBe continueButton
      }
    }

    s"Show uploaded file result page in welsh ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getUpdateResultPage(Welsh, EvidenceChoices.RATES_BILL, fileUploaded = false)

      s"has a title of $rateTitleTextWelsh" in {
        document.title() shouldBe s"$rateTitleTextWelsh"
      }

      s"has a panel containing a header of '$rateHeaderTextWelsh'" in {
        document.select(headerSelector).text shouldBe rateHeaderTextWelsh
      }

      s"has hint text on the screen of '$fileUploadTextWelsh'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadTextWelsh
      }

      "has a file name" in {
        document.select(fileNameSelector).text shouldBe "test-file.jpeg"
      }

      "has a file upload status" in {
        document.select(fileUploadStatusSelector).text shouldBe uploadedStatusTextWelsh
      }

      "has a remove file link" in {
        document.select(removeActionSelector).text shouldBe removeFileTextWelsh
      }

      "has a continue button" in {
        document.getElementById("continue").text shouldBe continueButton
      }
    }

    s"Show uploading file result page in english with ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getUpdateResultPage(English, EvidenceChoices.RATES_BILL, fileUploaded = false)

      s"has a title of $rateTitleText" in {
        document.title() shouldBe s"$rateTitleText"
      }

      s"has a panel containing a header of '$rateHeaderText'" in {
        document.select(headerSelector).text shouldBe rateHeaderText
      }

      s"has hint text on the screen of '$fileUploadText'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadText
      }
      "has a file upload status" in {
        document.select(fileUploadStatusSelector).text shouldBe uploadingStatusText
      }

      "has a remove file link" in {
        document.select(removeActionSelector).text shouldBe removeFileText
      }

      "has a continue button" in {
        document.getElementById("continue").text shouldBe continueButton
      }
    }

    s"Show uploading file result page in welsh ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getUpdateResultPage(Welsh, EvidenceChoices.RATES_BILL, fileUploaded = false)

      s"has a title of $rateTitleTextWelsh" in {
        document.title() shouldBe s"$rateTitleTextWelsh"
      }

      s"has a panel containing a header of '$rateHeaderTextWelsh'" in {
        document.select(headerSelector).text shouldBe rateHeaderTextWelsh
      }

      s"has hint text on the screen of '$fileUploadTextWelsh'" in {
        document.getElementsByClass("govuk-hint").text() shouldBe fileUploadTextWelsh
      }

      "has a file name" in {
        document.select(fileNameSelector).text shouldBe "test-file.jpeg"
      }

      "has a file upload status" in {
        document.select(fileUploadStatusSelector).text shouldBe uploadingStatusTextWelsh
      }

      "has a remove file link" in {
        document.select(removeActionSelector).text shouldBe removeFileTextWelsh
      }

      "has a continue button" in {
        document.getElementById("continue").text shouldBe continueButton
      }
    }
  }

  private def getUpdateResultPage(language: Language, evidenceChoice: EvidenceChoices, fileUploaded: Boolean = true): Document = {
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
          isSubmitted = None,
          fileReference = Some(fileRef)
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
    if(fileUploaded) fileUploadedStub else fileUploadingStub

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/evidence/$evidenceChoice/upload/result")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  def fileUploadedStub = {
    val attachmentJson = Json.parse(
      """{
        |   "_id":"81eb7217-49dd-486d-a5fe-b2fbb10731cf",
        |   "initiatedAt":"2024-01-22T16:39:29.136326Z",
        |   "fileName":"test-file.jpeg",
        |   "mimeType":"image/jpeg",
        |   "destination":"PROPERTY_LINK_EVIDENCE_DFE",
        |   "data":{
        |      "success_action_redirect":"http://localhost:9523/business-rates-property-linking/my-organisation/claim/property-links/evidence/RATES_BILL/upload/result?key=ad344c3c-0560-40be-842a-511b4b09b404",
        |      "x-amz-credential":"ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
        |      "x-amz-meta-upscan-initiate-response":"2024-01-22T16:39:24.962360Z",
        |      "x-amz-meta-original-filename":"${filename}",
        |      "x-amz-algorithm":"AWS4-HMAC-SHA256",
        |      "x-amz-signature":"xxxx",
        |      "error_action_redirect":"http://localhost:9523/business-rates-property-linking/my-organisation/claim/property-links/evidence/RATES_BILL/upload/clear",
        |      "x-amz-meta-session-id":"86c0e47a-c952-4752-aa06-fa1c0fbc8fd7",
        |      "x-amz-meta-callback-url":"http://localhost:9541/business-rates-attachments/callback",
        |      "x-amz-date":"20240122T163924Z",
        |      "x-amz-meta-upscan-initiate-received":"2024-01-22T16:39:24.962360Z",
        |      "x-amz-meta-request-id":"3832bdc6-56b3-45fc-85a0-60cf4d305bcc",
        |      "key":"ad344c3c-0560-40be-842a-511b4b09b404",
        |      "acl":"private",
        |      "x-amz-meta-consuming-service":"business-rates-attachments",
        |      "policy":"eyJjb25kaXRpb25zIjpbWyJjb250ZW50LWxlbmd0aC1yYW5nZSIsMSwxMDQ4NTc2MF1dfQ=="
        |   },
        |   "state":"ScanPending",
        |   "history":[
        |      {
        |         "state":"ScanPending",
        |         "timeStamp":"2024-01-22T16:39:29.211502Z"
        |      },
        |      {
        |         "state":"MetadataReceived",
        |         "timeStamp":"2024-01-22T16:39:29.211485Z"
        |      },
        |      {
        |         "state":"Initiated",
        |         "timeStamp":"2024-01-22T16:39:29.211474Z"
        |      },
        |      {
        |         "state":"Received",
        |         "timeStamp":"2024-01-22T16:39:29.136326Z"
        |      }
        |   ],
        |   "scanResult":{
        |      "reference":"ad344c3c-0560-40be-842a-511b4b09b404",
        |      "fileStatus":"READY",
        |      "downloadUrl":"http://localhost:9570/upscan/download/065c62a5-652f-44d3-8b81-1b9b5f196ca6",
        |      "uploadDetails":{
        |         "fileName":"1c404f7a05d7e747435287f6481766fb2dc25c53-2023-07-14-16-08-16-347838.jpeg",
        |         "fileMimeType":"image/jpeg",
        |         "uploadTimestamp":"2024-01-22T16:39:29.136326Z",
        |         "checksum":"fdf8cfbd7dab843c04a5c9d6624b823fb9cd0f0b9532eb325d1ee7611cb2a797",
        |         "size":2088935
        |      }
        |   },
        |   "principal":{
        |      "externalId":"Ext-c4d0ec3a-4c37-488c-9cd3-8afcf51bde58",
        |      "groupId":"ip-group-1"
        |   }
        |}""".stripMargin)

    stubFor {
      get(s"/business-rates-attachments/attachments/$fileRef")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(attachmentJson).toString())
        }
    }
  }

  def fileUploadingStub = {
    stubFor {
      get(s"/business-rates-attachments/attachments/$fileRef")
        .willReturn {
          aResponse.withStatus(EXPECTATION_FAILED)
        }
    }
  }
}

