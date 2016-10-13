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

import connectors.AccountConnector
import controllers.Account
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

object StubAccountConnector extends AccountConnector(StubHttp) {

  private var stubbedAccounts: Seq[Account] = Nil

  def stubAccount(account: Account): Unit = {
    stubbedAccounts = stubbedAccounts :+ account
  }

  def reset(): Unit = {
    stubbedAccounts = Nil
  }

  override def get()(implicit hc: HeaderCarrier): Future[Seq[Account]] = Future.successful(stubbedAccounts)

  override def get(accountId: String)(implicit hc: HeaderCarrier): Future[Option[Account]] = Future.successful(stubbedAccounts.find(_.companyName == accountId))

  override def create(account: Account)(implicit hc: HeaderCarrier): Future[Unit] = Future.successful(stubAccount(account))
}
