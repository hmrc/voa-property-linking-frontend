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

import config.ApplicationConfig
import controllers.PropertyLinkingController
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms.single
import play.api.mvc._
import views.html._
import form.Mappings._

import javax.inject.Inject
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.ExecutionContext

class AccountTypeController @Inject()(
      val errorHandler: CustomErrorHandler,
      accountTypeView: registration.accountType
)(
      implicit executionContext: ExecutionContext,
      val config: ApplicationConfig,
      implicit override val controllerComponents: MessagesControllerComponents
) extends PropertyLinkingController with Logging {

  def continue(accountType: String): Map[String, Seq[String]] =
    Map("accountType" -> Seq(accountType), "continue" -> Seq(config.dashboardUrl("home")), "origin" -> Seq("voa"))

  def show(): Action[AnyContent] = Action { implicit request =>
    Ok(accountTypeView(AccountTypeIndividual.form))
  }

  def submit(): Action[AnyContent] = Action { implicit request =>
    AccountTypeIndividual.form
      .bindFromRequest()
      .fold(
        errors => BadRequest(accountTypeView(errors)),
        accountTypeIndividual =>
          if (accountTypeIndividual) Redirect(config.ggRegistrationUrl, continue("individual"))
          else Redirect(config.ggRegistrationUrl, continue("organisation"))
      )
  }

  object AccountTypeIndividual {
    lazy val form = Form(single("accountTypeIndividual" -> mandatoryBoolean))
  }

}
