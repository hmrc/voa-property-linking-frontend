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

import com.typesafe.config.{Config, ConfigFactory}
import play.api.Configuration
import uk.gov.hmrc.govukfrontend.views.html.components._
import uk.gov.hmrc.hmrcfrontend.config.{AccessibilityStatementConfig, TrackingConsentConfig}
import uk.gov.hmrc.hmrcfrontend.views.config.HmrcFooterItems
import uk.gov.hmrc.hmrcfrontend.views.html.components.HmrcFooter
import uk.gov.hmrc.hmrcfrontend.views.html.helpers.{HmrcStandardFooter, HmrcTrackingConsentSnippet}

trait GdsComponents {

  private val minimalConfig: Config =
    ConfigFactory.parseString("")

  lazy val minimalConfiguration = Configuration(minimalConfig)

  lazy val formWithCSRF = new FormWithCSRF
  lazy val govukAccordion = new GovukAccordion
  lazy val govukBackLink = new GovukBackLink
  lazy val govukBreadcrumbs = new GovukBreadcrumbs
  lazy val govukButton = new GovukButton
  lazy val govukCharacterCount = new GovukCharacterCount(govukTextarea, govukHint)
  lazy val govukCheckboxes = new GovukCheckboxes(govukErrorMessage, govukFieldset, govukHint, govukLabel)
  lazy val govukDateInput = new GovukDateInput(govukErrorMessage, govukHint, govukFieldset, govukInput)
  lazy val govukDetails = new GovukDetails
  lazy val govukErrorMessage = new GovukErrorMessage
  lazy val govukErrorSummary = new GovukErrorSummary
  lazy val govukFieldset = new GovukFieldset
  lazy val govukFooter = new GovukFooter
  lazy val govukHeader = new GovukHeader
  lazy val govukHint = new GovukHint
  lazy val govukInput = new GovukInput(govukErrorMessage, govukHint, govukLabel)
  lazy val govukInsetText = new GovukInsetText
  lazy val govukLabel = new GovukLabel
  lazy val govukPanel = new GovukPanel
  lazy val govukPhaseBanner = new GovukPhaseBanner(govukTag)
  lazy val govukRadios = new GovukRadios(govukErrorMessage, govukFieldset, govukHint, govukLabel)
  lazy val govukSelect = new GovukSelect(govukErrorMessage, govukHint, govukLabel)
  lazy val govukSkipLink = new GovukSkipLink
  lazy val govukSummaryList = new GovukSummaryList
  lazy val govukTabs = new GovukTabs
  lazy val govukTable = new GovukTable
  lazy val govukTag = new GovukTag
  lazy val govukTemplate = new GovukTemplate(govukHeader, govukFooter, govukSkipLink, new FixedWidthPageLayout())
  lazy val govukTextarea = new GovukTextarea(govukErrorMessage, govukHint, govukLabel)
  lazy val govukWarningText = new GovukWarningText
  lazy val hmrcFooter = new HmrcFooter()
  lazy val hmrcFooterItems = new HmrcFooterItems(new AccessibilityStatementConfig(minimalConfiguration))
  lazy val hmrcStandardFooter = new HmrcStandardFooter(hmrcFooter, hmrcFooterItems)
  lazy val hmrcTrackingConsentSnippet = new HmrcTrackingConsentSnippet(new TrackingConsentConfig(minimalConfiguration))

}
