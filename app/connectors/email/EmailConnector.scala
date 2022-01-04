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

package connectors.email

import javax.inject.Inject
import models.email.EmailRequest
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class EmailConnector @Inject()(config: ServicesConfig, http: HttpClient) {

  private val serviceUrl = config.baseUrl("email")

  def send(emailRequest: EmailRequest)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Unit] =
    http
      .POST[EmailRequest, HttpResponse](s"$serviceUrl/hmrc/email", emailRequest)
      .map(_ => ())

}
