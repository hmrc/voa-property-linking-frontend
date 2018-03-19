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

package utils

import models.enrolment.{UserDetails, UserInfo}
import play.api.libs.json.Json
import play.api.mvc.Session
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole}

object SessionHelpers {

  val key = new {
    val externalId = "externalId"
    val credId = "credId"
    val firstName = "firstName"
    val lastName = "lastName"
    val email = "email"
    val postcode = "postcode"
    val groupId = "groupId"
    val affinityGroup = "affinityGroup"
    val role = "role"
  }

  implicit class SessionOps(session: Session) {

    def getUserDetails: Option[UserDetails] = {
      (session.get(key.externalId), session.get(key.externalId), session.get(key.firstName), session.get(key.lastName), session.get(key.email), session.get(key.postcode), session.get(key.groupId), session.get(key.affinityGroup), session.get(key.role)) match {
        case (Some(externalId), Some(credId), firstName, lastName, Some(email), postcode, Some(groupId), Some(affinityGroup), Some(role)) =>
          (Json.parse(affinityGroup).\("affinityGroup").asOpt[AffinityGroup], Json.parse(role).\("credentialRole").asOpt[CredentialRole]) match {
            case (Some(aff), Some(r)) => Some(UserDetails(externalId, UserInfo(firstName, lastName, email, postcode, groupId, credId, aff, r)))
          }
        case _ =>
          None
      }
    }

    def putUserDetails(userDetails: UserDetails) = {
      session
        .+(key.externalId -> userDetails.externalId)
        .+(key.credId -> userDetails.userInfo.gatewayId)
        .+(key.firstName -> userDetails.userInfo.firstName.getOrElse(""))
        .+(key.lastName -> userDetails.userInfo.lastName.getOrElse(""))
        .+(key.email -> userDetails.userInfo.email)
        .+(key.postcode -> userDetails.userInfo.postcode.getOrElse(""))
        .+(key.groupId -> userDetails.userInfo.groupIdentifier)
        .+(key.affinityGroup -> userDetails.userInfo.affinityGroup.toJson.toString)
        .+(key.role -> userDetails.userInfo.credentialRole.toJson.toString)
    }

    def removeUserDetails = {
      session
        .-(key.externalId)
        .-(key.credId)
        .-(key.firstName)
        .-(key.lastName)
        .-(key.email)
        .-(key.postcode)
        .-(key.groupId)
        .-(key.affinityGroup)
        .-(key.role)
    }
  }

}
