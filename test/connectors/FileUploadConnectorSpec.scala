/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors

import config.WSHttp
import connectors.fileUpload.{FileMetadata, FileUploadConnector}
import controllers.ControllerSpec
import models.{FileInfo, NoEvidenceFlag, RatesBillFlag, RatesBillType}
import play.api.libs.ws.WSClient
import resources._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => matching, _}
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.StubServicesConfig
import uk.gov.hmrc.http.HeaderCarrier

class FileUploadConnectorSpec extends ControllerSpec {

  "Retrieving a file's metadata" must "return the filename and evidence type if a file has been uploaded" in {
    val envelopeId: String = shortString
    when(mockHttp.GET[JsValue](matching(s"http://localhost:8898/file-upload/envelopes/$envelopeId"))(any(), any[HeaderCarrier], any())).thenReturn(Future.successful(fileUploadedMetadata))

    val data = await(testConnector.getFileMetadata(envelopeId)(HeaderCarrier()))
    data mustBe FileMetadata(RatesBillFlag, Some(FileInfo("downloadfile.PDF", RatesBillType)))
  }

  it must "return no filename or evidence type if the user does not have evidence" in {
    val envelopeId: String = shortString
    when(mockHttp.GET[JsValue](matching(s"http://localhost:8898/file-upload/envelopes/$envelopeId"))(any(), any[HeaderCarrier], any())).thenReturn(Future.successful(noEvidenceMetadata))

    val data = await(testConnector.getFileMetadata(envelopeId)(HeaderCarrier()))
    data mustBe FileMetadata(NoEvidenceFlag, None)
  }

  it must "return no filename or evidence type if the user has not uploaded a file" in {
    //FUaaS does not validate that the POST request actually contains a file
    val envelopeId: String = shortString
    when(mockHttp.GET[JsValue](matching(s"http://localhost:8898/file-upload/envelopes/$envelopeId"))(any(), any[HeaderCarrier], any())).thenReturn(Future.successful(noFileUploadedMetadata))

    val data = await(testConnector.getFileMetadata(envelopeId)(HeaderCarrier()))
    data mustBe FileMetadata(NoEvidenceFlag, None)
  }

  lazy val mockHttp = mock[WSHttp]

  lazy val testConnector = new FileUploadConnector(StubServicesConfig, mockHttp)

  lazy val fileUploadedMetadata = Json.parse {
    """{
      |    "id": "d1cb6432-8297-4d36-813b-cf076a8edc42",
      |    "metadata": {
      |        "submissionId": "PL1ZRPR2Z",
      |        "personId": 374017877
      |    },
      |    "constraints": {
      |        "maxItems": 100,
      |        "maxSize": "10MB",
      |        "maxSizePerItem": "10MB",
      |        "contentTypes": [
      |            "application/pdf",
      |            "image/jpeg"
      |        ]
      |    },
      |    "status": "OPEN",
      |    "files": [
      |        {
      |            "id": "3d3e32e4-6c4b-443b-81ff-de096917ad58",
      |            "status": "AVAILABLE",
      |            "name": "downloadfile.PDF",
      |            "contentType": "application/pdf",
      |            "length": 81860,
      |            "created": "2017-07-20T12:38:26Z",
      |            "metadata": {
      |                "evidenceType": [
      |                    "ratesBill"
      |                ],
      |                "csrfToken": [
      |                    "736833dddf16f037ca9ef9f64be6937f53051b37-1500554293452-02aa9be7ba3a77b348957416"
      |                ]
      |            },
      |            "href": "/file-upload/envelopes/d1cb6432-8297-4d36-813b-cf076a8edc42/files/3d3e32e4-6c4b-443b-81ff-de096917ad58/content"
      |        }
      |    ]
      |}""".stripMargin
  }

  lazy val noEvidenceMetadata = Json.parse {
    """{
      |    "id": "2318e360-15c3-4bf5-9fd6-65d0ad23361e",
      |    "metadata": {
      |        "submissionId": "PL1ZRPR3N",
      |        "personId": 374017877
      |    },
      |    "constraints": {
      |        "maxItems": 100,
      |        "maxSize": "10MB",
      |        "maxSizePerItem": "10MB",
      |        "contentTypes": [
      |            "application/pdf",
      |            "image/jpeg"
      |        ]
      |    },
      |    "status": "OPEN"
      |}""".stripMargin
  }

  lazy val noFileUploadedMetadata = Json.parse {
    """{
      |    "id": "c41f93e2-1747-4674-8f2a-03f97ed6d0cd",
      |    "metadata": {
      |        "submissionId": "PL1ZRPR3W",
      |        "personId": 374017877
      |    },
      |    "constraints": {
      |        "maxItems": 100,
      |        "maxSize": "10MB",
      |        "maxSizePerItem": "10MB",
      |        "contentTypes": [
      |            "application/pdf",
      |            "image/jpeg"
      |        ]
      |    },
      |    "status": "OPEN",
      |    "files": [
      |        {
      |            "id": "44375998-fb35-4a24-b9cf-babc90e0648e",
      |            "status": "AVAILABLE",
      |            "name": "",
      |            "contentType": "application/octet-stream",
      |            "length": 0,
      |            "created": "2017-07-21T14:22:14Z",
      |            "metadata": {
      |                "evidenceType": [
      |                    "ratesBill"
      |                ],
      |                "csrfToken": [
      |                    "edd8b50b97e8cd029686516ea6ffd6c589a233ea-1500646927707-e258ec4620f5cdf7bcae0289"
      |                ]
      |            },
      |            "href": "/file-upload/envelopes/c41f93e2-1747-4674-8f2a-03f97ed6d0cd/files/44375998-fb35-4a24-b9cf-babc90e0648e/content"
      |        }
      |    ]
      |}""".stripMargin
  }
}
