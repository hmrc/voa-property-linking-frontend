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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Layouts
import uk.gov.hmrc.govukfrontend.views.html.components._
import uk.gov.hmrc.hmrcfrontend.views.html.components._
import uk.gov.hmrc.hmrcfrontend.views.html.helpers._
import views.html.propertyrepresentation.appoint._
import views.html.{addUserToGG, start}

trait FakeViews extends Layouts {

  // all deprecated classes should be located in this file until DI is introduced

  // all val's referencing mainLayout need to be lazy as some of the deprecated objects
  // are not available until the fake application is running
  // (e.g. HmrcStandardFooter & HmrcTrackingConsentSnippet)
  lazy val mainLayout = new views.html.mainLayout(
    govukTemplate = GovukTemplate,
    govukHeader = GovukHeader,
    govukFooter = GovukFooter,
    govukBackLink = GovukBackLink,
    govukDetails = GovukDetails,
    govukPhaseBanner = GovukPhaseBanner,
    hmrcStandardFooter = {
      val m = mock(classOf[HmrcStandardFooter])
      when(m.apply(any(), any())(any(), any())).thenReturn(Html(""))
      m
    },
    hmrcTrackingConsentSnippet = {
      val m = mock(classOf[HmrcTrackingConsentSnippet])
      when(m.apply(any())(any())).thenReturn(Html(""))
      m
    },
    head = new views.html.head()
  )

  lazy val startView = new start(mainLayout, GovukInsetText, GovukDetails)
  lazy val addUsertoGGView = new addUserToGG(mainLayout)
  lazy val assessmentsView = new views.html.dashboard.assessments(mainLayout, GovukDetails)
  lazy val invalidAccountTypeView = new views.html.errors.invalidAccountType(mainLayout, GovukButton)
  lazy val invalidAccountCreationView = new views.html.errors.invalidAccountCreation(mainLayout)

  lazy val registerIndividualView = new views.html.createAccount.registerIndividual(
    mainLayout,
    GovukInsetText,
    GovukDetails,
    GovukErrorSummary,
    GovukInput,
    GovukDateInput,
    GovukButton,
    FormWithCSRF)

  lazy val registerOrganisationView =
    new views.html.createAccount.registerOrganisation(
      mainLayout,
      GovukButton,
      GovukDateInput,
      GovukInsetText,
      GovukDetails,
      GovukErrorSummary,
      GovukInput,
      GovukRadios,
      FormWithCSRF)

  lazy val registerAssistantAdminView = new views.html.createAccount.registerAssistantAdmin(
    mainLayout,
    GovukInsetText,
    GovukDetails,
    GovukErrorSummary,
    GovukInput,
    GovukButton,
    GovukDateInput,
    FormWithCSRF)

  lazy val registerAssistantView = new views.html.createAccount.registerAssistant(
    mainLayout,
    GovukInsetText,
    GovukDetails,
    GovukErrorSummary,
    GovukInput,
    GovukButton,
    FormWithCSRF)

  lazy val registerConfirmationView =
    new views.html.createAccount.registrationConfirmation(mainLayout, GovukInsetText, GovukDetails, GovukButton)

  lazy val revokeAgentSummaryView =
    new views.html.propertyrepresentation.revokeAgentSummary(mainLayout, GovukButton, GovukPanel)
  lazy val appointAgentSummaryView = new appointAgentSummary(mainLayout, GovukButton, GovukPanel)
  lazy val revokeAgentPropertiesView = new views.html.propertyrepresentation.revokeAgentProperties(
    mainLayout,
    FormWithCSRF,
    GovukErrorSummary,
    GovukInput,
    GovukTable,
    GovukButton)

  lazy val appointAgentPropertiesView = new views.html.propertyrepresentation.appoint.appointAgentProperties(
    mainLayout,
    FormWithCSRF,
    GovukErrorSummary,
    GovukInput,
    GovukTable,
    GovukButton)

  lazy val updateBusinessAddressView =
    new views.html.details.updateBusinessAddress(mainLayout, GovukButton, GovukInput, FormWithCSRF, GovukErrorSummary)
  lazy val updateBusinessNameView =
    new views.html.details.updateBusinessName(mainLayout, GovukButton, GovukInput, FormWithCSRF, GovukErrorSummary)
  lazy val updateBusinessPhoneView =
    new views.html.details.updateBusinessPhone(mainLayout, GovukButton, GovukInput, FormWithCSRF, GovukErrorSummary)
  lazy val updateBusinessEmailView =
    new views.html.details.updateBusinessEmail(mainLayout, GovukButton, GovukInput, FormWithCSRF, GovukErrorSummary)

  lazy val updateAddressView =
    new views.html.details.updateAddress(mainLayout, GovukButton, GovukInput, FormWithCSRF, GovukErrorSummary)
  lazy val updatePhoneView =
    new views.html.details.updatePhone(mainLayout, GovukButton, GovukInput, FormWithCSRF, GovukErrorSummary)
  lazy val updateMobileView =
    new views.html.details.updateMobile(mainLayout, GovukButton, GovukInput, FormWithCSRF, GovukErrorSummary)
  lazy val updateEmailView =
    new views.html.details.updateEmail(mainLayout, GovukButton, GovukInput, FormWithCSRF, GovukErrorSummary)
  lazy val updateNameView =
    new views.html.details.updateName(mainLayout, GovukButton, GovukInput, FormWithCSRF, GovukErrorSummary)
  lazy val managedByAgentsPropertiesView = new views.html.dashboard.managedByAgentsProperties(mainLayout)

}
