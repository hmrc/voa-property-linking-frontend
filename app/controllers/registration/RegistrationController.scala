/*
 * Copyright 2021 HM Revenue & Customs
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

import actions.registration.{GgAuthenticatedAction, SessionUserDetailsAction}
import cats.data.OptionT
import cats.implicits._
import config.ApplicationConfig
import connectors.{Addresses, GroupAccounts, IndividualAccounts}
import controllers.PropertyLinkingController
import models.registration.UserDetails._
import models.registration._
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import repositories.SessionRepo
import services._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.{Assistant, ConfidenceLevel, User}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RegistrationController @Inject()(
      val errorHandler: CustomErrorHandler,
      ggAuthenticated: GgAuthenticatedAction,
      sessionUserDetailsAction: SessionUserDetailsAction,
      groupAccounts: GroupAccounts,
      individualAccounts: IndividualAccounts,
      addresses: Addresses,
      registrationService: RegistrationService,
      invalidAccountTypeView: views.html.errors.invalidAccountType,
      invalidAccountCreationView: views.html.errors.invalidAccountCreation,
      registerIndividualView: views.html.createAccount.registerIndividual,
      registerOrganisationView: views.html.createAccount.registerOrganisation,
      registerAssistantAdminView: views.html.createAccount.registerAssistantAdmin,
      registerAssistantView: views.html.createAccount.registerAssistant,
      registerConfirmationView: views.html.createAccount.registrationConfirmation,
      @Named("personSession") val personalDetailsSessionRepo: SessionRepo
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController with Logging {

  def show(): Action[AnyContent] = (ggAuthenticated andThen sessionUserDetailsAction).async { implicit request =>
    individualAccounts.withExternalId(request.externalId).flatMap {
      case Some(_) =>
        Future.successful(Redirect(config.dashboardUrl("home")))
      case None =>
        request.userDetails match {
          case user @ IndividualUserDetails() =>
            val fieldData = request.sessionPersonDetails match {
              case None                                                     => FieldData(user)
              case Some(sessionPersonDetails: IndividualUserAccountDetails) => FieldData(sessionPersonDetails)
            }
            Future.successful(Ok(registerIndividualView(AdminUser.individual, fieldData)))
          case user @ OrganisationUserDetails() =>
            orgShow(user, request.sessionPersonDetails)
          case _ @AgentUserDetails() =>
            Future.successful(Ok(invalidAccountTypeView()))
          case _ => Future.successful(Ok(invalidAccountTypeView()))
        }
    }
  }

  def submitIndividual(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    val userDetails = request.userDetails
    AdminUser.individual
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(registerIndividualView(errors, FieldData()))),
        (success: IndividualUserAccountDetails) =>
          personalDetailsSessionRepo.saveOrUpdate(success) map { _ =>
            identityVerificationIfRequired(request.userDetails)
        }
      )
  }

  def submitOrganisation(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    AdminUser.organisation
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(registerOrganisationView(errors, FieldData()))),
        (success: AdminOrganisationAccountDetails) =>
          personalDetailsSessionRepo.saveOrUpdate(success) map { _ =>
            identityVerificationIfRequired(request.userDetails)
        }
      )
  }

  private def identityVerificationIfRequired(userDetails: UserDetails) =
    if (userDetails.confidenceLevel.level < ConfidenceLevel.L200.level)
      // push user through IV as their Confidence Level is insufficient
      Redirect(controllers.routes.IdentityVerification.startIv())
    else {
      // skip IV as user's Confidence Level is sufficient
      // TODO temporary solution - intention is to call registrationService.continue()
      Redirect(controllers.routes.IdentityVerification.bypass) // TODO
    }

  def submitAdminToExistingOrganisation(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    val userDetails = request.userDetails
    AdminInExistingOrganisationUser.organisation
      .bindFromRequest()
      .fold(
        errors => {
          getCompanyDetails(request.groupIdentifier).map {
            case Some(fieldData) =>
              BadRequest(
                registerAssistantAdminView(
                  errors,
                  fieldData
                ))
            case _ =>
              unableToRetrieveCompanyDetails
          }
        },
        (success: AdminInExistingOrganisationAccountDetails) => {
          getCompanyDetails(request.groupIdentifier).flatMap {
            case Some(fieldData) =>
              personalDetailsSessionRepo.saveOrUpdate(success.toAdminOrganisationAccountDetails(fieldData)) map { _ =>
                identityVerificationIfRequired(request.userDetails)
              }
            case _ =>
              unableToRetrieveCompanyDetails
          }
        }
      )
  }

  def submitAssistant(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    val userDetails = request.userDetails
    AssistantUser.assistant
      .bindFromRequest()
      .fold(
        errors => {
          getCompanyDetails(request.groupIdentifier).map {
            case Some(fieldData) =>
              BadRequest(
                registerAssistantView(
                  errors,
                  fieldData
                ))
            case _ =>
              unableToRetrieveCompanyDetails

          }
        },
        success => {
          getCompanyDetails(request.groupIdentifier).flatMap {
            case Some(fieldData) =>
              registrationService
                .create(success.toGroupDetails(fieldData), request.userDetails, Some(Organisation))(
                  success.toIndividualAccountSubmission(fieldData))
                .map {
                  case RegistrationSuccess(personId) => Redirect(routes.RegistrationController.success(personId))
                  case EnrolmentFailure              => InternalServerError(errorHandler.internalServerErrorTemplate)
                  case DetailsMissing                => InternalServerError(errorHandler.internalServerErrorTemplate)
                }
            case _ => unableToRetrieveCompanyDetails
          }
        }
      )
  }

  private def unableToRetrieveCompanyDetails = throw new IllegalStateException("Unable to retrieve company details")

  def success(personId: Long): Action[AnyContent] = ggAuthenticated { implicit request =>
    val user = request.userDetails
    Ok(registerConfirmationView(personId.toString, user.affinityGroup, user.credentialRole))
  }

  private def orgShow(userDetails: UserDetails, sessionPersonDetails: Option[User])(
        implicit request: Request[AnyContent]) =
    getCompanyDetails(userDetails.groupIdentifier).map {
      case Some(fieldData) =>
        userDetails.credentialRole match {
          case User =>
            val data = sessionPersonDetails match {
              case None                                       => fieldData
              case Some(spd: AdminOrganisationAccountDetails) => FieldData(spd)
            }

            Ok(registerAssistantAdminView(AdminInExistingOrganisationUser.organisation, data))
          case Assistant =>
            val data = sessionPersonDetails match {
              case None                                                 => fieldData
              case Some(spd: AdminInExistingOrganisationAccountDetails) => FieldData(spd)
            }
            Ok(registerAssistantView(AssistantUser.assistant, data))
        }
      case None =>
        userDetails.credentialRole match {
          case Assistant =>
            Ok(invalidAccountCreationView())
          case _ =>
            val data = sessionPersonDetails match {
              case None                                       => FieldData(userDetails)
              case Some(spd: AdminOrganisationAccountDetails) => FieldData(spd)
            }

            Ok(registerOrganisationView(AdminUser.organisation, data))
        }
    }

  private def getCompanyDetails[A](groupIdentifier: String)(implicit hc: HeaderCarrier): Future[Option[FieldData]] =
    (for {
      acc     <- OptionT(groupAccounts.withGroupId(groupIdentifier))
      address <- OptionT(addresses.findById(acc.addressId))
    } yield {
      new FieldData(
        postcode = address.postcode,
        businessAddress = address,
        email = acc.email,
        businessName = acc.companyName,
        businessPhoneNumber = acc.phone,
        isAgent = acc.isAgent
      )
    }).value
}
