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

import uk.gov.hmrc.govukfrontend.views.html.components._
import views.html.dvr._
import views.html.errors.propertyMissing
import views.html.propertyrepresentation.appoint.appointAgentSummary
import views.html.registration._
import views.html.{addUserToGG, startOldJourney}
import views.html.helpers._
import views.html.propertyLinking._
import views.html._
import views.html.propertyrepresentation.manage._
import views.html.propertyrepresentation._

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
    new dvrFiles(
      mainLayout,
      govukButton,
      govukDetails,
      govukWarningText,
      govukTable,
      govukTabs,
      govukSummaryList,
      govukRadios,
      govukErrorSummary,
      formWithCSRF,
      govukInsetText)
  lazy val cannotRaiseChallengeView = new cannotRaiseChallenge(mainLayout, govukButton)
  lazy val propertyMissingView = new propertyMissing(mainLayout)

  lazy val startView = new start(mainLayout, govukInsetText, govukButton)
  lazy val startViewOld = new startOldJourney(mainLayout, govukInsetText, govukDetails)
  lazy val doYouHaveAccountView =
    new doYouHaveAccount(mainLayout, govukButton, govukRadios, formWithCSRF, govukErrorSummary)
  lazy val accountTypeView = new accountType(mainLayout, govukButton, govukRadios, formWithCSRF, govukErrorSummary)
  lazy val addUsertoGGView = new addUserToGG(mainLayout)
  lazy val assessmentsView = new views.html.dashboard.assessments(mainLayout, govukDetails)
  lazy val invalidAccountTypeView = new views.html.errors.invalidAccountType(mainLayout, govukButton)
  lazy val invalidAccountCreationView = new views.html.errors.invalidAccountCreation(mainLayout)

  lazy val registerIndividualView = new views.html.createAccount.registerIndividual(
    mainLayout,
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
    govukErrorSummary,
    govukInput,
    govukButton,
    dateFields,
    formWithCSRF)

  lazy val registerAssistantView =
    new views.html.createAccount.registerAssistant(mainLayout, govukErrorSummary, govukInput, govukButton, formWithCSRF)

  lazy val registerConfirmationView =
    new views.html.createAccount.registrationConfirmation(mainLayout, govukButton)
  lazy val confirmationView =
    new views.html.createAccount.confirmation(mainLayout, govukInsetText, govukButton, govukPanel)

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
  lazy val occupancyOfPropertyPage = new views.html.propertyLinking.occupancyOfProperty(
    govukErrorSummary,
    govukRadios,
    govukButton,
    dateFields,
    mainLayout,
    formWithCSRF)

  lazy val uploadRatesBillView = new views.html.propertyLinking.uploadRatesBill(
    govukErrorSummary,
    govukWarningText,
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
  lazy val linkingRequestSubmittedView = new linkingRequestSubmitted(govukPanel, govukButton, mainLayout)
  lazy val ownershipToPropertyView = new ownershipToProperty(
    govukErrorSummary,
    govukDetails,
    govukRadios,
    govukButton,
    govukInput,
    dateFields,
    mainLayout,
    formWithCSRF)
  lazy val chooseEvidenceView =
    new chooseEvidence(govukErrorSummary, govukRadios, govukButton, mainLayout, formWithCSRF)
  lazy val relationshipToPropertyView =
    new relationshipToProperty(govukErrorSummary, govukDetails, govukRadios, govukButton, mainLayout, formWithCSRF)
  lazy val myAgentsView = new myAgents(govukTable, mainLayout)
  lazy val manageAgentView = new manageAgent(govukErrorSummary, govukRadios, govukButton, mainLayout, formWithCSRF)
  lazy val removeAgentFromOrganisationView =
    new removeAgentFromOrganisation(govukErrorSummary, govukRadios, govukButton, mainLayout, formWithCSRF)
  lazy val unassignAgentFromPropertyView =
    new unassignAgentFromProperty(govukErrorSummary, govukRadios, govukButton, mainLayout, formWithCSRF)
  lazy val addAgentToAllPropertyView =
    new addAgentToAllProperties(
      govukErrorSummary: GovukErrorSummary,
      govukRadios: GovukRadios,
      govukButton: GovukButton,
      mainLayout: views.html.mainLayout,
      formWithCSRF: FormWithCSRF)
  lazy val confirmAddAgentToAllPropertyView =
    new confirmAddAgentToAllProperties(govukPanel, mainLayout)
  lazy val unassignAgentFromAllPropertiesView =
    new unassignAgentFromAllProperties(
      govukErrorSummary: GovukErrorSummary,
      govukRadios: GovukRadios,
      govukButton: GovukButton,
      mainLayout: views.html.mainLayout,
      formWithCSRF: FormWithCSRF)
  lazy val confirmUnassignAgentFromAllPropertiesView =
    new confirmUnassignAgentFromAllProperties(govukPanel, mainLayout)
  lazy val confirmRemoveAgentFromOrganisationView =
    new confirmRemoveAgentFromOrganisation(govukPanel, mainLayout)
  lazy val manageAgentPropertiesView = new manageAgentProperties(govukTable, govukButton, mainLayout)
  val revokeClientPropertyView = new revokeClient(formWithCSRF, govukButton, mainLayout)
  val confirmRevokeClientPropertyView = new confirmRevokeClientProperty(govukPanel, mainLayout)

}
