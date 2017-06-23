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

package views.details

import controllers.ControllerSpec
import models.{Address, DetailedIndividualAccount, GroupAccount, IndividualDetails}
import play.api.test.FakeRequest
import utils.HtmlPage
import play.api.i18n.Messages.Implicits._
import resources._

class DetailsPageSpec extends ControllerSpec {

  behavior of "DetailsPage"

  it should "Allow the user to update personal name/email/addess/telephone" in {
    val individualAccount = DetailedIndividualAccount(
      "bbb", "aaa", 123, 123,
      IndividualDetails("firstName", "lastName", "email@email.com", "phone1", None, 100))
    val groupAccount = GroupAccount(123, "groupId", "companyName", 100, "email@email.com", "123", false, false, 123)
    val address = Address(Some(100), "line1", "line2", "line3", "line4", "BN1 1NA")
    implicit val request = FakeRequest()
    val actualHtml = views.html.details.details(individualAccount, groupAccount, address)
    val html = HtmlPage(actualHtml)
    html.mustContainLink("#personalName", controllers.routes.DetailsController.personalName().url)
    html.mustContainLink("#personalEmail", controllers.routes.DetailsController.personalEmail().url)
    html.mustContainLink("#personalAddress", controllers.routes.DetailsController.personalAddress().url)
    html.mustContainLink("#personalTelephone", controllers.routes.DetailsController.personalTelephone().url)
  }

}
