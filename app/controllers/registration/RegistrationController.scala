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

import actions.registration.{GgAuthenticatedAction, SessionUserDetailsAction}
import cats.data.OptionT
import cats.implicits._
import config.ApplicationConfig
import connectors.{Addresses, GroupAccounts, IndividualAccounts}
import controllers.PropertyLinkingController
import javax.inject.{Inject, Named}
import models.registration.UserDetails._
import models.registration._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import repositories.SessionRepo
import services._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.{Assistant, User}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class RegistrationController @Inject()(
                                        val errorHandler: CustomErrorHandler,
                                        ggAuthenticated: GgAuthenticatedAction,
                                        sessionUserDetailsAction: SessionUserDetailsAction,
                                        groupAccounts: GroupAccounts,
                                        individualAccounts: IndividualAccounts,
                                        addresses: Addresses,
                                        registrationService: RegistrationService,
                                        @Named("personSession") val personalDetailsSessionRepo: SessionRepo
                                      )(
                                        implicit executionContext: ExecutionContext,
                                        override val messagesApi: MessagesApi,
                                        override val controllerComponents: MessagesControllerComponents,
                                        val config: ApplicationConfig
                                      ) extends PropertyLinkingController {

  def show(): Action[AnyContent] = (ggAuthenticated andThen sessionUserDetailsAction).async { implicit request =>
    individualAccounts.withExternalId(request.externalId).flatMap {
      case Some(voaUser) =>
        Future.successful(Redirect(controllers.routes.Dashboard.home()))
      case None =>
        request.userDetails match {
          case user@IndividualUserDetails() =>
            val fieldData = request.sessionPersonDetails match {
              case None => FieldData(user)
              case Some(sessionPersonDetails: IndividualUserAccountDetails) => FieldData(sessionPersonDetails)
            }
            Future.successful(Ok(views.html.createAccount.register_individual(AdminUser.individual, fieldData)))
          case user@OrganisationUserDetails() =>
            orgShow(user, request.sessionPersonDetails)
          case user@AgentUserDetails() =>
            Future.successful(Ok(views.html.errors.invalidAccountType()))
        }
    }
  }

  def submitIndividual(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    AdminUser.individual.bindFromRequest().fold(
      errors =>
        Future.successful(BadRequest(views.html.createAccount.register_individual(errors, FieldData()))),
      (success: IndividualUserAccountDetails) => personalDetailsSessionRepo.saveOrUpdate(success) map { _ =>
        Redirect(controllers.routes.IdentityVerification.startIv())
      }
    )
  }

  def submitOrganisation(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    AdminUser.organisation.bindFromRequest().fold(
      errors =>
        Future.successful(BadRequest(views.html.createAccount.register_organisation(errors, FieldData()))),
      (success: AdminOrganisationAccountDetails) => personalDetailsSessionRepo.saveOrUpdate(success) map { _ =>
        Redirect(controllers.routes.IdentityVerification.startIv())
      }
    )
  }

  def submitAdminToExistingOrganisation(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    AdminInExistingOrganisationUser.organisation.bindFromRequest().fold(
      errors => {
        getCompanyDetails(request.groupIdentifier).map {
          case Some(fieldData) =>
            BadRequest(
              views.html.createAccount.register_assistant_admin(
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
              Redirect(controllers.routes.IdentityVerification.startIv())
            }
          case _ =>
            unableToRetrieveCompanyDetails
        }
      }
    )
  }

  def submitAssistant(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    AssistantUser.assistant.bindFromRequest().fold(
      errors => {
        getCompanyDetails(request.groupIdentifier).map {
          case Some(fieldData) =>
            BadRequest(
              views.html.createAccount.register_assistant(
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
              .create(success.toGroupDetails(fieldData), request.userDetails, Some(Organisation))(success.toIndividualAccountSubmission(fieldData))
              .map {
                case RegistrationSuccess(personId) => Redirect(routes.RegistrationController.success(personId))
                case EnrolmentFailure => InternalServerError(errorHandler.internalServerErrorTemplate)
                case DetailsMissing => InternalServerError(errorHandler.internalServerErrorTemplate)
              }
          case _ => unableToRetrieveCompanyDetails
        }
      }
    )
  }

  private def unableToRetrieveCompanyDetails = throw new IllegalStateException("Unable to retrieve company details")

  def success(personId: Long): Action[AnyContent] = ggAuthenticated { implicit request =>
    val user = request.userDetails
    Ok(views.html.createAccount.registration_confirmation(personId.toString, user.affinityGroup, user.credentialRole))
  }

  private def orgShow(userDetails: UserDetails, sessionPersonDetails: Option[User])(implicit request: Request[AnyContent]) = {
    getCompanyDetails(userDetails.groupIdentifier).map {
      case Some(fieldData) =>
        userDetails.credentialRole match {
          case User =>
            val data = sessionPersonDetails match {
              case None => fieldData
              case Some(spd: AdminOrganisationAccountDetails) => FieldData(spd)
            }

            Ok(views.html.createAccount.register_assistant_admin(
              AdminInExistingOrganisationUser.organisation,
              data))
          case Assistant =>
            val data = sessionPersonDetails match {
              case None => fieldData
              case Some(spd: AdminInExistingOrganisationAccountDetails) => FieldData(spd)
            }

            Ok(views.html.createAccount.register_assistant(
              AssistantUser.assistant,
              data))
        }
      case None =>
        userDetails.credentialRole match {
          case Assistant =>
            Ok(views.html.errors.invalidAccountCreation())
          case _ =>
            val data = sessionPersonDetails match {
              case None =>  FieldData(userDetails)
              case Some(spd: AdminOrganisationAccountDetails) => FieldData(spd)
            }

            Ok(views.html.createAccount.register_organisation(
              AdminUser.organisation,
              data))
        }
    }
  }

  private def getCompanyDetails[A](groupIdentifier: String)(implicit hc: HeaderCarrier): Future[Option[FieldData]] = {
    (for {
      acc <- OptionT(groupAccounts.withGroupId(groupIdentifier))
      address <- OptionT(addresses.findById(acc.addressId))
    } yield {
      new FieldData(
        postcode = address.postcode,
        businessAddress = address,
        email = acc.email,
        businessName = acc.companyName,
        businessPhoneNumber = acc.phone,
        isAgent = acc.isAgent)
    }).value
  }
}
