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

package controllers.test

import javax.inject.Inject

import actions.AuthenticatedAction
import controllers.PropertyLinkingController
import models.test.TestUserDetails
import play.api.libs.json.Json
import services.{EnrolmentService, Failure, Success}

class TestUserDetailsController @Inject()(
                                           authenticated: AuthenticatedAction,
                                           enrolmentService: EnrolmentService
                                         ) extends PropertyLinkingController {

  def getUserDetails() = authenticated { implicit request =>
    Ok(Json.toJson(request.organisationAccount.isAgent match {

      case true => TestUserDetails(
        personId = request.individualAccount.individualId,
        organisationId = request.organisationAccount.id,
        organisationName = request.organisationAccount.companyName,
        governmentGatewayGroupId = request.organisationAccount.groupId,
        governmentGatewayExternalId = request.individualAccount.externalId,
        agentCode = Some(request.organisationAccount.agentCode))
      case _ => TestUserDetails(personId = request.individualAccount.individualId,
        organisationId = request.organisationAccount.id,
        organisationName = request.organisationAccount.companyName,
        governmentGatewayGroupId = request.organisationAccount.groupId,
        governmentGatewayExternalId = request.individualAccount.externalId,
        agentCode = None)
    }))
  }

  def deEnrol() = authenticated { implicit request =>
    enrolmentService.deEnrolUser.map{
      case Success => Ok("Successful")
      case Failure => Ok("Failure")
    }
  }

}
