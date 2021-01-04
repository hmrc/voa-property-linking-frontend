/*
 * Copyright 2021 HM Revenue & Customs
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

import controllers.VoaPropertyLinkingSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.http.Status.{OK => _}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

class FileUploadControllerSpec extends VoaPropertyLinkingSpec {
  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  lazy val mockBusinessRatesAttachmentService = mock[BusinessRatesAttachmentsService]
  def controller() = TestFileUploadController

  //TODO write these tests.

  implicit lazy val request = FakeRequest().withSession(token).withHeaders(HOST -> "localhost:9523")

  implicit lazy val hc = HeaderCarrier()

  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)

  object TestFileUploadController
      extends UploadController(
        mockCustomErrorHandler,
        preAuthenticatedActionBuilders(),
        withLinkingSession,
        mockBusinessRatesAttachmentService)

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    when(f.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }

}
