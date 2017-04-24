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

package views.dashboard

import actions.BasicAuthenticatedRequest
import controllers.{ControllerSpec, ManagePropertiesVM, Pagination}
import models.{DetailedIndividualAccount, GroupAccount, PropertyLink}
import play.api.test.FakeRequest
import org.scalacheck.Arbitrary.arbitrary
import resources._
import utils.HtmlPage
import play.api.i18n.Messages.Implicits._

class ManagePropertiesPageSpec extends ControllerSpec {

  implicit val request = FakeRequest()
  val organisationAccount = arbitrary[GroupAccount]
  val individualAccount = arbitrary[DetailedIndividualAccount]
  implicit val basicAuthenticatedRequest = BasicAuthenticatedRequest(organisationAccount, individualAccount, request)

  "Manage properties page" should "show the submissionId if the property link is pending" in {

    val pendingProp = arbitrary[PropertyLink].sample.get.copy(
        organisationId = organisationAccount.id,
        pending = true
      )
    val approvedProp = arbitrary[PropertyLink].sample.get.copy(
      organisationId = organisationAccount.id,
      pending = false
    )
    val html = views.html.dashboard.manageProperties(ManagePropertiesVM(organisationAccount.id, Seq(pendingProp, approvedProp), Pagination(1, 25, 25)))
    val page = HtmlPage(html)
    page.mustContain1("#submissionId")
  }

}
