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

import controllers.Account
import serialization.JsonFormats._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class AccountConnector(http: HttpGet with HttpPut with HttpPost)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("property-representations") + s"/property-linking"

  implicit val rds: HttpReads[Unit] = new HttpReads[Unit] {
    override def read(method: String, url: String, response: HttpResponse): Unit = Unit
  }

  def get()(implicit hc: HeaderCarrier): Future[Seq[Account]] = {
    val url = baseUrl + s"/accounts"
    http.GET[Seq[Account]](url)
  }

  def get(accountId: String)(implicit hc: HeaderCarrier): Future[Option[Account]] = {
    val url = baseUrl + s"/accounts"
    http.GET[Seq[Account]](url).map(_.find(_.companyName == accountId))
  }

  def create(account: Account)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/accounts"
    http.POST[Account, Unit](url, account)
  }
}

