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

import javax.inject.Inject

import models.DraftCase
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class DraftCases @Inject()(http: WSHttp, config: ServicesConfig)(implicit ec: ExecutionContext) {
  lazy val checkUrl = config.baseUrl("business-rates-check")

  def get(personId: Long)(implicit hc: HeaderCarrier): Future[Seq[DraftCase]] = {
    http.GET[Seq[DraftCase]](s"$checkUrl/partial-check/resume/person/$personId")
  }

}
