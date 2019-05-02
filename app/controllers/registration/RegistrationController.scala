/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.registration

import actions.AuthenticatedAction
import auth.VoaAction
import cats.data.OptionT
import cats.implicits._
import config.ApplicationConfig
import connectors.{Addresses, GroupAccounts, IndividualAccounts, VPLAuthConnector}
import controllers.PropertyLinkingController
import exceptionhandler.ErrorHandler
import javax.inject.{Inject, Named}
import models.registration._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import repositories.SessionRepo
import services._
import services.email.EmailService
import services.iv.IdentityVerificationService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.{Admin, Assistant, User}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.Future

class RegistrationController @Inject()(ggAction: VoaAction,
                                       groupAccounts: GroupAccounts,
                                       individualAccounts: IndividualAccounts,
                                       enrolmentService: EnrolmentService,
                                       auth: VPLAuthConnector,
                                       addresses: Addresses,
                                       registrationService: RegistrationService,
                                       emailService: EmailService,
                                       authenticatedAction: AuthenticatedAction,
                                       errorHandler: ErrorHandler,
                                       identityVerificationService: IdentityVerificationService,
                                       @Named("personSession") val personalDetailsSessionRepo: SessionRepo
                                      )(implicit val messagesApi: MessagesApi, val config: ApplicationConfig) extends PropertyLinkingController {

  import utils.SessionHelpers._

  def show(): Action[AnyContent] = ggAction.async(isSession = true) { ctx =>
    implicit request =>
      auth.userDetails(ctx).flatMap { authUser =>
        individualAccounts
          .withExternalId(authUser.externalId)
          .map(_ => Redirect(controllers.routes.Dashboard.home()))
          .recoverWith {
            case _: NotFoundException =>
              authUser match {
                case user@UserDetails(_, UserInfo(_, _, _, _, _, _, Individual, _))   =>
                  Future.successful(Ok(views.html.createAccount.register_individual(AdminUser.individual, FieldData(userInfo = user.userInfo))))
                case user@UserDetails(_, UserInfo(_, _, _, _, _, _, Organisation, _)) =>
                  orgShow(ctx, user)
                case user@UserDetails(_, UserInfo(_, _, _, _, _, _, Agent, _))        =>
                  Future.successful(Ok(views.html.errors.invalidAccountType()))
              }
            case e                    => throw e
          }
      }
  }

  def submitIndividual(): Action[AnyContent] = ggAction.async(isSession = false) { ctx =>
    implicit request =>
      AdminUser.individual.bindFromRequest().fold(
        errors => Future.successful(BadRequest(views.html.createAccount.register_individual(errors, FieldData()))),
        success => personalDetailsSessionRepo.saveOrUpdate(success) map { _ =>
          Redirect(controllers.routes.IdentityVerification.startIv)
        }
      )
  }

  def submitOrganisation(): Action[AnyContent] = ggAction.async(isSession = false) { ctx =>
    implicit request =>
      AdminUser.organisation.bindFromRequest().fold(
        errors => Future.successful(BadRequest(views.html.createAccount.register_organisation(errors, FieldData()))),
        success => personalDetailsSessionRepo.saveOrUpdate(success) map { _ =>
          Redirect(controllers.routes.IdentityVerification.startIv)
        }
      )
  }

  def submitAdminToExistingOrganisation(): Action[AnyContent] = ggAction.async(isSession = false) { ctx =>
    implicit request =>
      AdminInExistingOrganisationUser.organisation.bindFromRequest().fold(
        errors => getCompanyDetails(ctx).map { fieldData =>
              BadRequest(
                views.html.createAccount.register_assistant_admin(
                  errors,
                  fieldData
                ))
            }
        ,
        success =>
          getCompanyDetails(ctx).flatMap { fieldData => {
              personalDetailsSessionRepo.saveOrUpdate(success.toAdminOrganisationAccountDetails(fieldData)) map { _ =>
                Redirect(controllers.routes.IdentityVerification.startIv)
              }
            }
        }
      )
  }

  def submitAssistant(): Action[AnyContent] = ggAction.async(isSession = false) { ctx =>
    implicit request =>
      AssistantUser.assistant.bindFromRequest().fold(
        errors => getCompanyDetails(ctx).map { fieldData =>
          BadRequest(
            views.html.createAccount.register_assistant(
              errors,
              fieldData
            ))
        },
        success => getCompanyDetails(ctx).flatMap { fieldData =>
              registrationService
                .create(success.toGroupDetails(fieldData), ctx, Organisation)(success.toIndividualAccountSubmission(fieldData))(hc, ec)
                .map {
                  case RegistrationSuccess(personId)  => Redirect(routes.RegistrationController.success(personId))
                  case EnrolmentFailure               => errorHandler.internalServerError
                  case DetailsMissing                 => errorHandler.internalServerError
                }
          }
      )
  }

  def success(personId: Long): Action[AnyContent] = ggAction.async(isSession = false) { ctx =>
    implicit request =>
      auth.userDetails(ctx).map(user =>
        Ok(views.html.createAccount.registration_confirmation(personId.toString, user.userInfo.affinityGroup, user.userInfo.credentialRole))
          .withSession(request.session.removeUserDetails))
  }

  private def orgShow[A](ctx: A, userDetails: UserDetails)(implicit request: Request[AnyContent]): Future[Result] =
    getCompanyDetails(ctx).map { fieldData =>
        userDetails.userInfo.credentialRole match {
          case Admin | User =>
            Ok(views.html.createAccount.register_assistant_admin(
              AdminInExistingOrganisationUser.organisation,
              fieldData))
          case Assistant    =>
            Ok(views.html.createAccount.register_assistant(
              AssistantUser.assistant,
              fieldData))
        }
    }.recover {
      case _: NotFoundException =>
        userDetails.userInfo.credentialRole match {
          case Admin | User =>
            Ok(views.html.createAccount.register_organisation(
              AdminUser.organisation,
              FieldData(userDetails.userInfo)))
          case Assistant    =>
            Ok(views.html.errors.invalidAccountCreation())
        }
      case e                    => throw e
    }

  private def getCompanyDetails[A](ctx: A)(implicit hc: HeaderCarrier): Future[FieldData] = {
    for {
      groupId <- auth.getGroupId(ctx)
      acc     <- groupAccounts.withGroupId(groupId)
      address <- addresses.findById(acc.addressId)
    } yield {
      new FieldData(
        postcode = address.postcode,
        businessAddress = address,
        email = acc.email,
        businessName = acc.companyName,
        businessPhoneNumber = acc.phone,
        isAgent = acc.isAgent
      )
    }
  }
}
