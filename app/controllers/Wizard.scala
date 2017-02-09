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

import config.Wiring
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import form.Mappings._
import uk.gov.voa.play.form.ConditionalMappings._

object Wizard extends Controller {

  def authenticationWizard() = Action { implicit request =>
    Ok(views.html.authenticationWizard.authenticationWizard(wizardForm))
  }

  def submit() = Action { implicit request =>
      wizardForm.bindFromRequest.fold(
          errors => BadRequest(views.html.authenticationWizard.authenticationWizard(errors)),
          data => redirectToStart(data)
      )
  }

  private def redirectToStart(options: WizardOptions) = options match {
      case WizardOptions(true, Some(true), _) => Redirect(routes.Wizard.beforeYouStartA)
      case WizardOptions(true, Some(false), _) => Redirect(routes.Wizard.beforeYouStartB)
      case WizardOptions(false, _, Some(true)) => Redirect(routes.Wizard.registerExistingGG)
      case WizardOptions(false, _, Some(false)) => Redirect(routes.Wizard.beforeYouStartD)
  }



  def beforeYouStartA() = Action { implicit request =>
      Ok(views.html.authenticationWizard.beforeYouStartA())
  }

  def beforeYouStartB() = Action { implicit request =>
      Ok(views.html.authenticationWizard.beforeYouStartB())
  }

  def registerExistingGG() = Action { implicit request =>
      Ok(views.html.authenticationWizard.registerExistingGG(useExistingAccountForm))
  }

  def beforeYouStartC() = Action { implicit request =>
      Ok(views.html.authenticationWizard.beforeYouStartC())
  }

  def beforeYouStartD() = Action { implicit request =>
      Ok(views.html.authenticationWizard.beforeYouStartD())
  }

  def submitExistingGG() = Action { implicit request =>
      useExistingAccountForm.bindFromRequest.fold(
          errors => BadRequest(views.html.authenticationWizard.registerExistingGG(errors)),
          useExisting => if (useExisting) {
              Redirect(routes.Wizard.beforeYouStartC)
          } else {
              Redirect(routes.Wizard.beforeYouStartD)
          }
      )
  }

  lazy val wizardForm = Form(mapping(
    keys.businessHasRegistered -> mandatoryBoolean,
    keys.givenGGDetails -> mandatoryIfTrue(keys.businessHasRegistered, mandatoryBoolean),
    keys.existingGGAccount -> mandatoryIfFalse(keys.businessHasRegistered, mandatoryBoolean)
  )(WizardOptions.apply)(WizardOptions.unapply))

  lazy val keys = new {
      val businessHasRegistered = "businessHasRegistered"
      val givenGGDetails = "givenGGDetails"
      val existingGGAccount = "existingGGAccount"
      val useExistingGGAccount = "useExistingGGAccount"
  }

  lazy val useExistingAccountForm = Form(single(keys.useExistingGGAccount -> mandatoryBoolean))
}

case class WizardOptions(businessHasRegistered: Boolean, givenGGDetails: Option[Boolean], existingGGAccount: Option[Boolean])
