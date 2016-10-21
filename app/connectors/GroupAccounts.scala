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

import controllers.GroupAccountDetails
import models.GroupAccount
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class GroupAccounts(http: HttpGet with HttpPost)(implicit ec: ExecutionContext) extends ServicesConfig {

  lazy val url = baseUrl("property-representations") + "/property-linking/groups"

  def get()(implicit hc: HeaderCarrier): Future[Seq[GroupAccount]] = {
    http.GET[Seq[GroupAccount]](url)
  }

  def get(groupId: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[GroupAccount]](s"$url/$groupId")
  }

  def create(account: GroupAccount)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST[GroupAccount, HttpResponse](url, account) map { _ => () }
  }

  def create(groupId: String, details: GroupAccountDetails)(implicit hc: HeaderCarrier): Future[Unit] = {
    create(GroupAccount(groupId, details.companyName, details.address, details.email, details.phone, details.isSmallBusiness, details.isAgent))
  }
}
