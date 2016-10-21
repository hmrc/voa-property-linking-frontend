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

package utils

import connectors.GroupAccounts
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.GroupAccount
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object StubGroupAccountConnector extends GroupAccounts(StubHttp) {

  private var stubbedGroups: Seq[GroupAccount] = Nil

  def stubAccount(account: GroupAccount) {
    stubbedGroups = stubbedGroups :+ account
  }

  def reset() {
    stubbedGroups = Nil
  }

  override def get()(implicit hc: HeaderCarrier): Future[Seq[GroupAccount]] = Future.successful(stubbedGroups)

  override def get(groupId: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = Future.successful(stubbedGroups.find(_.id == groupId))

  override def create(account: GroupAccount)(implicit hc: HeaderCarrier): Future[Unit] = Future.successful(stubAccount(account))
}
