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

package handlers

import config.ApplicationConfig
import controllers.VoaPropertyLinkingSpec
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import utils.Configs
import java.time.LocalDateTime

class CustomErrorHandlerSpec extends VoaPropertyLinkingSpec {

  implicit val appConfig: ApplicationConfig = Configs.applicationConfig
  implicit val hc = HeaderCarrier()

  val testErrorHandler =
    new CustomErrorHandler(
      errorView,
      forbiddenView,
      technicalDifficultiesView,
      notFoundView,
      alreadySubmittedView,
      clock)

  "standardErrorTemplate" should
    "display the standard error page with the given page title, heading and message" in {
    val result = testErrorHandler.standardErrorTemplate("Test title", "Test heading", "Test message")(FakeRequest())

    result shouldBe errorView("Test title", "Test heading", "Test message")(
      FakeRequest(),
      messagesApi.preferred(FakeRequest()),
      appConfig)
  }

  "internalServerErrorTemplate" should
    "display the technical difficulties page with the error reference and time" in {
    val result = testErrorHandler.internalServerErrorTemplate(
      FakeRequest()
        .withHeaders(HeaderNames.xRequestId -> "govuk-tax-253f442d-1bb2-4d3c-9943-248f5d96a812"))

    result shouldBe technicalDifficultiesView(Some("253f442d-1bb2-4d3c-9943-248f5d96a812"), LocalDateTime.now(clock))(
      FakeRequest(),
      messagesApi.preferred(FakeRequest()),
      appConfig)
  }

  "internalServerErrorTemplate" should
    "display the technical difficulties page with the error reference and time with new requestId format" in {
    val result = testErrorHandler.internalServerErrorTemplate(
      FakeRequest()
        .withHeaders(HeaderNames.xRequestId -> "05HyDTzoaHs5NmHMyzggyG-s3cURERsPiWvS6oD5XRVA9KGtYWzGkQ=="))

    result shouldBe technicalDifficultiesView(
      Some("05HyDTzoaHs5NmHMyzggyG-s3cURERsPiWvS6oD5XRVA9KGtYWzGkQ=="),
      LocalDateTime.now(clock))(FakeRequest(), messagesApi.preferred(FakeRequest()), appConfig)
  }
}
