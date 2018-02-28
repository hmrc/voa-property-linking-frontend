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

package utils

import connectors.GroupAccounts
import models.{GroupAccount, GroupAccountSubmission}
import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.util.Random

object StubGroupAccountConnector extends GroupAccounts(StubServicesConfig, StubHttp) {

  private var stubbedGroups: Seq[GroupAccount] = Nil

  def stubAccount(account: GroupAccount) {
    stubbedGroups = stubbedGroups :+ account
  }

  def reset() {
    stubbedGroups = Nil
  }

  override def get(organisationId: Long)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = Future.successful(stubbedGroups.find(_.id == organisationId))

  override def withGroupId(groupId: String)(implicit hc: HeaderCarrier) = Future.successful(stubbedGroups.find(_.groupId == groupId))

  override def withAgentCode(agentCode: String)(implicit hc: HeaderCarrier) = Future.successful(stubbedGroups.find(_.agentCode.toString == agentCode))

  override def create(account: GroupAccountSubmission)(implicit hc: HeaderCarrier): Future[Long] = Future.successful {
    val id = randomId.toLong
    stubAccount(GroupAccount(id, account.id, account.companyName, account.addressId, account.email, account.phone, account.isAgent, arbitrary[Long].sample.get))
    id
  }

  private def randomId = Random.nextInt(Int.MaxValue)
}
