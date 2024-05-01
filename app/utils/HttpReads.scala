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

package utils

import exceptions.ConnectorException
import play.api.Logging
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.voa.businessrates.values.connectors.RequestResult.RequestFailure

object HttpReads extends Logging {

  def createReads(okStatus: Int, errorMap: Map[Int, RequestFailure]): HttpReads[Either[RequestFailure, HttpResponse]] =
    (method: String, url: String, response: HttpResponse) =>
      response.status match {
        case status if status == okStatus        => Right(response)
        case status if errorMap.contains(status) => Left(errorMap(status))
        case unexpectedStatus =>
          logger.warn(s"[$method $url] failed: status: $unexpectedStatus, body: ${response.body}")
          throw ConnectorException(method, url, response)
    }

}
