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

import com.typesafe.config.{Config, ConfigFactory}
import play.api.Configuration
import uk.gov.hmrc.govukfrontend.views.html.components._
import uk.gov.hmrc.govukfrontend.views.html.helpers.{GovukFormGroup, GovukHintAndErrorMessage, GovukLogo}
import uk.gov.hmrc.hmrcfrontend.config.{AccessibilityStatementConfig, AssetsConfig, RebrandConfig, TimeoutDialogConfig, TrackingConsentConfig, TudorCrownConfig}
import uk.gov.hmrc.hmrcfrontend.views.config.{HmrcFooterItems, StandardBetaBanner}
import uk.gov.hmrc.hmrcfrontend.views.html.components.{HmrcFooter, HmrcTimeoutDialog}
import uk.gov.hmrc.hmrcfrontend.views.html.helpers.{HmrcScripts, HmrcStandardFooter, HmrcTimeoutDialogHelper, HmrcTrackingConsentSnippet}

trait GdsComponents {

  private val minimalConfig: Config =
    ConfigFactory.parseString("""
                                |timeoutDialog.timeout=13min
                                |timeoutDialog.timeout=13min
                                |timeoutDialog.countdown=3min
                                |hmrc-timeout-dialog.defaultTimeoutInSeconds=15
                                |hmrc-timeout-dialog.defaultCountdownInSeconds=15
                                |hmrc-timeout-dialog.enableSynchroniseTabs=true
                                |session.timeoutSeconds=10
                                |session.timeout=10
                            """.stripMargin)

  lazy val minimalConfiguration = Configuration(minimalConfig)

  lazy val formWithCSRF = new FormWithCSRF
  lazy val govukAccordion = new GovukAccordion
  lazy val govukBackLink = new GovukBackLink
  lazy val govukBreadcrumbs = new GovukBreadcrumbs
  lazy val govukButton = new GovukButton
  lazy val govukCharacterCount = new GovukCharacterCount(govukTextarea, govukHint)
  lazy val govukCheckboxes =
    new GovukCheckboxes(govukFieldset, govukHint, govukLabel, govukFormGroup, govukHintAndErrorMessage)
  lazy val govukDateInput = new GovukDateInput(govukFieldset, govukInput, govukFormGroup, govukHintAndErrorMessage)
  lazy val govukDetails = new GovukDetails
  lazy val govukErrorMessage = new GovukErrorMessage
  lazy val govukErrorSummary = new GovukErrorSummary
  lazy val govukFieldset = new GovukFieldset
  lazy val govukFooter = new GovukFooter(RebrandConfig(minimalConfiguration), govukLogo)
  lazy val govukFormGroup = new GovukFormGroup
  lazy val govukHeader =
    new GovukHeader(TudorCrownConfig(minimalConfiguration), RebrandConfig(minimalConfiguration), govukLogo)
  lazy val govukHint = new GovukHint
  lazy val govukHintAndErrorMessage = new GovukHintAndErrorMessage(govukHint, govukErrorMessage)
  lazy val govukInput = new GovukInput(govukLabel, govukFormGroup, govukHintAndErrorMessage)
  lazy val govukInsetText = new GovukInsetText
  lazy val govukLabel = new GovukLabel
  lazy val govukLogo = new GovukLogo
  lazy val govukPanel = new GovukPanel
  lazy val govukPhaseBanner = new GovukPhaseBanner(govukTag)
  lazy val govukRadios = new GovukRadios(govukFieldset, govukHint, govukLabel, govukFormGroup, govukHintAndErrorMessage)
  lazy val govukSelect = new GovukSelect(govukLabel, govukFormGroup, govukHintAndErrorMessage)
  lazy val govukSkipLink = new GovukSkipLink
  lazy val govukSummaryList = new GovukSummaryList
  lazy val govukTabs = new GovukTabs
  lazy val govukTable = new GovukTable
  lazy val govukTag = new GovukTag
  lazy val govukTemplate = new GovukTemplate(
    govukHeader,
    govukFooter,
    govukSkipLink,
    new FixedWidthPageLayout(),
    RebrandConfig(minimalConfiguration)
  )
  lazy val govukTextarea = new GovukTextarea(govukLabel, govukFormGroup, govukHintAndErrorMessage)
  lazy val govukWarningText = new GovukWarningText
  lazy val govukFileUpload = new GovukFileUpload(govukLabel, govukFormGroup, govukHintAndErrorMessage)
  lazy val hmrcFooter = new HmrcFooter(govukFooter)
  lazy val hmrcFooterItems = new HmrcFooterItems(new AccessibilityStatementConfig(minimalConfiguration))
  lazy val hmrcStandardFooter = new HmrcStandardFooter(hmrcFooter, hmrcFooterItems)
  lazy val hmrcTrackingConsentSnippet = new HmrcTrackingConsentSnippet(new TrackingConsentConfig(minimalConfiguration))
  lazy val hmrcTimeoutDialogHelper =
    new HmrcTimeoutDialogHelper(new HmrcTimeoutDialog, new TimeoutDialogConfig(minimalConfiguration))
  lazy val standardBetaBanner = new StandardBetaBanner
  lazy val hmrcScripts = new HmrcScripts(new AssetsConfig)
}
