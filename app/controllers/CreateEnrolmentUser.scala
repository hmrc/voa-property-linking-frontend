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

import actions.EnrolmentService
import auth.GGAction
import connectors.{Addresses, GroupAccounts, IndividualAccounts, VPLAuthConnector}
import models._
import play.api.data.Form
import play.api.mvc.Result
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}

import scala.concurrent.Future

class CreateEnrolmentUser(
                         gGAction: GGAction,
                         groupAccounts: GroupAccounts,
                         individualAccounts: IndividualAccounts,
                         enrolmentService: EnrolmentService,
                         auth: VPLAuthConnector,
                         addresses: Addresses
                         ) extends PropertyLinkingController {


  def showEnrolmentPage() = gGAction.async(isSession = true) { ctx =>implicit request =>
    auth.userDetails(ctx).map{ x =>
      x.userInfo.affinityGroup match {
        case Individual =>
          Ok("TO COME")
        case Organisation =>
          Ok(views.html.createAccount.enrolment_org(CreateGroupAccount.form, x.userInfo.postcode, x.userInfo.email))
      }
    }
  }

  def showEnrolmentPageIndiv()

  def submitOrg = gGAction.async(isSession = false) { ctx =>implicit request =>
        CreateGroupAccount.form.bindFromRequest().fold(
          errors => ,
          success =>
            for {
            //Format: OFF
              user        <- auth.userDetails(ctx)
              groupId     <- auth.getGroupId(ctx)
              externalId  <- auth.getExternalId(ctx)
              id          <- addresses.registerAddress(success)
              individual   =  IndividualAccountSubmission(externalId, "NONIV", None, IndividualDetails(user.userInfo.firstName, user.userInfo.lastName, user.userInfo.email, success.phone, None, id))
              _           <- isExist(groupId, acc => individualAccounts.create(individual), groupAccounts.create(groupId, id, success, individual))
              personId    <- individualAccounts.withExternalId(externalId)
              res         <- x(personId, id)
            //Format: ON
            } yield res
        )
  }

  private def x(option: Option[DetailedIndividualAccount], addressId: Int) = option match {
    case Some(x) => enrolmentService.enrol(x.individualId, addressId).map(_ => Redirect(routes.CreateEnrolmentUser.success()))
    case None    => throw IllegalArgumentException
  }

  private def isExist(groupId: String, groupExists: GroupAccount => Future[Int], noGroup: Future[Long]) = {
    groupAccounts.withGroupId(groupId).map{
      //Format: OFF
      case Some(acc)  => groupExists(acc)
      case _          => noGroup
      //Format: ON
    }
  }

  def submitIndiv = ???


  implicit def vm(form: Form[_]): CreateGroupAccountVM = CreateGroupAccountVM(form)
}
