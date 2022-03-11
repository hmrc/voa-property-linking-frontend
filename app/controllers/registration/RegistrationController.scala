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

package controllers.registration

import actions.AuthenticatedAction
import actions.registration.requests.{RegistrationSessionRequest, RequestWithUserDetails}
import actions.registration.{GgAuthenticatedAction, SessionUserDetailsAction, WithRegistrationSessionRefiner}
import cats.data.OptionT
import cats.implicits._
import config.ApplicationConfig
import connectors.{Addresses, GroupAccounts, IndividualAccounts}
import controllers.PropertyLinkingController
import models.registration.UserDetails._
import models.registration._
import models.registration.IndividualName
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc._
import repositories.SessionRepo
import services._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.{Assistant, ConfidenceLevel, User}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import views.html._
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RegistrationController @Inject()(
      val errorHandler: CustomErrorHandler,
      ggAuthenticated: GgAuthenticatedAction,
      authenticated: AuthenticatedAction,
      sessionUserDetailsAction: SessionUserDetailsAction,
      groupAccounts: GroupAccounts,
      individualAccounts: IndividualAccounts,
      addresses: Addresses,
      withRegistrationSessionRefiner: WithRegistrationSessionRefiner,
      registrationService: RegistrationService,
      invalidAccountTypeView: errors.invalidAccountType,
      invalidAccountCreationView: errors.invalidAccountCreation,
      registerIndividualView: createAccount.registerIndividual,
      registerOrganisationView: createAccount.registerOrganisation,
      registerAssistantAdminView: createAccount.registerAssistantAdmin,
      registerAssistantView: createAccount.registerAssistant,
      registerConfirmationView: createAccount.registrationConfirmation,
      registerIndividualNameView: createAccount.individual.registerIndividualName,
      registerIndividualPersonalDetailsView: createAccount.individual.registerIndividualPersonalDetails,
      registerIndividualDOBView: createAccount.individual.registerIndividualDOB,
      registerIndividualNINOView: createAccount.individual.registerIndividualNINO,
      registerOrganisationIsAgentView: createAccount.organisation.registerOrganisationIsAgent,
      registerOrganisationNameView: createAccount.organisation.registerOrganisationName,
      registerOrganisationDetailsView: createAccount.organisation.registerOrganisationDetails,
      registerOrganisationDOBView: createAccount.organisation.registerOrganisationDOB,
      registerOrganisationNINOView: createAccount.organisation.registerOrganisationNINO,
      confirmationView: createAccount.confirmation,
      @Named("personSession") val personalDetailsSessionRepo: SessionRepo,
      @Named("registrationDetails") val registrationDetailsSessionRepo: SessionRepo
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
            if (config.newRegistrationJourneyEnabled) {
              Future.successful(Ok(registerIndividualNameView(IndividualName.individualNameForm)))
            } else Future.successful(Ok(registerIndividualView(AdminUser.individual, fieldData)))
          case user @ OrganisationUserDetails() =>
            orgShow(user, request.sessionPersonDetails)
          case _ @AgentUserDetails() =>
            Future.successful(Ok(invalidAccountTypeView()))
          case _ => Future.successful(Ok(invalidAccountTypeView()))
        }
    }
  }

  def submitIndividualOld(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
      AdminUser.individual
        .bindFromRequest()
        .fold(
          errors => Future.successful(BadRequest(registerIndividualView(errors, FieldData()))),
          (success: IndividualUserAccountDetails) =>
            personalDetailsSessionRepo.saveOrUpdate(success) flatMap { _ =>
              identityVerificationIfRequiredOld(request)
            }
        )
  }

  def submitIndividualName(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
      IndividualName.individualNameForm
        .bindFromRequest
        .fold(
          errors => Future.successful(BadRequest(registerIndividualNameView(errors))),
          (successfulFormIndividualName: IndividualName) =>
           {
             registrationDetailsSessionRepo.start(RegistrationSession(successfulFormIndividualName))
             Future.successful(Redirect(controllers.registration.routes.RegistrationController.showIndividualPersonalDetails()))
           }
        )
  }


  def showIndividualPersonalDetails(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    Future.successful(Ok(registerIndividualPersonalDetailsView(IndividualPersonalDetails.individualPersonalDetailsForm)))
  }

  def submitIndividualPersonalDetails(): Action[AnyContent] = (ggAuthenticated andThen withRegistrationSessionRefiner).async { implicit request =>
    IndividualPersonalDetails.individualPersonalDetailsForm
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(registerIndividualPersonalDetailsView(errors))),
        (successfulFormIndividualPersonalDetails: IndividualPersonalDetails) =>
          {
            registrationDetailsSessionRepo.saveOrUpdate(RegistrationSession(request.sessionData, successfulFormIndividualPersonalDetails))
            Future.successful(Redirect(controllers.registration.routes.RegistrationController.showIndividualDOB()))
          }
      )

  }

  def showIndividualDOB(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    Future.successful(Ok(registerIndividualDOBView(IndividualDateOfBirth.individualDateOfBirthForm)))
  }

  def submitIndividualDOB(): Action[AnyContent] = (ggAuthenticated andThen withRegistrationSessionRefiner).async { implicit request =>
    IndividualDateOfBirth.individualDateOfBirthForm
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(registerIndividualDOBView(errors))),
        (successfulFormIndividualDOB: IndividualDateOfBirth) =>
          {
            registrationDetailsSessionRepo.saveOrUpdate(RegistrationSession(request.sessionData, successfulFormIndividualDOB))
            Future.successful(Redirect(controllers.registration.routes.RegistrationController.showIndividualNINO()))
          }
      )
  }

  def showIndividualNINO(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    Future.successful(Ok(registerIndividualNINOView(IndividualNino.individualNinoForm)))
  }

  def submitIndividualNINO(): Action[AnyContent] = (ggAuthenticated andThen withRegistrationSessionRefiner).async { implicit request =>
    IndividualNino.individualNinoForm
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(registerIndividualNINOView(errors))),
        (successfulFormIndividualNINO: IndividualNino) =>
          {
            registrationDetailsSessionRepo.saveOrUpdate(RegistrationSession(request.sessionData, successfulFormIndividualNINO)
            ) flatMap { _ =>
              identityVerificationIfRequired(request)
            }
          }
      )
  }

  def submitOrganisationOld(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    AdminUser.organisation
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(registerOrganisationView(errors, FieldData()))),
        (success: AdminOrganisationAccountDetails) =>
          personalDetailsSessionRepo.saveOrUpdate(success) flatMap { _ =>
            identityVerificationIfRequiredOld(request)
          }
      )
  }

  def submitOrganisationName(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    OrganisationName.organisationNameForm
      .bindFromRequest
      .fold(
        errors => Future.successful(BadRequest(registerOrganisationNameView(errors))),
        (successfulFormOrgName: OrganisationName) =>
        {
          registrationDetailsSessionRepo.start(RegistrationSession(successfulFormOrgName))
          Future.successful(Redirect(controllers.registration.routes.RegistrationController.showOrganisationIsAgent()))
        }
      )
  }

  def showOrganisationIsAgent(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    Future.successful(Ok(registerOrganisationIsAgentView(OrganisationIsAgent.organisationIsAgentForm)))
  }

  def submitOrganisationIsAgent(): Action[AnyContent] = (ggAuthenticated andThen withRegistrationSessionRefiner).async { implicit request =>
    OrganisationIsAgent.organisationIsAgentForm
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(registerOrganisationIsAgentView(errors))),
        (successfulFormOrgIsAgent: OrganisationIsAgent) =>
          {
            registrationDetailsSessionRepo.saveOrUpdate(RegistrationSession(request.sessionData, successfulFormOrgIsAgent))
            Future.successful(Redirect(controllers.registration.routes.RegistrationController.showOrganisationDetails()))
          }
      )
  }

  def showOrganisationDetails(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    Future.successful(Ok(registerOrganisationDetailsView(OrganisationDetails.organisationDetailsForm)))
  }

  def submitOrganisationDetails(): Action[AnyContent] = (ggAuthenticated andThen withRegistrationSessionRefiner).async { implicit request =>
    OrganisationDetails.organisationDetailsForm
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(registerOrganisationDetailsView(errors))),
        (successfulFormOrgDetails: OrganisationDetails) =>
          {
            registrationDetailsSessionRepo.saveOrUpdate(RegistrationSession(request.sessionData, successfulFormOrgDetails))
            Future.successful(Redirect(controllers.registration.routes.RegistrationController.showIndividualNINO()))
          }
      )
  }

  def showOrganisationDOB(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    Future.successful(Ok(registerOrganisationDOBView(OrganisationDateOfBirth.organisationDateOfBirthForm)))
  }

  def submitOrganisationDOB(): Action[AnyContent] = (ggAuthenticated andThen withRegistrationSessionRefiner).async { implicit request =>
    OrganisationDateOfBirth.organisationDateOfBirthForm
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(registerOrganisationDOBView(errors))),
        (successfulFormOrgDOB: OrganisationDateOfBirth) =>
          {
            registrationDetailsSessionRepo.saveOrUpdate(RegistrationSession(request.sessionData, successfulFormOrgDOB
            ))
            Future.successful(Redirect(controllers.registration.routes.RegistrationController.showOrganisationNINO()))
          }
      )
  }

  def showOrganisationNINO(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
    Future.successful(Ok(registerOrganisationNINOView(OrganisationNino.organisationNinoForm)))
  }

  def submitOrganisationNINO(): Action[AnyContent] = (ggAuthenticated andThen withRegistrationSessionRefiner).async { implicit request =>
    OrganisationNino.organisationNinoForm
      .bindFromRequest()
      .fold(
        errors => Future.successful(BadRequest(registerOrganisationNINOView(errors))),
        (successfulFormOrganisationNINO: OrganisationNino) =>
        {
          registrationDetailsSessionRepo.saveOrUpdate(RegistrationSession(request.sessionData, successfulFormOrganisationNINO)
          ) flatMap { _ =>
            identityVerificationIfRequired(request)
          }
        }
      )
  }

  def submitAdminToExistingOrganisation(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
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
              personalDetailsSessionRepo.saveOrUpdate(success.toAdminOrganisationAccountDetails(fieldData)) flatMap {
                _ =>
                  identityVerificationIfRequiredOld(request)
              }
            case _ =>
              unableToRetrieveCompanyDetails
          }
        }
      )
  }

  // TODO check IV enabled flag here
  private def identityVerificationIfRequiredOld(request: RequestWithUserDetails[_])(
    implicit hc: HeaderCarrier): Future[Result] =
    if (request.userDetails.confidenceLevel.level < ConfidenceLevel.L200.level) {
      // push user through IV as their Confidence Level is insufficient
      Future.successful(Redirect(controllers.routes.IdentityVerification.startIv))
    } else {
      // skip IV as user's Confidence Level is sufficient
      registrationService.continueOld(None, request.userDetails).map {
        case Some(RegistrationSuccess(personId)) =>
          if (config.newRegistrationJourneyEnabled)
            Redirect(routes.RegistrationController.confirmation(personId))
          else Redirect(routes.RegistrationController.success(personId))
        case _ => InternalServerError(errorHandler.internalServerErrorTemplate(request))
      }
    }

  private def identityVerificationIfRequired(request: RegistrationSessionRequest[_])(
        implicit hc: HeaderCarrier): Future[Result] =
    if (request.userDetails.confidenceLevel.level < ConfidenceLevel.L200.level) {
      // push user through IV as their Confidence Level is insufficient
      Future.successful(Redirect(controllers.routes.IdentityVerification.startIv))
    } else {
      // skip IV as user's Confidence Level is sufficient
      registrationService.continue(None, request.userDetails).map {
        case Some(RegistrationSuccess(personId)) =>
          if (config.newRegistrationJourneyEnabled)
            Redirect(routes.RegistrationController.confirmation(personId))
          else Redirect(routes.RegistrationController.success(personId))
        case _ => InternalServerError(errorHandler.internalServerErrorTemplate(request))
      }
    }

  def submitAssistant(): Action[AnyContent] = ggAuthenticated.async { implicit request =>
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
            case _ => unableToRetrieveCompanyDetails
          }
        },
        success => {
          getCompanyDetails(request.groupIdentifier).flatMap {
            case Some(fieldData) =>
              registrationService
                .create(success.toGroupDetails(fieldData), request.userDetails, Some(Organisation))(
                  success.toIndividualAccountSubmission(fieldData))
                .map {
                  case RegistrationSuccess(personId) =>
                    if (config.newRegistrationJourneyEnabled)
                      Redirect(routes.RegistrationController.confirmation(personId))
                    else Redirect(routes.RegistrationController.success(personId))
                  case EnrolmentFailure => InternalServerError(errorHandler.internalServerErrorTemplate)
                  case DetailsMissing   => InternalServerError(errorHandler.internalServerErrorTemplate)
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

  def confirmation(personId: Long): Action[AnyContent] = authenticated { implicit request =>
    Ok(
      confirmationView(
        personId.toString,
        request.organisationAccount.agentCode,
        request.individualAccount.details.email))
  }

  private def orgShow(userDetails: UserDetails, sessionPersonDetails: Option[User])(
        implicit request: Request[AnyContent]): Future[Result] =
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
            if(config.newRegistrationJourneyEnabled){
              Ok(registerOrganisationNameView(OrganisationName.organisationNameForm))
            } else Ok(registerOrganisationView(AdminUser.organisation, data))
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
