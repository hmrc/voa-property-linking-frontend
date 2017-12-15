/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import javax.inject.Inject

import actions.{AuthenticatedAction, EnrolmentService, Failure, Success}
import auth.{GGAction, UnAuthAction, UserDetails}
import config.Global
import connectors.{Addresses, GroupAccounts, IndividualAccounts, VPLAuthConnector}
import models._
import play.api.data.Form
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

//Importing cats for monad transformers
import cats._
import cats.data._
import cats.implicits._

class CreateEnrolmentUser @Inject()(
                         gGAction: UnAuthAction,
                         groupAccounts: GroupAccounts,
                         individualAccounts: IndividualAccounts,
                         enrolmentService: EnrolmentService,
                         auth: VPLAuthConnector,
                         addresses: Addresses,
                         authenticatedAction: AuthenticatedAction
                         ) extends PropertyLinkingController {

  def show() = gGAction.async(isSession = true) { ctx =>implicit request =>
    auth.userDetails(ctx).flatMap{ x =>
      x.userInfo.affinityGroup match {
        case Individual =>
          Future.successful(Ok(views.html.createAccount.enrolment_org(CreateGroupAccount.form)))
        case Organisation =>
          (for {
            groupId     <- OptionT.liftF(auth.getGroupId(ctx))
            acc         <- OptionT(groupAccounts.withGroupId(groupId))
            address    <- OptionT(addresses.findById(acc.addressId))
          } yield Ok(views.html.createAccount.enrolment_org(CreateGroupAccount.form, address.postcode, acc.email)))
            .value
            .map{
            case Some(x)  => x
            case None     => Ok(views.html.createAccount.enrolment_org(CreateGroupAccount.form))
          }
      }
    }
  }

  def submitIndiv = gGAction.async(isSession = false){ ctx => implicit request =>
    CreateGroupAccount.form.bindFromRequest().fold(
      errors => BadRequest(views.html.createAccount.enrolment_org(errors, "", "")),
      success =>
        for {
          user <- auth.getUserDetails
          groupId <- auth.getGroupId(ctx)
          id      <- addresses.registerAddress(success)
          individual   =  IndividualAccountSubmission(user.externalId, "NONIV", None, IndividualDetails(user.userInfo.firstName.getOrElse(""), user.userInfo.lastName.getOrElse(""), user.userInfo.email, success.phone, None, id))
          _           <-  groupAccounts.create(groupId, id, success, individual)
          personId    <- individualAccounts.withExternalId(user.externalId) //This is used to get the personId back for the group accounts create.
          res         <- resultMapper(personId, id)
        } yield res
    )
  }


  def submitOrg() = gGAction.async(isSession = false) { ctx => implicit request =>
        CreateGroupAccount.form.bindFromRequest().fold(
          errors => BadRequest(views.html.createAccount.enrolment_org(errors, "", "")),
          success =>
            for {
            //Format: OFF
              user        <- auth.userDetails(ctx)
              groupId     <- auth.getGroupId(ctx)
              id          <- addresses.registerAddress(success)
              individual   =  IndividualAccountSubmission(user.externalId, "NONIV", None, IndividualDetails(user.userInfo.firstName.getOrElse(""), user.userInfo.lastName.getOrElse(""), user.userInfo.email, success.phone, None, id))
              _           <- groupExists(groupId, acc => individualAccounts.create(createIndividualAccountSubmission(user, success.phone)(acc)), groupAccounts.create(groupId, id, success, individual)) //If the user create can return the peresonId back we can shorten this function.
              personId    <- individualAccounts.withExternalId(user.externalId) //This is used to get the personId back for the group accounts create.
              res         <- resultMapper(personId, id)
            //Format: ON
            } yield res
        )
  }

  private def resultMapper(
                            option: Option[DetailedIndividualAccount],
                            addressId: Int)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = option match {
    case Some(x) => enrolmentService.enrol(x.individualId, addressId).map{
      case Success => Redirect(routes.CreateEnrolmentUser.success(x.individualId))
      case Failure => InternalServerError(Global.internalServerErrorTemplate)
    }
    case None    => Future.successful(InternalServerError(Global.internalServerErrorTemplate))
  }

  def success(personId: Long) = authenticatedAction { implicit request =>
    Ok(views.html.createAccount.confirmation_enrolment(personId))// Add Page
  }

  private def createIndividualAccountSubmission(userDetails: UserDetails, phoneNumber: String)(groupAccount: GroupAccount) = {
    IndividualAccountSubmission(userDetails.externalId, "NONIV", Some(groupAccount.id), IndividualDetails(userDetails.userInfo.firstName.getOrElse(""), userDetails.userInfo.lastName.getOrElse(""), userDetails.userInfo.email, phoneNumber, None, groupAccount.addressId))
  }

  private def groupExists(groupId: String, groupExists: GroupAccount => Future[Int], noGroup: Future[Long])(implicit hc: HeaderCarrier): Future[Long] = {
    groupAccounts.withGroupId(groupId).flatMap{
      //Format: OFF
      case Some(acc)  => groupExists(acc).map(_.toLong)
      case _          => noGroup
      //Format: ON
    }
  }

  implicit def vm(form: Form[_]): CreateGroupAccountVM = CreateGroupAccountVM(form)
}
