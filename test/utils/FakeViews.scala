/*
 * Copyright 2024 HM Revenue & Customs
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
import views.html.errors.{alreadySubmitted, detailedValuationError, error, forbidden, notFound, propertyMissing, technicalDifficulties}
import views.html.propertyrepresentation.appoint.{agentCode, appointAgentSummary}
import views.html.registration._
import views.html.{addUserToGG, startOldJourney}
import views.html.helpers._
import views.html.propertyLinking._
import views.html._
import views.html.dvr.tabs.{agentsTab, challengeCasesDetailsTab, checkCasesDetailsTab, comparablePropertiesTab, requestFutureValuationTab, startCheckTab, valuationTab}
import views.html.propertyrepresentation.manage._
import views.html.propertyrepresentation._

trait FakeViews extends GdsComponents {

  lazy val navigationBar = new navigationBar(hmrcLanguageSelectHelper)

  lazy val layout = new layout(
    hmrcStandardPage,
    navigationBar,
    govukDetails,
    hmrcTrackingConsentSnippet,
    hmrcTimeoutDialogHelper,
    standardBetaBanner,
    contentLayout,
    govukNotificationBanner
  )

  lazy val dateFields = new dateFields(govukDateInput: GovukDateInput)
  lazy val alreadyRequestedDetailedValuationView =
    new alreadyRequestedDetailedValuation(layout, govukInsetText, govukSummaryList, govukPanel)
  lazy val requestDetailedValuationView =
    new requestDetailedValuation(
      layout,
      govukButton,
      govukInsetText,
      govukTabs,
      formWithCSRF,
      govukSummaryList,
      requestFutureValuationTabView,
      govukPanel
    )
  lazy val requestedDetailedValuationView =
    new requestedDetailedValuation(layout, govukPanel, govukSummaryList)
  lazy val requestFutureValuationTabView = new requestFutureValuationTab(govukInsetText, govukPanel)
  lazy val agentsTab = new agentsTab(govukTable)
  lazy val challengeCasesDetailsTab = new challengeCasesDetailsTab(govukDetails, govukTable)
  lazy val checkCasesDetailsTab = new checkCasesDetailsTab(govukButton, govukDetails, govukTable, govukWarningText)
  lazy val startCheckTab = new startCheckTab(formWithCSRF, govukButton, govukDetails, govukRadios)
  lazy val valuationTab = new valuationTab(govukButton, govukInsetText, govukWarningText, govukPanel)
  lazy val comparablePropertiesTab = new comparablePropertiesTab()
  lazy val dvrFilesView =
    new dvrFiles(
      layout,
      agentsTab,
      challengeCasesDetailsTab,
      checkCasesDetailsTab,
      startCheckTab,
      valuationTab,
      comparablePropertiesTab,
      govukTabs,
      govukSummaryList,
      govukErrorSummary
    )
  lazy val cannotRaiseChallengeView = new cannotRaiseChallenge(layout, govukButton)
  lazy val propertyMissingView = new propertyMissing(layout)

  lazy val startView = new start(layout, govukInsetText, govukButton)
  lazy val startViewOld = new startOldJourney(layout, govukInsetText, govukDetails)
  lazy val doYouHaveAccountView =
    new doYouHaveAccount(layout, formErrorSummary, govukButton, govukRadios, formWithCSRF)
  lazy val accountTypeView =
    new accountType(layout, formErrorSummary, govukButton, govukRadios, formWithCSRF)
  lazy val addUsertoGGView = new addUserToGG(layout)
  lazy val bulletView = new views.html.components.bullets()
  lazy val naRateableDetailSectionView = new views.html.components.naRateableDetailSection(govukDetails, bulletView)
  lazy val assessmentDetailsWithRatingListView =
    new views.html.dashboard.assessmentDetailsWithRatingList(govukDetails, govukTag, govukTable)
  lazy val assessmentsView =
    new views.html.dashboard.assessments(
      layout,
      assessmentDetailsWithRatingListView,
      naRateableDetailSectionView
    )
  lazy val invalidAccountTypeView = new views.html.errors.invalidAccountType(layout, govukButton)
  lazy val invalidAccountCreationView = new views.html.errors.invalidAccountCreation(layout)

  lazy val addressView = new views.html.helpers.address(govukInput, govukButton)

  lazy val registerIndividualView = new views.html.createAccount.registerIndividual(
    addressView,
    layout,
    govukDetails,
    govukErrorSummary,
    govukInput,
    dateFields,
    govukButton,
    formWithCSRF
  )

  lazy val registerOrganisationView =
    new views.html.createAccount.registerOrganisation(
      addressView,
      layout,
      govukButton,
      dateFields,
      govukInsetText,
      govukDetails,
      govukErrorSummary,
      govukInput,
      govukRadios,
      formWithCSRF
    )

  lazy val registerAssistantAdminView = new views.html.createAccount.registerAssistantAdmin(
    addressView,
    layout,
    govukErrorSummary,
    govukInput,
    govukButton,
    dateFields,
    formWithCSRF
  )

  lazy val registerAssistantView =
    new views.html.createAccount.registerAssistant(
      addressView,
      layout,
      govukErrorSummary,
      govukInput,
      govukButton,
      formWithCSRF
    )

  lazy val registerConfirmationView =
    new views.html.createAccount.registrationConfirmation(layout, govukButton)
  lazy val confirmationView =
    new views.html.createAccount.confirmation(layout, govukButton, govukPanel)

  lazy val revokeAgentSummaryView =
    new views.html.propertyrepresentation.revokeAgentSummary(layout, govukButton, govukPanel)
  lazy val appointAgentSummaryView = new appointAgentSummary(layout, govukPanel)
  lazy val revokeAgentPropertiesView = new views.html.propertyrepresentation.revokeAgentProperties(
    revokedAgentPrivileges,
    layout,
    formWithCSRF,
    govukErrorSummary,
    govukInput,
    govukTable,
    govukButton,
    govukWarningText
  )

  lazy val appointAgentPropertiesView = new views.html.propertyrepresentation.appoint.appointAgentProperties(
    layout,
    formWithCSRF,
    govukErrorSummary,
    govukInput,
    govukTable,
    govukButton,
    govukSelect,
    govukFieldset
  )

  lazy val startPageView = new views.html.propertyrepresentation.appoint.start(govukButton, layout)
  lazy val agentCodePageView = new agentCode(
    govukErrorSummary,
    govukInput,
    govukButton,
    layout,
    formWithCSRF
  )
  lazy val isTheCorrectAgentView = new views.html.propertyrepresentation.appoint.isThisYourAgent(
    govukErrorSummary,
    govukRadios,
    govukButton,
    layout,
    formWithCSRF
  )
  lazy val agentToManageOnePropertyView = new views.html.propertyrepresentation.appoint.agentToManageOneProperty(
    govukErrorSummary,
    govukRadios,
    govukButton,
    layout,
    formWithCSRF
  )
  lazy val agentToManageMultiplePropertiesView =
    new views.html.propertyrepresentation.appoint.agentToManageMultipleProperties(
      govukErrorSummary,
      govukRadios,
      govukButton,
      layout,
      formWithCSRF
    )
  lazy val addAgentconfirmationView = new views.html.propertyrepresentation.appoint.confirmation(govukPanel, layout)
  lazy val checkYourAnswersView = new views.html.propertyrepresentation.appoint.checkYourAnswers(
    govukErrorSummary,
    govukButton,
    govukTable,
    layout,
    formWithCSRF,
    govukSummaryList
  )

  lazy val formErrorSummary = new views.html.helpers.formErrorSummary(govukErrorSummary)

  lazy val updateBusinessAddressView =
    new views.html.details.updateBusinessAddress(addressView, layout, formErrorSummary, govukButton, formWithCSRF)
  lazy val updateBusinessNameView =
    new views.html.details.updateBusinessName(layout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val updateBusinessPhoneView =
    new views.html.details.updateBusinessPhone(layout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val updateBusinessEmailView =
    new views.html.details.updateBusinessEmail(layout, formErrorSummary, govukButton, govukInput, formWithCSRF)

  lazy val updateAddressView =
    new views.html.details.updateAddress(addressView, layout, formErrorSummary, govukButton, formWithCSRF)
  lazy val updatePhoneView =
    new views.html.details.updatePhone(layout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val updateMobileView =
    new views.html.details.updateMobile(layout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val updateEmailView =
    new views.html.details.updateEmail(layout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val updateNameView =
    new views.html.details.updateName(layout, formErrorSummary, govukButton, govukInput, formWithCSRF)
  lazy val termsAndConditionsView = new views.html.createAccount.termsAndConditions(layout)
  lazy val occupancyOfPropertyPage = new views.html.propertyLinking.occupancyOfProperty(
    govukErrorSummary,
    govukRadios,
    govukButton,
    dateFields,
    layout,
    formWithCSRF
  )

  lazy val uploadEvidenceView =
    new views.html.propertyLinking.uploadEvidence(govukErrorSummary, govukRadios, govukButton, layout, formWithCSRF)
  lazy val uploadView =
    new views.html.propertyLinking.upload(govukButton, govukFileUpload, layout, formWithCSRF, govukErrorSummary)
  lazy val uploadResultView =
    new views.html.propertyLinking.upload_result(
      govukTag,
      govukSummaryList,
      layout,
      formWithCSRF,
      govukButton,
      govukErrorSummary
    )
  lazy val cannotProvideEvidenceView = new views.html.propertyLinking.cannotProvideEvidence(layout)

  lazy val declarationView = new views.html.propertyLinking.declaration(
    govukErrorSummary,
    govukWarningText,
    govukCheckboxes,
    govukButton,
    layout,
    formWithCSRF,
    govukSummaryList
  )
  lazy val linkingRequestSubmittedView = new linkingRequestSubmitted(govukPanel, layout)
  lazy val ownershipToPropertyView = new ownershipToProperty(
    govukErrorSummary,
    govukDetails,
    govukRadios,
    govukButton,
    dateFields,
    layout,
    formWithCSRF
  )
  lazy val chooseEvidenceView =
    new chooseEvidence(govukErrorSummary, govukRadios, govukButton, layout, formWithCSRF)
  lazy val chooseOccupierEvidenceView =
    new chooseOccupierEvidence(govukErrorSummary, govukRadios, govukButton, layout, formWithCSRF)
  lazy val claimPropertyStartView =
    new claimPropertyStart(govukSummaryList, govukButton, layout)
  lazy val relationshipToPropertyView =
    new relationshipToProperty(govukErrorSummary, govukDetails, govukRadios, govukButton, layout, formWithCSRF)
  lazy val myAgentsView = new myAgents(govukTable, layout)
  lazy val manageAgentView = new manageAgent(govukErrorSummary, govukRadios, govukButton, layout, formWithCSRF)
  lazy val removeAgentFromOrganisationView =
    new removeAgentFromOrganisation(govukErrorSummary, govukButton, layout, formWithCSRF)
  lazy val unassignAgentFromPropertyView =
    new unassignAgentFromProperty(revokedAgentPrivileges, govukErrorSummary, govukButton, layout, formWithCSRF)
  lazy val addAgentToAllPropertyView =
    new addAgentToAllProperties(
      govukErrorSummary: GovukErrorSummary,
      govukButton: GovukButton,
      layout: layout,
      formWithCSRF: FormWithCSRF
    )
  lazy val confirmAddAgentToAllPropertyView =
    new confirmAddAgentToAllProperties(govukPanel, layout)
  lazy val revokedAgentPrivileges =
    new revokedAgentPrivileges(govukWarningText)
  lazy val unassignAgentFromAllPropertiesView =
    new unassignAgentFromAllProperties(
      revokedAgentPrivileges: revokedAgentPrivileges,
      govukErrorSummary: GovukErrorSummary,
      govukButton: GovukButton,
      govukWarningText: GovukWarningText,
      layout: layout,
      formWithCSRF: FormWithCSRF
    )
  lazy val confirmUnassignAgentFromAllPropertiesView =
    new confirmUnassignAgentFromAllProperties(govukPanel, layout)
  lazy val confirmRemoveAgentFromOrganisationView =
    new confirmRemoveAgentFromOrganisation(govukPanel, layout)
  lazy val manageAgentPropertiesView = new manageAgentProperties(govukTable, layout)
  val revokeClientPropertyView = new revokeClient(formWithCSRF, govukButton, layout)
  val confirmRevokeClientPropertyView = new confirmRevokeClientProperty(govukPanel, layout)

  lazy val errorView = new error(layout)
  lazy val forbiddenView = new forbidden(layout)
  lazy val technicalDifficultiesView = new technicalDifficulties(layout)
  lazy val notFoundView = new notFound(layout)
  lazy val alreadySubmittedView = new alreadySubmitted(layout)
  lazy val valuationErrorView = new detailedValuationError(govukButton, layout)
}
