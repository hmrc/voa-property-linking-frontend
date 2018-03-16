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

package services.iv

import javax.inject.{Inject, Named}

import cats.instances.all._
import cats.syntax.all._
import cats.data.OptionT
import config.ApplicationConfig
import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import connectors.{Addresses, GroupAccounts, IndividualAccounts, VPLAuthConnector}
import controllers.routes
import models._
import models.identityVerificationProxy.Journey
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.Html
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel

import scala.concurrent.{ExecutionContext, Future}

trait IdentityVerificationService {

  type B

  val proxyConnector: IdentityVerificationProxyConnector
  val config: ApplicationConfig

  def start(userData: IVDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    proxyConnector
      .start(Journey("voa-property-linking", successUrl, failureUrl, ConfidenceLevel.L200, userData))

  def someCase(obj: B)(implicit request: Request[_], messages: Messages): Html

  def noneCase(implicit request: Request[_], messages: Messages): Html

  def continue[A](journeyId: String)(implicit ctx: A, hc: HeaderCarrier, ec: ExecutionContext): Future[Option[B]]

  protected val successUrl: String

  private val failureUrl = config.baseUrl + routes.IdentityVerification.fail().url
}

class IdentityVerificationServiceEnrolment @Inject()(
                                            auth: VPLAuthConnector,
                                            individuals: IndividualAccounts,
                                            val proxyConnector: IdentityVerificationProxyConnector,
                                            implicit val config: ApplicationConfig
                                          ) extends IdentityVerificationService {

  type B = DetailedIndividualAccount

  protected val successUrl: String = config.baseUrl + routes.IdentityVerification.success().url

  def someCase(obj: DetailedIndividualAccount)(implicit request: Request[_], messages: Messages) = views.html.identityVerification.success(routes.Dashboard.home().url)

  def noneCase(implicit request: Request[_], messages: Messages) = Html("Failure Case need Html")

  def continue[A](journeyId: String)(implicit ctx: A, hc: HeaderCarrier, ec: ExecutionContext): Future[Option[DetailedIndividualAccount]] = {
    (for {
      userId <- OptionT.liftF(auth.getExternalId(ctx))
      details <- OptionT.liftF(individuals.withExternalId(userId))
      res <- OptionT.fromOption[Future](details)
      _ <- OptionT.liftF(individuals.update(res.copy(trustId = journeyId)))
    } yield res).value
  }
}

class IdentityVerificationServiceNonEnrolment @Inject()(
                                                         auth: VPLAuthConnector,
                                                         individuals: IndividualAccounts,
                                                         val proxyConnector: IdentityVerificationProxyConnector,
                                                         @Named ("personSession") personalDetailsSessionRepo: SessionRepo,
                                                         implicit val config: ApplicationConfig,
                                                         groups: GroupAccounts,
                                                         addresses: Addresses
                                             ) extends IdentityVerificationService {

  type B = GroupAccount

  protected val successUrl: String = config.baseUrl + routes.IdentityVerification.restoreSession().url

  def someCase(obj: GroupAccount)(implicit request: Request[_], messages: Messages) = views.html.createAccount.groupAlreadyExists(obj.companyName)

  def noneCase(implicit request: Request[_], messages: Messages) = views.html.identityVerification.success(routes.CreateGroupAccount.show().url)

  def continue[A](journeyId: String)(implicit ctx: A, hc: HeaderCarrier, ec: ExecutionContext): Future[Option[GroupAccount]] = {
    val eventualGroupId = auth.getGroupId(ctx)
    val eventualExternalId = auth.getExternalId(ctx)
    val eventualIndividualDetails = personalDetailsSessionRepo.get[PersonalDetails]

    for {
      groupId <- eventualGroupId
      userId <- eventualExternalId
      account <- groups.withGroupId(groupId)
      details <- eventualIndividualDetails.map(_.getOrElse(throw new Exception("no details found")))
      id <- registerAddress(details)
      d = details.withAddressId(id)
      _ <- account match {
        case Some(acc) => individuals.create(IndividualAccountSubmission(userId, journeyId, Some(acc.id), d.individualDetails))
        case _ => personalDetailsSessionRepo.saveOrUpdate(d)
      }
    } yield account
  }

  private def registerAddress(details: PersonalDetails)(implicit hc: HeaderCarrier): Future[Int] = details.address.addressUnitId match {
    case Some(id) => Future.successful(id)
    case None => addresses.create(details.address)
  }
}


