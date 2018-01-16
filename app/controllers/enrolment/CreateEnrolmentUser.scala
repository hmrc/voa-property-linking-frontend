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
import config.Global
import connectors.{Addresses, GroupAccounts, IndividualAccounts, VPLAuthConnector}
import controllers.{GroupAccountDetails, PropertyLinkingController}
import models._
import models.enrolment._
import play.api.Logger
import play.api.data.Form
import play.api.mvc.{AnyContent, Request, Result}
import services.{EnrolmentService, Failure, Success}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class CreateEnrolmentUser @Inject()(
                                     ggAction: VoaAction,
                                     groupAccounts: GroupAccounts,
                                     individualAccounts: IndividualAccounts,
                                     enrolmentService: EnrolmentService,
                                     auth: VPLAuthConnector,
                                     addresses: Addresses,
                                     authenticatedAction: AuthenticatedAction
                                   ) extends PropertyLinkingController {

  def show() = ggAction.async(isSession = true) { ctx =>
    implicit request =>
      auth.userDetails(ctx).flatMap { userDetails =>
        userDetails.userInfo.affinityGroup match {
          case Individual =>
            Future.successful(
              Ok(views.html.createAccount.enrolment_individual(
                CreateEnrolmentIndividualAccount.form,
                FieldData(userInfo = userDetails.userInfo))))
          case Organisation => orgShow(ctx, userDetails)
        }
      }
  }

  def submitIndividual() = ggAction.async(isSession = false) { ctx =>
    implicit request =>
      CreateEnrolmentIndividualAccount.form.bindFromRequest().fold(
        errors => BadRequest(views.html.createAccount.enrolment_individual(errors, FieldData())),
        success =>
          for {
            user <- auth.getUserDetails
            groupId <- auth.getGroupId(ctx)
            groupAccountDetails <- GroupAccountDetails(success.tradingName.getOrElse("Not Applicable"), success.address, success.email, success.confirmedEmail, success.phone, false)
            id <- addresses.registerAddress(groupAccountDetails)
            individual = IndividualAccountSubmission(user.externalId, "NONIV", None, IndividualDetails(user.userInfo.firstName.getOrElse(""), user.userInfo.lastName.getOrElse(""), user.userInfo.email, success.phone, None, id))
            _ <- groupAccounts.create(groupId, id, groupAccountDetails, individual)
            personId <- individualAccounts.withExternalId(user.externalId) //This is used to get the personId back for the group accounts create.
            res <- resultMapper(personId, id)
          } yield res
      )
  }

  def submitOrganisation() = ggAction.async(isSession = false) { ctx =>
    implicit request =>
      CreateEnrolmentOrganisationAccount.form.bindFromRequest().fold(
        errors => BadRequest(views.html.createAccount.enrolment_organisation(errors, FieldData())),
        success =>
          for {
            //Format: OFF
            user                <- auth.userDetails(ctx)
            groupId             <- auth.getGroupId(ctx)
            groupAccountDetails <- GroupAccountDetails(success.companyName, success.address, success.email, success.confirmedEmail, success.phone, success.isAgent)
            id                  <- addresses.registerAddress(groupAccountDetails)
            individual          = IndividualAccountSubmission(user.externalId, "NONIV", None, IndividualDetails(user.userInfo.firstName.getOrElse(""), user.userInfo.lastName.getOrElse(""), user.userInfo.email, success.phone, None, id))
            _                   <- groupExists(groupId, acc => individualAccounts.create(createIndividualAccountSubmission(user, success.phone)(acc)), groupAccounts.create(groupId, id, groupAccountDetails, individual)) //If the user create can return the personId back we can shorten this function.
            personId            <- individualAccounts.withExternalId(user.externalId) //This is used to get the personId back for the group accounts create.
            res                 <- resultMapper(personId, id)
            //Format: ON
          } yield res
      )
  }

  private def resultMapper(option: Option[DetailedIndividualAccount], addressId: Int)
                          (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = option match {
    case Some(detailIndiv) => enrolmentService.enrol(detailIndiv.individualId, addressId).map {
      case Success => Redirect(routes.CreateEnrolmentUser.success(detailIndiv.individualId))
      case Failure => InternalServerError(Global.internalServerErrorTemplate)
    }
    case None => Future.successful(InternalServerError(Global.internalServerErrorTemplate))
  }

  def success(personId: Long) = authenticatedAction { implicit request =>
      Ok(views.html.createAccount.confirmation_enrolment(s"Person ID: $personId"))
  }

  private def createIndividualAccountSubmission(userDetails: UserDetails, phoneNumber: String)(groupAccount: GroupAccount) = {
    IndividualAccountSubmission(userDetails.externalId, "NONIV", Some(groupAccount.id), IndividualDetails(userDetails.userInfo.firstName.getOrElse(""), userDetails.userInfo.lastName.getOrElse(""), userDetails.userInfo.email, phoneNumber, None, groupAccount.addressId))
  }

  private def groupExists(groupId: String, groupExists: GroupAccount => Future[Int], noGroup: Future[Long])(implicit hc: HeaderCarrier): Future[Long] = {
    groupAccounts.withGroupId(groupId).flatMap {
      case Some(acc) => groupExists(acc).map(_.toLong)
      case _ => noGroup
    }
  }

  private def orgShow[A](ctx: A, userDetails: UserDetails)(implicit request: Request[AnyContent]) = {
    val fieldDataFOptT = for {
      groupId     <- OptionT.liftF(auth.getGroupId(ctx))
      acc         <- OptionT(groupAccounts.withGroupId(groupId))
      address    <- OptionT(addresses.findById(acc.addressId))
    } yield {
      acc.phone
      new FieldData(postcode = address.postcode, email = acc.email)
    }

    val fieldDataF = fieldDataFOptT
      .value
      .map(_.getOrElse(FieldData(userDetails.userInfo)))

    fieldDataF.map(fieldData =>
      Ok(views.html.createAccount.enrolment_organisation(
        CreateEnrolmentOrganisationAccount.form,
        fieldData))
    )
  }

  implicit private def organisationVm(form: Form[_]): CreateEnrolmentOrganisationAccountVM = CreateEnrolmentOrganisationAccountVM(form)

  implicit private def individualVm(form: Form[_]): CreateEnrolmentIndividualAccountVM = CreateEnrolmentIndividualAccountVM(form)
}
