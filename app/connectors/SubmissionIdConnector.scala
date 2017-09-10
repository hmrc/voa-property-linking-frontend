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

import com.google.inject.Inject
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.{ExecutionContext, Future}

class SubmissionIdConnector @Inject()(config: ServicesConfig, http: HttpGet)(implicit ec: ExecutionContext) {
  lazy val baseUrl: String = config.baseUrl("property-linking") + s"/property-linking"

  def get(prefix: String = "PL")(implicit hc: HeaderCarrier): Future[String] =
    http.GET[String](baseUrl + s"/submissionId/$prefix")
}
