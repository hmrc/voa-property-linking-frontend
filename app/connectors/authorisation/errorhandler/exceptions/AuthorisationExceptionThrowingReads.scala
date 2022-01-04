/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.Logging
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.json.{Json, OFormat, Reads}
import uk.gov.hmrc.http.{HttpReads, HttpReadsInstances, HttpResponse, UpstreamErrorResponse}
import utils.Cats._

import scala.util.Try

trait AuthorisationExceptionThrowingReads extends Logging {

  implicit def authorisationReads[A](implicit hrds: HttpReads[A]): HttpReads[A] = {

    def mapToException(method: String, url: String, response: HttpResponse)(e: UpstreamErrorResponse): A = {

      logger.debug(s"AuthorisationExceptionThrowingReads: $method $url ${response.body}")

      def authErrorMessage(response: HttpResponse)(r: Reads[UnauthorisedErrorResponse]): Option[String] =
        for {
          jsError <- Try(Json.parse(response.body)).toOption
          error   <- r.reads(jsError).asOpt
        } yield error.errorCode

      e.statusCode match {
        case UNAUTHORIZED =>
          throw AuthorisationFailure(
            authErrorMessage(response)(Json.reads[UnauthorisedErrorResponse]).getOrElse("Unexpected error."))
        case _ => throw e
      }
    }

    HttpReads.ask.flatMap {
      case (method, url, response) =>
        HttpReadsInstances.readEitherOf[A].map(_.leftMap(mapToException(method, url, response)).merge)
    }
  }
}

/***
  * Business-Rates-Authentication error response
  * e.g.
  * 401 Unauthorised
  * {"errorCode":"INVALID_GATEWAY_SESSION"}
  */
case class UnauthorisedErrorResponse(errorCode: String)

object UnauthorisedErrorResponse {
  implicit val format: OFormat[UnauthorisedErrorResponse] = Json.format
}
