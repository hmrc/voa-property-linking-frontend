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

import controllers.GroupAccountDetails
import models._
import play.api.libs.json.{JsDefined, JsNumber, JsValue}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class GroupAccounts(http: WSHttp)(implicit ec: ExecutionContext) extends ServicesConfig {

  lazy val url = baseUrl("property-linking") + "/property-linking/groups"

  def get(organisationId: Int)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[GroupAccount]](s"$url/$organisationId")
  }

  def withGroupId(groupId: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[GroupAccount]](s"$url?groupId=$groupId")
  }

  def withAgentCode(agentCode: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[GroupAccount]](s"$url/agentCode/$agentCode")
  }

  def create(account: GroupAccountSubmission)(implicit hc: HeaderCarrier): Future[Int] = {
    http.POST[GroupAccountSubmission, JsValue](url, account) map { js => js \ "id" match {
      case JsDefined(JsNumber(id)) => id.toInt
      case _ => throw new Exception(s"Invalid id $js")
    }}
  }

  def create(groupId: String, addressId: Int, details: GroupAccountDetails,
             individualAccountSubmission: IndividualAccountSubmission)
            (implicit hc: HeaderCarrier): Future[Int] = {
    create(GroupAccountSubmission(
      groupId, details.companyName, addressId, details.email, details.phone, details.isAgent, individualAccountSubmission
    ))
  }

  def update(orgId: Int, details: UpdatedOrganisationAccount)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PUT[UpdatedOrganisationAccount, HttpResponse](s"$url/$orgId", details) map { _ => () }
  }
}
