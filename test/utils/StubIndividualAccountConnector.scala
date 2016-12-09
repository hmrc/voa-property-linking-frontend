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

import connectors.IndividualAccounts
import models.IndividualAccount
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.Random

object StubIndividualAccountConnector extends IndividualAccounts(StubHttp) {

  private var stubbedIndividuals: Seq[IndividualAccount] = Nil

  def stubAccount(account: IndividualAccount): Unit = {
    stubbedIndividuals = stubbedIndividuals :+ account
  }

  def reset(): Unit = {
    stubbedIndividuals = Nil
  }

  override def get(externalId: String)(implicit hc: HeaderCarrier): Future[Option[IndividualAccount]] = Future.successful(stubbedIndividuals.find(_.externalId == externalId))

  override def create(account: IndividualAccount)(implicit hc: HeaderCarrier): Future[Int] = Future.successful(stubAccount(account)).map { _ => Random.nextInt(Int.MaxValue) }
}
