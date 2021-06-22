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
import uk.gov.hmrc.govukfrontend.views.html.components._
import uk.gov.hmrc.hmrcfrontend.views.html.helpers._
import views.html.propertyrepresentation.appoint.{appointAgentProperties, appointAgentSummary}
import views.html.{addUserToGG, start}

trait FakeViews extends Layouts {
  //all deprecated classes should be located in this file until DI is introduced

  val mainLayout = new views.html.mainLayout(
    govukTemplate = GovukTemplate,
    govukHeader = GovukHeader,
    govukFooter = GovukFooter,
    govukBackLink = GovukBackLink,
    govukDetails = GovukDetails,
    govukPhaseBanner = GovukPhaseBanner,
    hmrcStandardFooter = HmrcStandardFooter,
    hmrcTrackingConsentSnippet = HmrcTrackingConsentSnippet,
    head = new views.html.head()
  )

  val startView = new start(GovukInsetText, GovukDetails, mainLayout)
  val addUsertoGGView = new addUserToGG(mainLayout)
  val assessmentsView = new views.html.dashboard.assessments(mainLayout, GovukDetails)
  val invalidAccountTypeView = new views.html.errors.invalidAccountType(mainLayout, GovukButton)
  val invalidAccountCreationView = new views.html.errors.invalidAccountCreation(mainLayout)

  val registerIndividualView = new views.html.createAccount.register_individual(
    mainLayout,
    GovukInsetText,
    GovukDetails,
    GovukErrorSummary,
    GovukInput,
    GovukDateInput,
    GovukButton)

  val registerOrganisationView =
    new views.html.createAccount.register_organisation(
      mainLayout,
      GovukButton,
      GovukDateInput,
      GovukInsetText,
      GovukDetails,
      GovukErrorSummary,
      GovukInput,
      GovukRadios)

  val registerAssistantAdminView = new views.html.createAccount.register_assistant_admin(
    mainLayout,
    GovukInsetText,
    GovukDetails,
    GovukErrorSummary,
    GovukInput,
    GovukButton,
    GovukDateInput)

  val registerAssistantView = new views.html.createAccount.register_assistant(
    mainLayout,
    GovukInsetText,
    GovukDetails,
    GovukErrorSummary,
    GovukInput,
    GovukButton)

  val registerConfirmationView =
    new views.html.createAccount.registration_confirmation(mainLayout, GovukInsetText, GovukDetails, GovukButton)

  val revokeAgentSummaryView = new views.html.propertyrepresentation.revokeAgentSummary(mainLayout, GovukButton)
  val appointAgentSummaryView = new appointAgentSummary(mainLayout, GovukButton)
  val appointAgentPropertiesView = new appointAgentProperties(GovukTable, GovukButton, mainLayout, GovukErrorSummary)
}
