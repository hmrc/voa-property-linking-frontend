/*
 * Copyright 2018 HM Revenue & Customs
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
import controllers.GroupAccountDetails
import models._
import play.api.libs.json.{JsDefined, JsNumber, JsValue}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import config.WSHttp
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class GroupAccounts @Inject()(config: ServicesConfig, http: WSHttp)(implicit ec: ExecutionContext) {

  lazy val url = config.baseUrl("property-linking") + "/property-linking/groups"

  def get(organisationId: Long)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[GroupAccount]](s"$url/$organisationId")
  }

  def withGroupId(groupId: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[GroupAccount]](s"$url?groupId=$groupId")
  }

  def withAgentCode(agentCode: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[GroupAccount]](s"$url/agentCode/$agentCode")
  }

  def create(account: GroupAccountSubmission)(implicit hc: HeaderCarrier): Future[Long] = {
    http.POST[GroupAccountSubmission, JsValue](url, account) map { js => js \ "id" match {
      case JsDefined(JsNumber(id)) => id.toLong
      case _ => throw new Exception(s"Invalid id $js")
    }}
  }

  def create(groupId: String, addressId: Int, details: GroupAccountDetails,
             individualAccountSubmission: IndividualAccountSubmission)
            (implicit hc: HeaderCarrier): Future[Long] = {
    create(GroupAccountSubmission(
      groupId, details.companyName, addressId, details.email, details.phone, details.isAgent, individualAccountSubmission
    ))
  }

  def update(orgId: Long, details: UpdatedOrganisationAccount)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PUT[UpdatedOrganisationAccount, HttpResponse](s"$url/$orgId", details) map { _ => () }
  }
}
