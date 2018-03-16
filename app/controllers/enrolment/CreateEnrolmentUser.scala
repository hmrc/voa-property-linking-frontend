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

package controllers.enrolment

import javax.inject.Inject

import actions.AuthenticatedAction
import auth.VoaAction
import cats.data.OptionT
import cats.implicits._
import config.{ApplicationConfig, Global}
import connectors.{Addresses, GroupAccounts, IndividualAccounts, VPLAuthConnector}
import controllers.PropertyLinkingController
import models.enrolment._
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request}
import services._
import services.email.EmailService
import services.iv.IdentityVerificationService
import uk.gov.hmrc.auth.core.{Admin, Assistant}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}

import scala.concurrent.Future

class CreateEnrolmentUser @Inject()(ggAction: VoaAction,
                                     groupAccounts: GroupAccounts,
                                     individualAccounts: IndividualAccounts,
                                     enrolmentService: EnrolmentService,
                                     auth: VPLAuthConnector,
                                     addresses: Addresses,
                                    registration: RegistrationService,
                                     emailService: EmailService,
                                     authenticatedAction: AuthenticatedAction,
                                    identityVerificationService: IdentityVerificationService
                                   )(implicit val messagesApi: MessagesApi, val config: ApplicationConfig) extends PropertyLinkingController {

  import utils.SessionHelpers._

  def show() = ggAction.async(isSession = true) { ctx => implicit request =>
      auth.userDetails(ctx).flatMap {
          case user @ UserDetails(_, UserInfo(_, _, _, _, _, _, Individual, _)) =>
            Future.successful(Ok(views.html.createAccount.enrolment_individual(EnrolmentUser.individual, FieldData(userInfo = user.userInfo))))
          case user @ UserDetails(_, UserInfo(_, _, _, _, _, _, Organisation, _)) =>
            orgShow(ctx, user)
        }
  }

  def submitIndividual() = ggAction.async(isSession = false) { ctx =>
    implicit request =>
      EnrolmentUser.individual.bindFromRequest().fold(
        errors =>
          BadRequest(views.html.createAccount.enrolment_individual(errors, FieldData())),
        success =>
          registration.create(success.toGroupDetails, success.toIvDetails, ctx)(success.toIndividualAccountSubmission)(hc, ec).map{
            case EnrolmentSuccess(link, personId) => Redirect(routes.CreateEnrolmentUser.success(personId, link.getLink(config.ivEnabled)))
            case IVNotRequired(personId)          => Redirect(routes.CreateEnrolmentUser.success(personId, controllers.routes.Dashboard.home().url))
            case EnrolmentFailure                 => InternalServerError(Global.internalServerErrorTemplate)
            case DetailsMissing                   => InternalServerError(Global.internalServerErrorTemplate)
          }
      )
  }

  def submitOrganisation() = ggAction.async(isSession = false) { ctx =>
    implicit request =>
      EnrolmentUser.organisation.bindFromRequest().fold(
        errors => BadRequest(views.html.createAccount.enrolment_organisation(errors, FieldData())),
        success =>
          registration
            .create(success.toGroupDetails, success.toIvDetails, ctx)(success.toIndividualAccountSubmission)(hc, ec)
          .map{
            case EnrolmentSuccess(link, personId) => Redirect(routes.CreateEnrolmentUser.success(personId, link.getLink(config.ivEnabled)))
            case IVNotRequired(personId)          => Redirect(routes.CreateEnrolmentUser.success(personId, controllers.routes.Dashboard.home().url))
            case EnrolmentFailure                 => InternalServerError(Global.internalServerErrorTemplate)
            case DetailsMissing                   => InternalServerError(Global.internalServerErrorTemplate)
          }
      )
  }

  def success(personId: Long, url: String) = authenticatedAction { implicit request =>
    Ok(views.html.createAccount.confirmation_enrolment(s"Person ID: $personId", url))
      .withSession(request.session.removeUserDetails)
  }

  private def orgShow[A](ctx: A, userDetails: UserDetails)(implicit request: Request[AnyContent]) = {
    val fieldDataFOptT = (for {
      groupId     <- OptionT.liftF(auth.getGroupId(ctx))
      acc         <- OptionT(groupAccounts.withGroupId(groupId))
      address     <- OptionT(addresses.findById(acc.addressId))
    } yield {
      new FieldData(postcode = address.postcode, email = acc.email)
    }).value

    fieldDataFOptT.map{
      case Some(fieldData) =>
        Ok(views.html.createAccount.enrolment_organisation(
          EnrolmentUser.organisation,
          fieldData))
      case None =>
        userDetails.userInfo.role match {
          case Admin      =>
            Ok(views.html.createAccount.enrolment_organisation(
            EnrolmentUser.organisation,
              FieldData(userDetails.userInfo)))
          case Assistant  =>
            Unauthorized
        }
    }

  }
}
