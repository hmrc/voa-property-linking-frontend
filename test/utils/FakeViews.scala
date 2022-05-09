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

package utils

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.html.components.{FormWithCSRF, GovukButton, GovukDateInput, GovukDetails, GovukErrorSummary, GovukRadios}
import uk.gov.hmrc.hmrcfrontend.views.html.helpers._
import views.html.createAccount.confirmation
import views.html.dvr.{alreadyRequestedDetailedValuation, cannotRaiseChallenge, dvrFiles, requestDetailedValuation, requestedDetailedValuation}
import views.html.errors.propertyMissing
import views.html.propertyrepresentation.appoint.appointAgentSummary
import views.html.registration._
import views.html.{addUserToGG, startOldJourney}
import views.html.helpers._

trait FakeViews extends GdsComponents {

  lazy val mainLayout = new views.html.mainLayout(
    govukTemplate = govukTemplate,
    govukHeader = govukHeader,
    govukFooter = govukFooter,
    govukBackLink = govukBackLink,
    govukDetails = govukDetails,
    govukPhaseBanner = govukPhaseBanner,
    hmrcStandardFooter = hmrcStandardFooter,
    hmrcTrackingConsentSnippet = hmrcTrackingConsentSnippet,
    head = new views.html.head()
  )
  lazy val dateFields = new dateFields(govukDateInput: GovukDateInput)
  lazy val alreadyRequestedDetailedValuationView = new alreadyRequestedDetailedValuation(mainLayout)
  lazy val requestDetailedValuationView = new requestDetailedValuation(mainLayout, govukButton, formWithCSRF)
  lazy val requestedDetailedValuationView = new requestedDetailedValuation(mainLayout)
  lazy val dvrFilesView =
    new dvrFiles(mainLayout, govukButton, govukDetails, govukWarningText, govukTable, govukTabs, govukSummaryList)
  lazy val cannotRaiseChallengeView = new cannotRaiseChallenge(mainLayout, govukButton)
  lazy val propertyMissingView = new propertyMissing(mainLayout)

  lazy val startView = new start(mainLayout, govukInsetText, govukDetails, govukButton)
  lazy val startViewOld = new startOldJourney(mainLayout, govukInsetText, govukDetails)
  lazy val doYouHaveAccountView = new doYouHaveAccount(
    mainLayout,
    govukInsetText,
    govukDetails,
    govukButton,
    govukRadios,
    formWithCSRF,
    govukErrorSummary)
  lazy val accountTypeView =
    new accountType(mainLayout, govukInsetText, govukDetails, govukButton, govukRadios, formWithCSRF, govukErrorSummary)
  lazy val addUsertoGGView = new addUserToGG(mainLayout)
  lazy val assessmentsView = new views.html.dashboard.assessments(mainLayout, govukDetails)
  lazy val invalidAccountTypeView = new views.html.errors.invalidAccountType(mainLayout, govukButton)
  lazy val invalidAccountCreationView = new views.html.errors.invalidAccountCreation(mainLayout)

  lazy val registerIndividualView = new views.html.createAccount.registerIndividual(
    mainLayout,
    govukInsetText,
    govukDetails,
    govukErrorSummary,
    govukInput,
    dateFields,
    govukButton,
    formWithCSRF)

  lazy val registerOrganisationView =
    new views.html.createAccount.registerOrganisation(
      mainLayout,
      govukButton,
      dateFields,
      govukInsetText,
      govukDetails,
      govukErrorSummary,
      govukInput,
      govukRadios,
      formWithCSRF)

  lazy val registerAssistantAdminView = new views.html.createAccount.registerAssistantAdmin(
    mainLayout,
    govukInsetText,
    govukDetails,
    govukErrorSummary,
    govukInput,
    govukButton,
    dateFields,
    formWithCSRF)

  lazy val registerAssistantView = new views.html.createAccount.registerAssistant(
    mainLayout,
    govukInsetText,
    govukDetails,
    govukErrorSummary,
    govukInput,
    govukButton,
    formWithCSRF)

  lazy val registerConfirmationView =
    new views.html.createAccount.registrationConfirmation(mainLayout, govukInsetText, govukDetails, govukButton)
  lazy val confirmationView =
    new views.html.createAccount.confirmation(mainLayout, govukInsetText, govukDetails, govukButton, govukPanel)

  lazy val revokeAgentSummaryView =
    new views.html.propertyrepresentation.revokeAgentSummary(mainLayout, govukButton, govukPanel)
  lazy val appointAgentSummaryView = new appointAgentSummary(mainLayout, govukButton, govukPanel)
  lazy val revokeAgentPropertiesView = new views.html.propertyrepresentation.revokeAgentProperties(
    mainLayout,
    formWithCSRF,
    govukErrorSummary,
    govukInput,
    govukTable,
    govukButton)

  lazy val appointAgentPropertiesView = new views.html.propertyrepresentation.appoint.appointAgentProperties(
    mainLayout,
    formWithCSRF,
    govukErrorSummary,
    govukInput,
    govukTable,
    govukButton,
    govukSelect)

  lazy val startPageView = new views.html.propertyrepresentation.appoint.start(
    govukErrorSummary,
    govukInput,
    govukButton,
    mainLayout,
    formWithCSRF)
  lazy val isTheCorrectAgentView = new views.html.propertyrepresentation.appoint.isThisYourAgent(
    govukErrorSummary,
    govukRadios,
    govukButton,
    mainLayout,
    formWithCSRF)
  lazy val agentToManageOnePropertyView = new views.html.propertyrepresentation.appoint.agentToManageOneProperty(
    govukErrorSummary,
    govukRadios,
    govukButton,
    mainLayout,
    formWithCSRF)
  lazy val agentToManageMultiplePropertiesView =
    new views.html.propertyrepresentation.appoint.agentToManageMultipleProperties(
      govukErrorSummary,
      govukRadios,
      govukButton,
      mainLayout,
      formWithCSRF)
  lazy val addAgentconfirmationView = new views.html.propertyrepresentation.appoint.confirmation(govukPanel, mainLayout)
  lazy val checkYourAnswersView = new views.html.propertyrepresentation.appoint.checkYourAnswers(
    govukErrorSummary,
    govukButton,
    govukTable,
    mainLayout,
    formWithCSRF)

  lazy val updateBusinessAddressView =
    new views.html.details.updateBusinessAddress(mainLayout, govukButton, govukInput, formWithCSRF, govukErrorSummary)
  lazy val updateBusinessNameView =
    new views.html.details.updateBusinessName(mainLayout, govukButton, govukInput, formWithCSRF, govukErrorSummary)
  lazy val updateBusinessPhoneView =
    new views.html.details.updateBusinessPhone(mainLayout, govukButton, govukInput, formWithCSRF, govukErrorSummary)
  lazy val updateBusinessEmailView =
    new views.html.details.updateBusinessEmail(mainLayout, govukButton, govukInput, formWithCSRF, govukErrorSummary)

  lazy val updateAddressView =
    new views.html.details.updateAddress(mainLayout, govukButton, govukInput, formWithCSRF, govukErrorSummary)
  lazy val updatePhoneView =
    new views.html.details.updatePhone(mainLayout, govukButton, govukInput, formWithCSRF, govukErrorSummary)
  lazy val updateMobileView =
    new views.html.details.updateMobile(mainLayout, govukButton, govukInput, formWithCSRF, govukErrorSummary)
  lazy val updateEmailView =
    new views.html.details.updateEmail(mainLayout, govukButton, govukInput, formWithCSRF, govukErrorSummary)
  lazy val updateNameView =
    new views.html.details.updateName(mainLayout, govukButton, govukInput, formWithCSRF, govukErrorSummary)
  lazy val managedByAgentsPropertiesView = new views.html.dashboard.managedByAgentsProperties(mainLayout)
  lazy val termsAndConditionsView = new views.html.createAccount.termsAndConditions(mainLayout)
  lazy val occupanyOfPropertyPage = new views.html.propertyLinking.occupancyOfProperty(
    govukErrorSummary: GovukErrorSummary,
    govukDetails: GovukDetails,
    govukRadios: GovukRadios,
    govukButton: GovukButton,
    dateFields: dateFields,
    mainLayout: views.html.mainLayout,
    formWithCSRF: FormWithCSRF
  )

  lazy val uploadRatesBillView = new views.html.propertyLinking.uploadRatesBill(
    govukErrorSummary,
    govukWarningText,
    govukSelect,
    govukDetails,
    govukButton,
    mainLayout,
    formWithCSRF)
  lazy val uploadEvidenceView = new views.html.propertyLinking.uploadEvidence(
    govukErrorSummary,
    govukRadios,
    govukButton,
    mainLayout,
    formWithCSRF,
    govukWarningText)
  lazy val cannotProvideEvidenceView = new views.html.propertyLinking.cannotProvideEvidence(mainLayout)

  lazy val declarationView = new views.html.propertyLinking.declaration(
    govukErrorSummary,
    govukWarningText,
    govukCheckboxes,
    govukButton,
    mainLayout,
    formWithCSRF,
    govukSummaryList
  )

  lazy val myAgentsView = new views.html.propertyrepresentation.manage.myAgents(govukTable, mainLayout)

}
