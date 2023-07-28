/*
 * Copyright 2023 HM Revenue & Customs
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
import views.html.errors.{alreadySubmitted, error, forbidden, notFound, propertyMissing, technicalDifficulties}
import views.html.propertyrepresentation.appoint.appointAgentSummary
import views.html.registration._
import views.html.{addUserToGG, startOldJourney}
import views.html.helpers._
import views.html.propertyLinking._
import views.html._
import views.html.dvr.tabs.{agentsTab, challengeCasesDetailsTab, checkCasesDetailsTab, startCheckTab, valuationTab}
import views.html.propertyrepresentation.manage._
import views.html.propertyrepresentation._

trait FakeViews extends GdsComponents {

  lazy val govukWrapper = new views.html.govukWrapper(
    govukTemplate,
    govukHeader,
    govukPhaseBanner,
    hmrcStandardFooter,
    hmrcTrackingConsentSnippet,
    hmrcTimeoutDialogHelper
  )

  lazy val mainLayout = new views.html.mainLayout(
    govukWrapper,
    govukDetails,
    new views.html.head()
  )
  lazy val dateFields = new dateFields(govukDateInput: GovukDateInput)
  lazy val alreadyRequestedDetailedValuationView =
    new alreadyRequestedDetailedValuation(mainLayout, govukInsetText, govukSummaryList)
  lazy val requestDetailedValuationView =
    new requestDetailedValuation(mainLayout, govukButton, govukInsetText, govukTabs, formWithCSRF, govukSummaryList)
  lazy val requestedDetailedValuationView = new requestedDetailedValuation(mainLayout, govukPanel, govukSummaryList)
  lazy val agentsTab = new agentsTab(govukTable)
  lazy val challengeCasesDetailsTab = new challengeCasesDetailsTab(govukDetails, govukTable)
  lazy val checkCasesDetailsTab = new checkCasesDetailsTab(govukButton, govukDetails, govukTable, govukWarningText)
  lazy val startCheckTab = new startCheckTab(formWithCSRF, govukButton, govukDetails, govukRadios)
  lazy val valuationTab = new valuationTab(govukButton, govukInsetText, govukWarningText)
  lazy val dvrFilesView =
    new dvrFiles(
      mainLayout,
      agentsTab,
      challengeCasesDetailsTab,
      checkCasesDetailsTab,
      startCheckTab,
      valuationTab,
      govukTabs,
      govukSummaryList,
      govukErrorSummary
    )
  lazy val cannotRaiseChallengeView = new cannotRaiseChallenge(mainLayout, govukButton)
  lazy val propertyMissingView = new propertyMissing(mainLayout)

  lazy val startView = new start(mainLayout, govukInsetText, govukButton)
  lazy val startViewOld = new startOldJourney(mainLayout, govukInsetText, govukDetails)
  lazy val doYouHaveAccountView =
    new doYouHaveAccount(mainLayout, formErrorSummary, govukButton, govukRadios, formWithCSRF)
  lazy val accountTypeView =
    new accountType(mainLayout, formErrorSummary, govukButton, govukRadios, formWithCSRF)
  lazy val addUsertoGGView = new addUserToGG(mainLayout)
  lazy val bulletView = new views.html.components.bullets()
  lazy val naRateableDetailSectionView = new views.html.components.naRateableDetailSection(govukDetails, bulletView)
  lazy val assessmentDetailsView = new views.html.dashboard.assessmentDetails(govukDetails, govukTag)
  lazy val assessmentsView =
    new views.html.dashboard.assessments(mainLayout, assessmentDetailsView, naRateableDetailSectionView)
  lazy val invalidAccountTypeView = new views.html.errors.invalidAccountType(mainLayout, govukButton)
  lazy val invalidAccountCreationView = new views.html.errors.invalidAccountCreation(mainLayout)

  lazy val addressView = new views.html.helpers.address(govukInput, govukButton)

  lazy val registerIndividualView = new views.html.createAccount.registerIndividual(
    addressView,
    mainLayout,
    govukDetails,
    govukErrorSummary,
    govukInput,
    dateFields,
    govukButton,
    formWithCSRF)

  lazy val registerOrganisationView =
    new views.html.createAccount.registerOrganisation(
      addressView,
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
    addressView,
    mainLayout,
    govukErrorSummary,
    govukInput,
    govukButton,
    dateFields,
    formWithCSRF)

  lazy val registerAssistantView =
    new views.html.createAccount.registerAssistant(
      addressView,
      mainLayout,
      govukErrorSummary,
      govukInput,
      govukButton,
      formWithCSRF)

  lazy val registerConfirmationView =
    new views.html.createAccount.registrationConfirmation(mainLayout, govukButton)
  lazy val confirmationView =
    new views.html.createAccount.confirmation(mainLayout, govukButton, govukPanel)

  lazy val revokeAgentSummaryView =
    new views.html.propertyrepresentation.revokeAgentSummary(mainLayout, govukButton, govukPanel)
  lazy val appointAgentSummaryView = new appointAgentSummary(mainLayout, govukPanel)
  lazy val revokeAgentPropertiesView = new views.html.propertyrepresentation.revokeAgentProperties(
    revokedAgentPrivileges,
    mainLayout,
    formWithCSRF,
    govukErrorSummary,
    govukInput,
    govukTable,
    govukButton,
    govukWarningText)

  lazy val appointAgentPropertiesView = new views.html.propertyrepresentation.appoint.appointAgentProperties(
    mainLayout,
    formWithCSRF,
    govukErrorSummary,
    govukInput,
    govukTable,
    govukButton,
    govukSelect,
    govukFieldset)

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

  lazy val formErrorSummary = new views.html.helpers.formErrorSummary(govukErrorSummary)

  lazy val updateBusinessAddressView =
    new views.html.details.updateBusinessAddress(addressView, mainLayout, formErrorSummary, govukButton, formWithCSRF)
  lazy val updateBusinessNameView =
    new views.html.details.updateBusinessName(mainLayout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val updateBusinessPhoneView =
    new views.html.details.updateBusinessPhone(mainLayout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val updateBusinessEmailView =
    new views.html.details.updateBusinessEmail(mainLayout, formErrorSummary, govukButton, govukInput, formWithCSRF)

  lazy val updateAddressView =
    new views.html.details.updateAddress(addressView, mainLayout, formErrorSummary, govukButton, formWithCSRF)
  lazy val updatePhoneView =
    new views.html.details.updatePhone(mainLayout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val updateMobileView =
    new views.html.details.updateMobile(mainLayout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val updateEmailView =
    new views.html.details.updateEmail(mainLayout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val updateNameView =
    new views.html.details.updateName(mainLayout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val termsAndConditionsView = new views.html.createAccount.termsAndConditions(mainLayout)
  lazy val occupancyOfPropertyPage = new views.html.propertyLinking.occupancyOfProperty(
    govukErrorSummary,
    govukRadios,
    govukButton,
    dateFields,
    mainLayout,
    formWithCSRF)

  lazy val uploadRatesBillLeaseOrLicenseView = new views.html.propertyLinking.uploadRatesBillLeaseOrLicense(
    govukButton,
    mainLayout,
    formWithCSRF
  )
  lazy val uploadEvidenceView =
    new views.html.propertyLinking.uploadEvidence(govukErrorSummary, govukRadios, govukButton, mainLayout, formWithCSRF)
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
  lazy val linkingRequestSubmittedView = new linkingRequestSubmitted(govukPanel, mainLayout)
  lazy val ownershipToPropertyView = new ownershipToProperty(
    govukErrorSummary,
    govukDetails,
    govukRadios,
    govukButton,
    dateFields,
    mainLayout,
    formWithCSRF)
  lazy val manageAgentPropertiesViewOld = new manageAgentPropertiesOld(govukTable, mainLayout)
  lazy val myAgentViewOld = new myAgentsOld(govukTable, mainLayout)
  lazy val chooseEvidenceView =
    new chooseEvidence(govukErrorSummary, govukRadios, govukButton, mainLayout, formWithCSRF)
  lazy val chooseOccupierEvidenceView =
    new chooseOccupierEvidence(govukErrorSummary, govukRadios, govukButton, mainLayout, formWithCSRF)
  lazy val claimPropertyStartView =
    new claimPropertyStart(govukSummaryList, govukButton, mainLayout)
  lazy val relationshipToPropertyView =
    new relationshipToProperty(govukErrorSummary, govukDetails, govukRadios, govukButton, mainLayout, formWithCSRF)
  lazy val myAgentsView = new myAgents(govukTable, mainLayout)
  lazy val manageAgentView = new manageAgent(govukErrorSummary, govukRadios, govukButton, mainLayout, formWithCSRF)
  lazy val manageAgentViewOld =
    new manageAgentOld(govukErrorSummary, govukRadios, govukButton, mainLayout, formWithCSRF)
  lazy val removeAgentFromOrganisationView =
    new removeAgentFromOrganisation(govukErrorSummary, govukButton, mainLayout, formWithCSRF)
  lazy val unassignAgentFromPropertyView =
    new unassignAgentFromProperty(revokedAgentPrivileges, govukErrorSummary, govukButton, mainLayout, formWithCSRF)
  lazy val addAgentToAllPropertyView =
    new addAgentToAllProperties(
      govukErrorSummary: GovukErrorSummary,
      govukButton: GovukButton,
      mainLayout: views.html.mainLayout,
      formWithCSRF: FormWithCSRF)
  lazy val confirmAddAgentToAllPropertyView =
    new confirmAddAgentToAllProperties(govukPanel, mainLayout)
  lazy val revokedAgentPrivileges =
    new revokedAgentPrivileges(govukWarningText)
  lazy val unassignAgentFromAllPropertiesView =
    new unassignAgentFromAllProperties(
      revokedAgentPrivileges: revokedAgentPrivileges,
      govukErrorSummary: GovukErrorSummary,
      govukButton: GovukButton,
      govukWarningText: GovukWarningText,
      mainLayout: views.html.mainLayout,
      formWithCSRF: FormWithCSRF
    )
  lazy val confirmUnassignAgentFromAllPropertiesView =
    new confirmUnassignAgentFromAllProperties(govukPanel, mainLayout)
  lazy val confirmRemoveAgentFromOrganisationView =
    new confirmRemoveAgentFromOrganisation(govukPanel, mainLayout)
  lazy val manageAgentPropertiesView = new manageAgentProperties(govukTable, mainLayout)
  val revokeClientPropertyView = new revokeClient(formWithCSRF, govukButton, mainLayout)
  val confirmRevokeClientPropertyView = new confirmRevokeClientProperty(govukPanel, mainLayout)

  lazy val errorView = new error(mainLayout)
  lazy val forbiddenView = new forbidden(mainLayout)
  lazy val technicalDifficultiesView = new technicalDifficulties(mainLayout)
  lazy val notFoundView = new notFound(mainLayout)
  lazy val alreadySubmittedView = new alreadySubmitted(mainLayout)
}
