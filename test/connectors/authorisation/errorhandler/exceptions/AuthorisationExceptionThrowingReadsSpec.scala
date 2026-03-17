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

package connectors.authorisation.errorhandler.exceptions

import controllers.VoaPropertyLinkingSpec
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

class AuthorisationExceptionThrowingReadsSpec extends VoaPropertyLinkingSpec {
  trait Setup {

    val exceptionThrowingReads: AuthorisationExceptionThrowingReads =
      new AuthorisationExceptionThrowingReads {}

    final def httpResponse(
          status: Int,
          body: String = "",
          headers: Map[String, Seq[String]] = Map.empty
    ): HttpResponse =
      HttpResponse(
        status = status,
        body = body,
        headers = headers
      )

    val authorisationErrorResponseBody: String =
      """{ "errorCode": "INVALID_GATEWAY_SESSION"}"""
  }

  "when handling a 2xx http response" should "return the response" in new Setup {
    val response: HttpResponse =
      httpResponse(OK)

    val out =
      exceptionThrowingReads
        .authorisationReads[HttpResponse]
        .read("GET", "URL", response)

    out shouldBe response
  }

  "when handling a 401 http response" should "throw an AuthorisationFailure exception" in new Setup {
    val response: HttpResponse =
      httpResponse(UNAUTHORIZED, authorisationErrorResponseBody)

    intercept[AuthorisationFailure] {
      exceptionThrowingReads
        .authorisationReads[HttpResponse]
        .read("GET", "URL", response)
    }.getMessage shouldBe "INVALID_GATEWAY_SESSION"
  }

  "when handling other 4xx http response" should "throw an UpstreamErrorResponse (4xx)" in new Setup {
    val response: HttpResponse =
      httpResponse(BAD_REQUEST)

    intercept[UpstreamErrorResponse] {
      exceptionThrowingReads
        .authorisationReads[HttpResponse]
        .read("GET", "URL", response)
    } match {
      case err @ UpstreamErrorResponse.WithStatusCode(BAD_REQUEST) =>
        err.getMessage() should startWith("GET of 'URL' returned 400")
    }
  }

  "when handling a 5xx http response" should "throw an UpstreamErrorResponse (5xx)" in new Setup {
    val response: HttpResponse =
      httpResponse(INTERNAL_SERVER_ERROR)

    intercept[UpstreamErrorResponse] {
      exceptionThrowingReads
        .authorisationReads[HttpResponse]
        .read("GET", "URL", response)
    } match {
      case err @ UpstreamErrorResponse.WithStatusCode(INTERNAL_SERVER_ERROR) =>
        err.getMessage() should startWith("GET of 'URL' returned 500")
    }
  }
}
