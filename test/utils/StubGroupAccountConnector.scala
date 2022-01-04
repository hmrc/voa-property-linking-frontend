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

package utils

import connectors.GroupAccounts
import models.{GroupAccount, GroupAccountSubmission}
import org.mockito.Mockito.mock
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import Configs._

object StubGroupAccountConnector
    extends GroupAccounts(servicesConfig, mock(classOf[HttpClient]))(ExecutionContext.global) {

  private var stubbedGroups: Seq[GroupAccount] = Nil

  def stubAccount(account: GroupAccount) {
    stubbedGroups = stubbedGroups :+ account
  }

  def reset() {
    stubbedGroups = Nil
  }

  override def get(organisationId: Long)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] =
    Future.successful(stubbedGroups.find(_.id == organisationId))

  override def withGroupId(groupId: String)(implicit hc: HeaderCarrier) =
    Future.successful(stubbedGroups.find(_.groupId == groupId))

  override def withAgentCode(agentCode: String)(implicit hc: HeaderCarrier) =
    Future.successful(stubbedGroups.find(_.agentCode.map(_.toString).contains(agentCode)))

  override def create(account: GroupAccountSubmission)(implicit hc: HeaderCarrier): Future[Long] = Future.successful {
    val id = randomId.toLong
    stubAccount(
      GroupAccount(
        id,
        account.id,
        account.companyName,
        account.addressId,
        account.email,
        account.phone,
        account.isAgent,
        Some(arbitrary[Long].sample.get).filter(_ => account.isAgent)
      ))
    id
  }

  private def randomId = Random.nextInt(Int.MaxValue)
}
