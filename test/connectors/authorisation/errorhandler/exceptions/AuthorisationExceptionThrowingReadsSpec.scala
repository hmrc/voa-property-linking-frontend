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

package connectors.authorisation.errorhandler.exceptions

import controllers.VoaPropertyLinkingSpec
import org.mockito.Mockito.when
import org.scalatest.Matchers.convertToAnyShouldWrapper
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

class AuthorisationExceptionThrowingReadsSpec extends VoaPropertyLinkingSpec {
  trait Setup {

    val exceptionThrowingReads: AuthorisationExceptionThrowingReads = new AuthorisationExceptionThrowingReads {}

    val mockHttpResponse: HttpResponse = mock[HttpResponse]

    val authorisationErrorResponseBody: String = """{ "errorCode": "INVALID_GATEWAY_SESSION"}"""
  }

  "when handling a 2xx http response" must "return the response" in new Setup {
    when(mockHttpResponse.status).thenReturn(OK)

    val response: HttpResponse =
      exceptionThrowingReads.authorisationReads[HttpResponse].read("GET", "URL", mockHttpResponse)
    response mustBe mockHttpResponse
  }

  "when handling a 401 http response" must "throw an AuthorisationFailure exception" in new Setup {
    when(mockHttpResponse.status).thenReturn(UNAUTHORIZED)
    when(mockHttpResponse.body).thenReturn(authorisationErrorResponseBody)

    intercept[AuthorisationFailure] {
      exceptionThrowingReads.authorisationReads[HttpResponse].read("GET", "URL", mockHttpResponse)
    }.getMessage mustBe "INVALID_GATEWAY_SESSION"
  }

  "when handling other 4xx http response" must "throw an UpstreamErrorResponse (4xx)" in new Setup {
    when(mockHttpResponse.status).thenReturn(BAD_REQUEST)

    intercept[UpstreamErrorResponse] {
      exceptionThrowingReads.authorisationReads[HttpResponse].read("GET", "URL", mockHttpResponse)
    } match {
      case UpstreamErrorResponse.WithStatusCode(BAD_REQUEST, err) =>
        err.getMessage() should startWith("GET of 'URL' returned 400")
    }
  }

  "when handling a 5xx http response" must "throw an UpstreamErrorResponse (5xx)" in new Setup {
    when(mockHttpResponse.status).thenReturn(INTERNAL_SERVER_ERROR)

    intercept[UpstreamErrorResponse] {
      exceptionThrowingReads.authorisationReads[HttpResponse].read("GET", "URL", mockHttpResponse)
    } match {
      case UpstreamErrorResponse.WithStatusCode(INTERNAL_SERVER_ERROR, err) =>
        err.getMessage() should startWith("GET of 'URL' returned 500")
    }
  }

}
