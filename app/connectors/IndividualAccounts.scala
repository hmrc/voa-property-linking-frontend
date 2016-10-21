/*
 * Copyright 2016 HM Revenue & Customs
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

import models.IndividualAccount
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class IndividualAccounts(http: HttpGet with HttpPut with HttpPost)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("property-representations") + s"/property-linking/individuals"

  def get()(implicit hc: HeaderCarrier): Future[Seq[IndividualAccount]] = {
    http.GET[Seq[IndividualAccount]](baseUrl)
  }

  def get(accountId: String)(implicit hc: HeaderCarrier): Future[Option[IndividualAccount]] = {
    http.GET[Option[IndividualAccount]](s"$baseUrl/$accountId")
  }

  def create(account: IndividualAccount)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST[IndividualAccount, HttpResponse](baseUrl, account) map { _ => () }
  }
}

