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

package connectors.errorhandler.exceptions

import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http._
import utils.Cats._

/****
  * This reads is designed to maintain behaviour of error handling
  * after the bootstrap-frontend-play26 upgrade
  *
  * Before the upgrade, 400 and 404 responses would throw exceptions:
  * (BadRequestException and NotFoundException)
  *
  * After the upgrade, no exceptions are thrown (by the library HttpErrorFunctions).
  *
  * This custom HttpReads should therefore recognise 400 and 404 http responses
  * and throw BadRequestException or NotFoundException
  *
  * TODO: this may be a temporary measure (tbd)
  */
trait ExceptionThrowingReads extends HttpErrorFunctions with Logging {

  def exceptionThrowingReads[A](implicit hrds: HttpReads[A]): HttpReads[A] = {

    def mapToException(method: String, url: String, response: HttpResponse)(e: UpstreamErrorResponse): A = {

      logger.debug(s"ExceptionThrowingReads: $method $url ${response.body}")

      e.statusCode match {
        case BAD_REQUEST =>
          throw new BadRequestException(badRequestMessage(method, url, response.body))
        case NOT_FOUND =>
          throw new NotFoundException(notFoundMessage(method, url, response.body))
        case _ => throw e
      }
    }

    HttpReads.ask.flatMap {
      case (method, url, response) =>
        HttpReadsInstances.readEitherOf[A].map(_.leftMap(mapToException(method, url, response)).merge)
    }
  }
}

object ExceptionThrowingReadsInstances extends HttpReadsInstances with ExceptionThrowingReads {
  override implicit val readRaw: HttpReads[HttpResponse] = exceptionThrowingReads(HttpReadsInstances.readRaw)
}
