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

package controllers

import base.{HtmlComponentHelpers, ISpecBase}
import binders.propertylinks.EvidenceChoices.EvidenceChoices
import binders.propertylinks.{ClaimPropertyReturnToPage, EvidenceChoices}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models._
import models.upscan.{FailureReason, FileStatus}
import models.upscan.FileStatus.{FAILED, FileStatus, READY, UPLOADING}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.json.{JsNull, Json}
import play.api.test.Helpers._
import repositories.PropertyLinkingSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class UploadResultControllerISpec extends ISpecBase with HtmlComponentHelpers {

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

  val fileUploadTextWelsh =
    "Rhaid i’r ffeil a ddewiswyd fod yn ddogfen Word, taenlen Excel, PDF neu ddelwedd (PNG neu JPG) ac yn llai na 10MB."
  val fileUploadText =
    "The selected file must be a Word document, Excel spreadsheet, PDF or image (PNG or JPG) and be smaller than 10MB."

  val uploadedStatusText = "uploaded"
  val uploadedStatusTextWelsh = "Wedi ei lanlwytho"

  val uploadingStatusText = "uploading"
  val uploadingStatusTextWelsh = "Wrthi’n lanlwytho"

  val failedStatusText = "failed"
  val failedStatusTextWelsh = "methu"

  val removeFileText = "Remove"
  val removeFileTextWelsh = "Dileu"

  val continueButton = "Continue"
  val continueButtonWelsh = "Parhau"

  val headerSelector = "h1"
  val fileUploadStatusSelector = "#main-content > div > div > dl > div > dd.govuk-summary-list__value > strong"
  val removeActionSelector = "#main-content > div > div > dl > div > dd.govuk-summary-list__actions > a"
  val fileNameSelector = "#main-content > div > div > dl > div > dt > a"

  "UploadResultController show" should {
    s"Show uploaded file status result page in english with ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getUploadResultPage(English, EvidenceChoices.RATES_BILL)

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
        document.select(removeActionSelector).text shouldBe "Remove test-file.jpeg"
      }

      "has a continue button" in {
        document.getElementById("continue").text shouldBe continueButton
      }
    }

    s"Show uploaded file status result page in welsh ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getUploadResultPage(Welsh, EvidenceChoices.RATES_BILL)

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
        document.select(removeActionSelector).text shouldBe "Dileu test-file.jpeg"
      }

      "has a continue button" in {
        document.getElementById("continue").text shouldBe continueButtonWelsh
      }
    }

    s"Show uploading file status result page in english with ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getUploadResultPage(English, EvidenceChoices.RATES_BILL, fileStatus = UPLOADING)

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

    s"Show uploading file status result page in welsh ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getUploadResultPage(Welsh, EvidenceChoices.RATES_BILL, fileStatus = UPLOADING)

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
        document.select(fileNameSelector).text shouldBe ""
      }

      "has a file upload status" in {
        document.select(fileUploadStatusSelector).text shouldBe uploadingStatusTextWelsh
      }

      "has a remove file link" in {
        document.select(removeActionSelector).text shouldBe removeFileTextWelsh
      }

      "has a continue button" in {
        document.getElementById("continue").text shouldBe continueButtonWelsh
      }
    }

    s"Show failed file status result page in english with ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getUploadResultPage(English, EvidenceChoices.RATES_BILL, fileStatus = FAILED)

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
        document.select(fileUploadStatusSelector).text shouldBe failedStatusText
      }

      "has a remove file link" in {
        document.select(removeActionSelector).text shouldBe "Remove test-file.jpeg"
      }

      "has a continue button" in {
        document.getElementById("continue").text shouldBe continueButton
      }
    }

    s"Show failed file status result page in welsh ${EvidenceChoices.RATES_BILL}" which {

      lazy val document = getUploadResultPage(Welsh, EvidenceChoices.RATES_BILL, fileStatus = FAILED)

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
        document.select(fileNameSelector).text shouldBe ""
      }

      "has a file upload status" in {
        document.select(fileUploadStatusSelector).text shouldBe failedStatusTextWelsh
      }

      "has a remove file link" in {
        document.select(removeActionSelector).text shouldBe "Dileu test-file.jpeg"
      }

      "has a continue button" in {
        document.getElementById("continue").text shouldBe continueButtonWelsh
      }
    }
  }

  "UploadResultController onSubmit" should {
    "Redirect to the DeclarationController when the file status is uploaded" in {
      val result = submitUploadResultPage(EvidenceChoices.RATES_BILL, READY)
      result.status shouldBe SEE_OTHER
      result
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/summary"
    }

    "Redirect to the UploadResultController show when the file status is uploading" in {
      val result = submitUploadResultPage(EvidenceChoices.RATES_BILL, UPLOADING)
      result.status shouldBe SEE_OTHER
      result
        .headers("Location")
        .head shouldBe s"/business-rates-property-linking/my-organisation/claim/property-links/evidence/RATES_BILL/upload/result"
    }

    "Redirect to the UploadResultController show when the file status is failed with reason 'QUARANTINED'" in {
      val result = submitUploadResultPage(EvidenceChoices.RATES_BILL, FAILED, Some(FailureReason.QUARANTINED.toString))
      result.status shouldBe SEE_OTHER
      result
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/evidence/RATES_BILL/upload?errorCode=QUARANTINE"
    }

    "Redirect to the UploadResultController show when the file status is failed with reason 'REJECTED'" in {
      val result = submitUploadResultPage(EvidenceChoices.RATES_BILL, FAILED, Some(FailureReason.REJECTED.toString))
      result.status shouldBe SEE_OTHER
      result
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/evidence/RATES_BILL/upload?errorCode=REJECTED"
    }

    "Redirect to the UploadResultController show when the file status is failed with reason 'UNKNOWN'" in {
      val result = submitUploadResultPage(EvidenceChoices.RATES_BILL, FAILED, Some(FailureReason.UNKNOWN.toString))
      result.status shouldBe SEE_OTHER
      result
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/evidence/RATES_BILL/upload?errorCode=UNKNOWN"
    }
  }

  "UploadResultController getUploadStatus" should {

    "Redirect to the DeclarationController when the file status is uploaded" in {
      val result = getUploadStatus(READY)
      result.status shouldBe OK
      result.body shouldBe Json
        .toJson(
          FileStatus.READY,
          "test-file.jpeg",
          "http://localhost:9570/upscan/download/3a0b10d9-a18b-4ed2-b67b-64861453f131"
        )
        .toString
    }

    "Redirect to the UploadResultController show when the file status is uploading" in {
      val result = getUploadStatus(UPLOADING)
      result.status shouldBe OK
      result.body shouldBe Json.toJson(FileStatus.UPLOADING).toString
    }

    "Redirect to the UploadResultController show when the file status is failed" in {
      val result = getUploadStatus(FAILED)
      result.body shouldBe Json.toJson(FileStatus.FAILED, "test-file.jpeg", JsNull).toString
      result.status shouldBe OK
    }
  }

  private def getUploadResultPage(
        language: Language,
        evidenceChoice: EvidenceChoices,
        fileStatus: FileStatus = READY
  ): Document = {
    commonStubs
    fileStatus match {
      case READY     => fileUploadedStub
      case UPLOADING => fileUploadingStub
      case FAILED    => fileUploadFailedStub()
    }

    val res = await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/evidence/$evidenceChoice/upload/result"
        )
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def submitUploadResultPage(
        evidenceChoice: EvidenceChoices,
        fileStatus: FileStatus = READY,
        failureReason: Option[String] = None
  ) = {
    commonStubs
    fileStatus match {
      case READY     => fileUploadedStub
      case UPLOADING => fileUploadingStub
      case FAILED    => failureReason.map(reason => fileUploadFailedStub(reason))
    }

    val res = await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/evidence/$evidenceChoice/upload/result/$fileStatus"
        )
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = "")
    )
    res
  }

  private def getUploadStatus(fileStatus: FileStatus) = {
    commonStubs
    fileStatus match {
      case READY     => fileUploadedStub
      case UPLOADING => fileUploadingStub
      case FAILED    => fileUploadFailedStub()
    }

    val res = await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/evidence/scan-status"
        )
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )
    res
  }

  def fileUploadedStub = {
    val attachmentJson = Json.parse(
      """{
        |    "_id" : "81eb7217-49dd-486d-a5fe-b2fbb10731cf",
        |    "initiatedAt" : "2024-02-15T08:48:07.878802Z",
        |    "fileName" : "test-file.jpeg",
        |    "mimeType" : "image/jpeg",
        |    "destination" : "PROPERTY_LINK_EVIDENCE_DFE",
        |    "data" : {},
        |    "state" : "MetadataPending",
        |    "history" : [
        |        {
        |            "state" : "MetadataPending",
        |            "timeStamp" : "2024-02-15T08:48:08.057375Z"
        |        },
        |        {
        |            "state" : "Initiated",
        |            "timeStamp" : "2024-02-15T08:48:08.057344Z"
        |        },
        |        {
        |            "state" : "Received",
        |            "timeStamp" : "2024-02-15T08:48:07.878802Z"
        |        }
        |    ],
        |    "scanResult" : {
        |        "reference" : "81eb7217-49dd-486d-a5fe-b2fbb10731cf",
        |        "fileStatus" : "READY",
        |        "downloadUrl" : "http://localhost:9570/upscan/download/3a0b10d9-a18b-4ed2-b67b-64861453f131",
        |        "uploadDetails" : {
        |            "fileName" : "test-file.jpeg",
        |            "fileMimeType" : "image/jpeg",
        |            "uploadTimestamp" : "2024-02-15T08:48:07.878802Z",
        |            "checksum" : "f2126624bc967534587a5baa3d742d03fb31201a1b5bf004fabab7db00f7447c",
        |            "size" : 202526
        |        }
        |    },
        |    "initiateResult" : {
        |        "reference" : "81eb7217-49dd-486d-a5fe-b2fbb10731cf",
        |        "uploadRequest" : {
        |            "href" : "http://localhost:9570/upscan/upload-proxy",
        |            "fields" : {
        |                "success_action_redirect" : "http://localhost:9523/business-rates-property-linking/my-organisation/claim/property-links/evidence/RATES_BILL/upload/result?key=5bc828e0-8904-40fe-aff9-b01b630a5b73",
        |                "x-amz-credential" : "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
        |                "x-amz-meta-upscan-initiate-response" : "2024-02-15T08:48:02.763797Z",
        |                "x-amz-meta-original-filename" : "${filename}",
        |                "x-amz-algorithm" : "AWS4-HMAC-SHA256",
        |                "x-amz-signature" : "xxxx",
        |                "error_action_redirect" : "http://localhost:9523/business-rates-property-linking/my-organisation/claim/property-links/evidence/RATES_BILL/upload/clear",
        |                "x-amz-meta-session-id" : "81eb7217-49dd-486d-a5fe-b2fbb10731cf",
        |                "x-amz-meta-callback-url" : "http://localhost:9541/business-rates-attachments/callback",
        |                "x-amz-date" : "20240215T084802Z",
        |                "x-amz-meta-upscan-initiate-received" : "2024-02-15T08:48:02.763797Z",
        |                "x-amz-meta-request-id" : "3a9a8d8e-3b59-4c02-9de9-bccfd3c348aa",
        |                "key" : "5bc828e0-8904-40fe-aff9-b01b630a5b73",
        |                "acl" : "private",
        |                "x-amz-meta-consuming-service" : "business-rates-attachments",
        |                "policy" : "eyJjb25kaXRpb25zIjpbWyJjb250ZW50LWxlbmd0aC1yYW5nZSIsMSwxMDQ4NTc2MF1dfQ=="
        |            }
        |        }
        |    },
        |    "principal" : {
        |        "externalId" : "Ext-c4d0ec3a-4c37-488c-9cd3-8afcf51bde58",
        |        "groupId" : "ip-group-1"
        |    }
        |}""".stripMargin
    )

    stubFor {
      get(s"/business-rates-attachments/attachments/$fileRef")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(attachmentJson).toString())
        }
    }
  }

  def fileUploadFailedStub(failureReason: String = "QUARANTINE") = {
    val attachmentJson = Json.parse(s"""{
                                       |   "_id":"81eb7217-49dd-486d-a5fe-b2fbb10731cf",
                                       |   "initiatedAt":"2024-01-22T16:39:29.136326Z",
                                       |   "fileName":"test-file.jpeg",
                                       |   "mimeType":"image/jpeg",
                                       |   "destination":"PROPERTY_LINK_EVIDENCE_DFE",
                                       |   "data":{
                                       |      "success_action_redirect":"http://localhost:9523/business-rates-property-linking/my-organisation/claim/property-links/evidence/RATES_BILL/upload/result?key=ad344c3c-0560-40be-842a-511b4b09b404",
                                       |      "x-amz-credential":"ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
                                       |      "x-amz-meta-upscan-initiate-response":"2024-01-22T16:39:24.962360Z",
                                       |      "x-amz-meta-original-filename":"filename",
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
                                       |      "fileStatus" : "FAILED",
                                       |      "failureDetails": {
                                       |        "failureReason": "$failureReason",
                                       |        "message": "e.g. This file has a virus"
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

  def fileUploadingStub =
    stubFor {
      get(s"/business-rates-attachments/attachments/$fileRef")
        .willReturn {
          aResponse.withStatus(EXPECTATION_FAILED)
        }
    }

  def commonStubs = {
    val relationshipCapacity = Some(Occupier)
    val userIsAgent = false
    val propertyOwnership: Option[PropertyOwnership] = Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1)))
    val propertyOccupancy: Option[PropertyOccupancy] = Some(
      PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None)
    )
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
        )
      )
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
  }
}
