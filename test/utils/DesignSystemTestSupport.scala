/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.govukfrontend.views.Layouts
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukBackLink, GovukButton, GovukDateInput, GovukDetails, GovukErrorSummary, GovukFooter, GovukHeader, GovukInput, GovukInsetText, GovukRadios}
import views.html.{addUserToGG, start}

trait DesignSystemTestSupport extends Layouts {
  //all deprecated classes should be located in this file until DI is introduced

  val mainLayout = new views.html.mainLayout(
    GovukTemplate,
    GovukHeader,
    GovukFooter,
    GovukBackLink,
    GovukDetails,
    new views.html.head())

  val startView = new start(GovukInsetText, GovukDetails, mainLayout)
  val addUsertoGGView = new addUserToGG(mainLayout)
  val assessmentsView = new views.html.dashboard.assessments(mainLayout, GovukDetails)
  val invalidAccountTypeView = new views.html.errors.invalidAccountType(mainLayout, GovukButton)
  val invalidAccountCreationView = new views.html.errors.invalidAccountCreation(mainLayout)

  val registerIndividualView = new views.html.createAccount.register_individual(
    GovukInsetText,
    GovukDetails,
    mainLayout,
    GovukErrorSummary,
    GovukInput,
    GovukDateInput,
    GovukButton)

  val registerOrganisationView =
    new views.html.createAccount.register_organisation(
      GovukButton,
      GovukDateInput,
      GovukInsetText,
      GovukDetails,
      mainLayout,
      GovukErrorSummary,
      GovukInput,
      GovukRadios)

  val registerAssAdminView = new views.html.createAccount.register_assistant_admin(
    GovukInsetText,
    GovukDetails,
    mainLayout,
    GovukErrorSummary,
    GovukInput,
    GovukButton,
    GovukDateInput)

  val registerAssistantView = new views.html.createAccount.register_assistant(
    GovukInsetText,
    GovukDetails,
    mainLayout,
    GovukErrorSummary,
    GovukInput,
    GovukButton)

  val registerConfirmationView =
    new views.html.createAccount.registration_confirmation(GovukInsetText, GovukDetails, mainLayout)

}
