/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.play.config.inject.ServicesConfig
import config.WSHttp
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, NotFoundException }

class BusinessRatesValuationConnector @Inject()(config: ServicesConfig, http: WSHttp) {
  val url = config.baseUrl("business-rates-valuation")

  def isViewable(authorisationId: Long, assessmentRef: Long)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.GET[HttpResponse](s"$url/property-link/$authorisationId/assessment/$assessmentRef") map { _ => true } recover { case _: NotFoundException => false }
  }
}
