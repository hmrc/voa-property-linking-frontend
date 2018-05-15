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

package models.enrolment

import java.time.LocalDate

import form.Mappings._
import form.TextMatching
import models.{email => _, _}
import play.api.data.{Form, Mapping}
import play.api.data.Forms._
import play.api.data.validation.Constraints
import uk.gov.hmrc.domain.Nino
import views.helpers.Errors
import auth.VoaAction
import config.ApplicationConfig
import connectors.{IndividualAccounts, VPLAuthConnector}
import controllers.GroupAccountDetails
import form.Mappings._
import form.TextMatching
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.{Form, Mapping}
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import repositories.SessionRepo
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.SessionKeys
import views.helpers.Errors

case class EnrolmentIndividualAccountDetails(firstName: String,
                                             lastName: String,
                                             address: Address,
                                             dob: LocalDate,
                                             nino: Nino,
                                             phone: String,
                                             mobilePhone: String,
                                             email: String,
                                             confirmedEmail: String,
                                             tradingName: Option[String]
                                            ) extends EnrolmentUser {

  override def toIvDetails = IVDetails(
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = Some(dob),
    nino = Some(nino)
  )

  def toGroupDetails = GroupAccountDetails(
    companyName = tradingName.getOrElse("Not Applicable"),
    address = address,
    email = email,
    confirmedEmail = confirmedEmail,
    phone = phone,
    isAgent = false
  )
}