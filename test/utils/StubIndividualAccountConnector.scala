/*
 * Copyright 2024 HM Revenue & Customs
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
import models.{DetailedIndividualAccount, IndividualAccountSubmission}
import org.mockito.Mockito.mock
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.Configs._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object StubIndividualAccountConnector
    extends IndividualAccounts(servicesConfig, mock(classOf[DefaultHttpClient]))(ExecutionContext.global) {

  private var stubbedIndividuals: Seq[DetailedIndividualAccount] = Nil

  def stubAccount(account: DetailedIndividualAccount): Unit =
    stubbedIndividuals = stubbedIndividuals :+ account

  def reset(): Unit =
    stubbedIndividuals = Nil

  override def get(personId: Long)(implicit hc: HeaderCarrier): Future[Option[DetailedIndividualAccount]] =
    Future.successful(stubbedIndividuals.find(_.individualId == personId))

  override def update(account: DetailedIndividualAccount)(implicit hc: HeaderCarrier): Future[Unit] =
    Future.successful {
      stubbedIndividuals = stubbedIndividuals.map {
        case DetailedIndividualAccount(_, _, _, id, _) if id == account.individualId => account
        case acc                                                                     => acc
      }
    }

  override def withExternalId(
        externalId: String
  )(implicit hc: HeaderCarrier): Future[Option[DetailedIndividualAccount]] =
    Future.successful {
      stubbedIndividuals.find(_.externalId == externalId)
    }

  override def create(account: IndividualAccountSubmission)(implicit hc: HeaderCarrier): Future[Int] = {
    val personId = Random.nextInt(Int.MaxValue)
    Future.successful(stubAccount(detailed(personId, account))).map(_ => personId)(ExecutionContext.global)
  }

  private def detailed(personId: Int, account: IndividualAccountSubmission): DetailedIndividualAccount =
    DetailedIndividualAccount(
      account.externalId,
      account.trustId,
      account.organisationId.getOrElse(-1),
      personId,
      account.details
    )
}
