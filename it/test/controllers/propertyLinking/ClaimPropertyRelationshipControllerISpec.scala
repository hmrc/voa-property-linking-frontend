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

package controllers.propertyLinking

import base.ISpecBase
import binders.propertylinks.ClaimPropertyReturnToPage
import com.github.tomakehurst.wiremock.client.WireMock._
import models.{ClientDetails, LinkingSession, Occupier, Owner, OwnerOccupier, PropertyRelationship}
import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.PropertyLinkingSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class ClaimPropertyRelationshipControllerISpec extends ISpecBase {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockPropertyLinkingSessionRepository: PropertyLinkingSessionRepository =
    app.injector.instanceOf[PropertyLinkingSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Connection to the property - Valuation Office Agency - GOV.UK"
  val errorTitleText = "Error: Connection to the property - Valuation Office Agency - GOV.UK"
  val headingText = "Connection to the property:"
  val captionText = "Add a property"
  val backLinkText = "Back"
  val errorSummaryTitleText = "There is a problem"
  val errorSummaryMessageText = "What is your connection to the property? - Select an option"
  val errorSummaryMessageTextAgent = "What is your client’s connection to the property? - Select an option"
  val radioErrorMessageText = "Error: Select an option"
  val ownOrOccupySummaryLinkText = "I own or occupy a part of this property"
  val ownOrOccupySummaryLinkTextAgent = "My client owns or occupies part of this property"
  val ownOrOccupySummaryContentText =
    "After the Valuation Office Agency have approved your connection, you may want to ask them to split this property to get separate valuations for each part."
  val ownOrOccupySummaryContentTextAgent =
    "You can still add this property on behalf of your client, as if they owned or occupied the whole property. After we’ve approved your client’s connection to the property, you can ask us to split the property to get separate valuations for each part."
  val dontOwnOrOccupySummaryLinkText = "I do not own the property but I sublet it to someone else"
  val dontOwnOrOccupySummaryLinkTextAgent = "My client does not own the property, but they sublet it to someone else"
  val dontOwnOrOccupySummaryContentP1Text =
    "If you sublet the property to another person or business, select ‘Owner’ and enter the dates of the sublet period."
  val dontOwnOrOccupySummaryContentP1TextAgent =
    "Your client is considered to be the owner if they sublet the property to another person or business. Enter the date the sublet started."
  val dontOwnOrOccupySummaryContentP2Text =
    "If you only sublet part of the property, you may want to ask the Valuation Office Agency to split this property to get separate valuations for each part. You can do this after they have approved your connection to the property."
  val dontOwnOrOccupySummaryContentP2TextAgent =
    "If only part of the property is sublet, you can ask us to split the property to get separate valuations for each part. You can do this after we’ve approved your client’s connection to the property."
  val whatIsYourConnectionText = "What is your connection to the property?"
  val whatIsYourConnectionTextAgent = "What is your client’s connection to the property?"
  val ownerText = "Owner"
  val occupierText = "Occupier"
  val ownerAndOccupierText = "Owner and occupier"
  val continueButtonText = "Continue"

  val titleTextWelsh = "Cysylltiad â’r eiddo - Valuation Office Agency - GOV.UK"
  val errorTitleTextWelsh = "Gwall: Cysylltiad â’r eiddo - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Cysylltiad â’r eiddo:"
  val captionTextWelsh = "Ychwanegu eiddo"
  val backLinkTextWelsh = "Yn ôl"
  val errorSummaryTitleTextWelsh = "Mae yna broblem"
  val errorSummaryMessageTextWelsh = "Beth yw eich cysylltiad chi â’r eiddo? - Dewiswch opsiwn"
  val errorSummaryMessageTextAgentWelsh = "Beth yw cysylltiad eich cleient â’r eiddo? - Dewiswch opsiwn"
  val radioErrorMessageTextWelsh = "Gwall: Dewiswch opsiwn"
  val ownOrOccupySummaryLinkTextWelsh = "Rwy’n berchen neu’n meddiannu rhan o’r eiddo hwn"
  val ownOrOccupySummaryLinkTextAgentWelsh = "Fy nghleient sydd berchen ar neu’n meddiannu rhan o’r eiddo hwn"
  val ownOrOccupySummaryContentTextWelsh =
    "Ar ôl i Asiantaeth y Swyddfa Brisio gymeradwyo eich cysylltiad, efallai byddwch yn dymuno eu holi i rannu’r eiddo hwn er mwyn cael prisiadau ar wahân ar gyfer pob rhan."
  val ownOrOccupySummaryContentTextAgentWelsh =
    "Gallwch barhau i ychwanegu’r eiddo hwn ar ran eich cleient, fel pe baent yn berchen neu’n meddiannu’r eiddo cyfan. Ar ôl i ni gymeradwyo cysylltiad eich cleient â’r eiddo, gallwch ofyn i ni rannu’r eiddo i gael prisiadau ar wahân ar gyfer pob rhan."
  val dontOwnOrOccupySummaryLinkTextWelsh = "Nid fi sy’n berchen ar yr eiddo ond rwy’n ei isosod i rywun arall"
  val dontOwnOrOccupySummaryLinkTextAgentWelsh =
    "Nid fy nghleient sydd berchen yr eiddo, ond maen nhw’n ei isosod i rywun arall"
  val dontOwnOrOccupySummaryContentP1TextWelsh =
    "Os ydych yn isosod yr eiddo i berson neu fusnes arall, dewiswch ’Perchennog’ a nodi’r dyddiadau neu’r cyfnod isosod."
  val dontOwnOrOccupySummaryContentP1TextAgentWelsh =
    "Ystyrir mai eich cleient yw’r perchennog os ydynt yn isosod yr eiddo i berson neu fusnes arall. Nodwch y dyddiad dechreuodd yr isosod."
  val dontOwnOrOccupySummaryContentP2TextWelsh =
    "Os ydych yn isosod rhan o’r eiddo yn unig, efallai dymunwch holi Asiantaeth y Swyddfa Brisio i rannu’r eiddo er mwyn derbyn prisiadau ar wahân ar gyfer pob rhan. Gallwch wneud hyn wedi iddynt gymeradwyo eich cysylltiad â’r eiddo."
  val dontOwnOrOccupySummaryContentP2TextAgentWelsh =
    "Os mai dim ond rhan o’r eiddo sy’n cael ei isosod, gallwch ofyn i ni rannu’r eiddo i gael prisiadau ar wahân ar gyfer pob rhan. Gallwch wneud hyn ar ôl i ni gymeradwyo cysylltiad eich cleient â’r eiddo."
  val whatIsYourConnectionTextWelsh = "Beth yw eich cysylltiad chi â’r eiddo?"
  val whatIsYourConnectionTextAgentWelsh = "Beth yw cysylltiad eich cleient â’r eiddo?"
  val ownerTextWelsh = "Perchennog"
  val occupierTextWelsh = "Meddiannydd"
  val ownerAndOccupierTextWelsh = "Perchennog a meddiannydd"
  val continueButtonTextWelsh = "Parhau"

  val headingLocator = "h1"
  val captionLocator = "#main-content > div > div > span"
  val backLinkLocator = "back-link"
  val errorSummaryTitleLocator = "#main-content > div > div > div.govuk-error-summary > div > h2"
  val errorSummaryMessageLocator = "#main-content > div > div > div.govuk-error-summary > div > div > ul > li > a"
  val radioErrorMessageLocator = "capacity-error"
  val ownOrOccupySummaryLinkLocator = "#own-or-occupy-details > summary > span"
  val ownOrOccupySummaryContentLocator = "#own-or-occupy-details > div"
  val dontOwnOrOccupySummaryLinkLocator = "#do-not-details > summary > span"
  val dontOwnOrOccupySummaryContentP1Locator = "#do-not-details > div > p:nth-child(1)"
  val dontOwnOrOccupySummaryContentP2Locator = "#do-not-details > div > p:nth-child(2)"
  val whatIsYourConnectionLocator = "#main-content > div > div > form > div > fieldset > legend"
  val ownerRadioLocator = "capacity"
  val ownerLabelLocator = "#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label"
  val occupierRadioLocator = "capacity-2"
  val occupierLabelLocator = "#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label"
  val ownerAndOccupierRadioLocator = "capacity-3"
  val ownerAndOccupierLabelLocator =
    "#main-content > div > div > form > div > fieldset > div > div:nth-child(3) > label"
  val continueButtonLocator = "continue"

  val backLinkHref = "/business-rates-property-linking/my-organisation/claim/property-links/claim-property-start"
  val checkYourAnswersHref = "/business-rates-property-linking/my-organisation/claim/property-links/summary"
  val errorHref = "#capacity"
  val propertyOwnershipRedirectUrl = "/business-rates-property-linking/my-organisation/claim/property-links/ownership"

  // showRelationship
  "ClaimPropertyRelationshipController showRelationship displays the correct content in English for an IP" which {
    lazy val document = getPropertyRelationshipPage(language = English, userIsAgent = false)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link with the correct href" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has a $ownOrOccupySummaryLinkText link with correct content" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkText
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentText
    }

    s"has a $dontOwnOrOccupySummaryLinkText link with correct content" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkText
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1Text
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2Text
    }

    s"has a subheading of $whatIsYourConnectionText" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionText
    }

    s"has an $ownerText radio" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerText
    }

    s"has an $occupierText radio" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierText
    }

    s"has an $ownerAndOccupierText radio" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierText
    }

    s"has a $continueButtonText button" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ClaimPropertyRelationshipController showRelationship displays the correct content in English for an Agent" which {
    lazy val document = getPropertyRelationshipPage(language = English, userIsAgent = true)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link with the correct href" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has a $ownOrOccupySummaryLinkTextAgent link with correct content" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextAgent
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextAgent
    }

    s"has a $dontOwnOrOccupySummaryLinkTextAgent link with correct content" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextAgent
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1TextAgent
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2TextAgent
    }

    s"has a subheading of $whatIsYourConnectionTextAgent" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextAgent
    }

    s"has an $ownerText radio" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerText
    }

    s"has an $occupierText radio" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierText
    }

    s"has an $ownerAndOccupierText radio" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierText
    }

    s"has a $continueButtonText button" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ClaimPropertyRelationshipController showRelationship displays the correct content in Welsh for an IP" which {
    lazy val document = getPropertyRelationshipPage(language = Welsh, userIsAgent = false)

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has a $ownOrOccupySummaryLinkText link with correct content in Welsh" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextWelsh
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextWelsh
    }

    s"has a $dontOwnOrOccupySummaryLinkText link with correct content in Welsh" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextWelsh
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1TextWelsh
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2TextWelsh
    }

    s"has a subheading of $whatIsYourConnectionText in Welsh" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextWelsh
    }

    s"has an $ownerText radio in Welsh" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerTextWelsh
    }

    s"has an $occupierText radio in Welsh" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierTextWelsh
    }

    s"has an $ownerAndOccupierText radio in Welsh" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ClaimPropertyRelationshipController showRelationship displays the correct content in Welsh for an Agent" which {
    lazy val document = getPropertyRelationshipPage(language = Welsh, userIsAgent = true)

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has a $ownOrOccupySummaryLinkTextAgent link with correct content in Welsh" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextAgentWelsh
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextAgentWelsh
    }

    s"has a $dontOwnOrOccupySummaryLinkTextAgent link with correct content in Welsh" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextAgentWelsh
      document
        .select(dontOwnOrOccupySummaryContentP1Locator)
        .text shouldBe dontOwnOrOccupySummaryContentP1TextAgentWelsh
      document
        .select(dontOwnOrOccupySummaryContentP2Locator)
        .text shouldBe dontOwnOrOccupySummaryContentP2TextAgentWelsh
    }

    s"has a subheading of $whatIsYourConnectionTextAgent in Welsh" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextAgentWelsh
    }

    s"has an $ownerText radio in Welsh" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerTextWelsh
    }

    s"has an $occupierText radio in Welsh" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierTextWelsh
    }

    s"has an $ownerAndOccupierText radio in Welsh" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  // submitRelationship - Error scenarios
  "ClaimPropertyRelationshipController submitRelationship displays an error in English when the IP doesn't select a radio" which {
    lazy val res =
      postPropertyRelationshipPage(language = English, userIsAgent = false, selectedRadio = None, fromCya = false)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorTitleText" in {
      document.title() shouldBe errorTitleText
    }

    s"has an error summary with correct error messages" in {
      document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
      document.select(errorSummaryMessageLocator).text shouldBe errorSummaryMessageText
      document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
      document.getElementById(radioErrorMessageLocator).text shouldBe radioErrorMessageText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link with the correct href" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has a $ownOrOccupySummaryLinkText link with correct content" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkText
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentText
    }

    s"has a $dontOwnOrOccupySummaryLinkText link with correct content" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkText
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1Text
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2Text
    }

    s"has a subheading of $whatIsYourConnectionText" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionText
    }

    s"has an $ownerText radio" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerText
    }

    s"has an $occupierText radio" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierText
    }

    s"has an $ownerAndOccupierText radio" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierText
    }

    s"has a $continueButtonText button" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ClaimPropertyRelationshipController submitRelationship displays an error in English when the Agent doesn't select a radio" which {
    lazy val res =
      postPropertyRelationshipPage(language = English, userIsAgent = true, selectedRadio = None, fromCya = false)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorTitleText" in {
      document.title() shouldBe errorTitleText
    }

    s"has an error summary with correct error messages" in {
      document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleText
      document.select(errorSummaryMessageLocator).text shouldBe errorSummaryMessageTextAgent
      document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
      document.getElementById(radioErrorMessageLocator).text shouldBe radioErrorMessageText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link with the correct href" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has a $ownOrOccupySummaryLinkTextAgent link with correct content" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextAgent
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextAgent
    }

    s"has a $dontOwnOrOccupySummaryLinkTextAgent link with correct content" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextAgent
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1TextAgent
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2TextAgent
    }

    s"has a subheading of $whatIsYourConnectionTextAgent" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextAgent
    }

    s"has an $ownerText radio" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerText
    }

    s"has an $occupierText radio" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierText
    }

    s"has an $ownerAndOccupierText radio" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierText
    }

    s"has a $continueButtonText button" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ClaimPropertyRelationshipController submitRelationship displays an error in Welsh when the IP doesn't select a radio" which {
    lazy val res =
      postPropertyRelationshipPage(language = Welsh, userIsAgent = false, selectedRadio = None, fromCya = false)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorTitleText in Welsh" in {
      document.title() shouldBe errorTitleTextWelsh
    }

    s"has an error summary with correct error messages in Welsh" in {
      document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
      document.select(errorSummaryMessageLocator).text shouldBe errorSummaryMessageTextWelsh
      document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
      document.getElementById(radioErrorMessageLocator).text shouldBe radioErrorMessageTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has a $ownOrOccupySummaryLinkText link with correct content in Welsh" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextWelsh
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextWelsh
    }

    s"has a $dontOwnOrOccupySummaryLinkText link with correct content in Welsh" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextWelsh
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1TextWelsh
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2TextWelsh
    }

    s"has a subheading of $whatIsYourConnectionText in Welsh" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextWelsh
    }

    s"has an $ownerText radio in Welsh" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerTextWelsh
    }

    s"has an $occupierText radio in Welsh" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierTextWelsh
    }

    s"has an $ownerAndOccupierText radio in Welsh" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ClaimPropertyRelationshipController submitRelationship displays an error in Welsh when the Agent doesn't select a radio" which {
    lazy val res =
      postPropertyRelationshipPage(language = Welsh, userIsAgent = true, selectedRadio = None, fromCya = false)
    lazy val document = Jsoup.parse(res.body)

    "has a status of 400" in {
      res.status shouldBe BAD_REQUEST
    }

    s"has a title of $errorTitleText in Welsh" in {
      document.title() shouldBe errorTitleTextWelsh
    }

    s"has an error summary with correct error messages in Welsh" in {
      document.select(errorSummaryTitleLocator).text shouldBe errorSummaryTitleTextWelsh
      document.select(errorSummaryMessageLocator).text shouldBe errorSummaryMessageTextAgentWelsh
      document.select(errorSummaryMessageLocator).attr("href") shouldBe errorHref
      document.getElementById(radioErrorMessageLocator).text shouldBe radioErrorMessageTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe backLinkHref
    }

    s"has a $ownOrOccupySummaryLinkTextAgent link with correct content in Welsh" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextAgentWelsh
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextAgentWelsh
    }

    s"has a $dontOwnOrOccupySummaryLinkTextAgent link with correct content in Welsh" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextAgentWelsh
      document
        .select(dontOwnOrOccupySummaryContentP1Locator)
        .text shouldBe dontOwnOrOccupySummaryContentP1TextAgentWelsh
      document
        .select(dontOwnOrOccupySummaryContentP2Locator)
        .text shouldBe dontOwnOrOccupySummaryContentP2TextAgentWelsh
    }

    s"has a subheading of $whatIsYourConnectionTextAgent in Welsh" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextAgentWelsh
    }

    s"has an $ownerText radio in Welsh" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerTextWelsh
    }

    s"has an $occupierText radio in Welsh" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierTextWelsh
    }

    s"has an $ownerAndOccupierText radio in Welsh" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  // submitRelationship - Redirect location
  "ClaimPropertyRelationshipController submitRelationship redirects to the property ownership page when an IP selects the 'Owner and occupier' radio" which {
    lazy val res = postPropertyRelationshipPage(
      language = Welsh,
      userIsAgent = false,
      selectedRadio = Some("OWNER_OCCUPIER"),
      fromCya = false)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(propertyOwnershipRedirectUrl)
    }
  }

  "ClaimPropertyRelationshipController submitRelationship redirects to the property ownership page when an IP selects the 'Owner' radio" which {
    lazy val res = postPropertyRelationshipPage(
      language = Welsh,
      userIsAgent = false,
      selectedRadio = Some("OWNER"),
      fromCya = false)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(propertyOwnershipRedirectUrl)
    }
  }

  "ClaimPropertyRelationshipController submitRelationship redirects to the property ownership page when an IP selects the 'Occupier' radio" which {
    lazy val res = postPropertyRelationshipPage(
      language = Welsh,
      userIsAgent = false,
      selectedRadio = Some("OCCUPIER"),
      fromCya = false)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(propertyOwnershipRedirectUrl)
    }
  }

  "ClaimPropertyRelationshipController submitRelationship redirects to the property ownership page when an Agent selects the 'Owner and occupier' radio" which {
    lazy val res = postPropertyRelationshipPage(
      language = Welsh,
      userIsAgent = true,
      selectedRadio = Some("OWNER_OCCUPIER"),
      fromCya = false)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(propertyOwnershipRedirectUrl)
    }
  }

  "ClaimPropertyRelationshipController submitRelationship redirects to the property ownership page when an Agent selects the 'Owner' radio" which {
    lazy val res =
      postPropertyRelationshipPage(language = Welsh, userIsAgent = true, selectedRadio = Some("OWNER"), fromCya = false)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(propertyOwnershipRedirectUrl)
    }
  }

  "ClaimPropertyRelationshipController submitRelationship redirects to the property ownership page when an Agent selects the 'Occupier' radio" which {
    lazy val res = postPropertyRelationshipPage(
      language = Welsh,
      userIsAgent = true,
      selectedRadio = Some("OCCUPIER"),
      fromCya = false)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(propertyOwnershipRedirectUrl)
    }
  }

  // submitRelationship - Redirect location from cya
  "ClaimPropertyRelationshipController submitRelationship redirects to the check your answers page when an IP selects the 'Owner and occupier' radio" which {
    lazy val res = postPropertyRelationshipPage(
      language = Welsh,
      userIsAgent = false,
      selectedRadio = Some("OWNER_OCCUPIER"),
      fromCya = true)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(checkYourAnswersHref)
    }
  }

  "ClaimPropertyRelationshipController submitRelationship redirects to the check your answers page when an IP selects the 'Owner' radio" which {
    lazy val res =
      postPropertyRelationshipPage(language = Welsh, userIsAgent = false, selectedRadio = Some("OWNER"), fromCya = true)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(checkYourAnswersHref)
    }
  }

  "ClaimPropertyRelationshipController submitRelationship redirects to the check your answers page when an IP selects the 'Occupier' radio" which {
    lazy val res = postPropertyRelationshipPage(
      language = Welsh,
      userIsAgent = false,
      selectedRadio = Some("OCCUPIER"),
      fromCya = true)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(checkYourAnswersHref)
    }
  }

  "ClaimPropertyRelationshipController submitRelationship redirects to the check your answers page when an Agent selects the 'Owner and occupier' radio" which {
    lazy val res = postPropertyRelationshipPage(
      language = Welsh,
      userIsAgent = true,
      selectedRadio = Some("OWNER_OCCUPIER"),
      fromCya = true)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(checkYourAnswersHref)
    }
  }

  "ClaimPropertyRelationshipController submitRelationship redirects to the check your answers page when an Agent selects the 'Owner' radio" which {
    lazy val res =
      postPropertyRelationshipPage(language = Welsh, userIsAgent = true, selectedRadio = Some("OWNER"), fromCya = true)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(checkYourAnswersHref)
    }
  }

  "ClaimPropertyRelationshipController submitRelationship redirects to the check your answers page when an Agent selects the 'Occupier' radio" which {
    lazy val res = postPropertyRelationshipPage(
      language = Welsh,
      userIsAgent = true,
      selectedRadio = Some("OCCUPIER"),
      fromCya = true)

    "has the correct status and redirect location" in {
      res.status shouldBe SEE_OTHER
      res.header("location") shouldBe Some(checkYourAnswersHref)
    }
  }

  // back from cya
  "ClaimPropertyRelationshipController back takes the IP back to the relationship page with a preselected 'Owner and occupier' radio in English" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = English, userIsAgent = false, selectedRadio = Some("OWNER_OCCUPIER"))

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link with the correct href" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkText link with correct content" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkText
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentText
    }

    s"has a $dontOwnOrOccupySummaryLinkText link with correct content" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkText
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1Text
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2Text
    }

    s"has a subheading of $whatIsYourConnectionText" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionText
    }

    s"has an $ownerText radio" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerText
    }

    s"has an $occupierText radio" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierText
    }

    s"has an $ownerAndOccupierText radio" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe true
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierText
    }

    s"has a $continueButtonText button" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ClaimPropertyRelationshipController back takes the IP back to the relationship page with a preselected 'Occupier' radio in English" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = English, userIsAgent = false, selectedRadio = Some("OCCUPIER"))

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link with the correct href" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkText link with correct content" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkText
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentText
    }

    s"has a $dontOwnOrOccupySummaryLinkText link with correct content" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkText
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1Text
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2Text
    }

    s"has a subheading of $whatIsYourConnectionText" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionText
    }

    s"has an $ownerText radio" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerText
    }

    s"has an $occupierText radio" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe true
      document.select(occupierLabelLocator).text shouldBe occupierText
    }

    s"has an $ownerAndOccupierText radio" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierText
    }

    s"has a $continueButtonText button" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ClaimPropertyRelationshipController back takes the IP back to the relationship page with a preselected 'Owner' radio in English" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = English, userIsAgent = false, selectedRadio = Some("OWNER"))

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link with the correct href" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkText link with correct content" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkText
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentText
    }

    s"has a $dontOwnOrOccupySummaryLinkText link with correct content" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkText
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1Text
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2Text
    }

    s"has a subheading of $whatIsYourConnectionText" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionText
    }

    s"has an $ownerText radio" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe true
      document.select(ownerLabelLocator).text shouldBe ownerText
    }

    s"has an $occupierText radio" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierText
    }

    s"has an $ownerAndOccupierText radio" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierText
    }

    s"has a $continueButtonText button" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ClaimPropertyRelationshipController back takes the Agent back to the relationship page with a preselected 'Owner and occupier' radio in English" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = English, userIsAgent = true, selectedRadio = Some("OWNER_OCCUPIER"))

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link with the correct href" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkTextAgent link with correct content" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextAgent
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextAgent
    }

    s"has a $dontOwnOrOccupySummaryLinkTextAgent link with correct content" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextAgent
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1TextAgent
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2TextAgent
    }

    s"has a subheading of $whatIsYourConnectionTextAgent" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextAgent
    }

    s"has an $ownerText radio" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerText
    }

    s"has an $occupierText radio" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierText
    }

    s"has an $ownerAndOccupierText radio" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe true
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierText
    }

    s"has a $continueButtonText button" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ClaimPropertyRelationshipController back takes the Agent back to the relationship page with a preselected 'Occupier' radio in English" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = English, userIsAgent = true, selectedRadio = Some("OCCUPIER"))

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link with the correct href" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkTextAgent link with correct content" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextAgent
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextAgent
    }

    s"has a $dontOwnOrOccupySummaryLinkTextAgent link with correct content" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextAgent
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1TextAgent
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2TextAgent
    }

    s"has a subheading of $whatIsYourConnectionTextAgent" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextAgent
    }

    s"has an $ownerText radio" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerText
    }

    s"has an $occupierText radio" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe true
      document.select(occupierLabelLocator).text shouldBe occupierText
    }

    s"has an $ownerAndOccupierText radio" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierText
    }

    s"has a $continueButtonText button" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ClaimPropertyRelationshipController back takes the Agent back to the relationship page with a preselected 'Owner' radio in English" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = English, userIsAgent = true, selectedRadio = Some("OWNER"))

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingLocator).text shouldBe headingText
    }

    s"has a caption of $captionText" in {
      document.select(captionLocator).text shouldBe captionText
    }

    "has a back link with the correct href" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkText
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkTextAgent link with correct content" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextAgent
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextAgent
    }

    s"has a $dontOwnOrOccupySummaryLinkTextAgent link with correct content" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextAgent
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1TextAgent
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2TextAgent
    }

    s"has a subheading of $whatIsYourConnectionTextAgent" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextAgent
    }

    s"has an $ownerText radio" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe true
      document.select(ownerLabelLocator).text shouldBe ownerText
    }

    s"has an $occupierText radio" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierText
    }

    s"has an $ownerAndOccupierText radio" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierText
    }

    s"has a $continueButtonText button" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonText
    }
  }

  "ClaimPropertyRelationshipController back takes the IP back to the relationship page with a preselected 'Owner and occupier' radio in Welsh" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = Welsh, userIsAgent = false, selectedRadio = Some("OWNER_OCCUPIER"))

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkText link with correct content in Welsh" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextWelsh
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextWelsh
    }

    s"has a $dontOwnOrOccupySummaryLinkText link with correct content in Welsh" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextWelsh
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1TextWelsh
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2TextWelsh
    }

    s"has a subheading of $whatIsYourConnectionText in Welsh" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextWelsh
    }

    s"has an $ownerText radio in Welsh" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerTextWelsh
    }

    s"has an $occupierText radio in Welsh" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierTextWelsh
    }

    s"has an $ownerAndOccupierText radio in Welsh" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe true
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ClaimPropertyRelationshipController back takes the IP back to the relationship page with a preselected 'Occupier' radio in Welsh" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = Welsh, userIsAgent = false, selectedRadio = Some("OCCUPIER"))

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkText link with correct content in Welsh" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextWelsh
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextWelsh
    }

    s"has a $dontOwnOrOccupySummaryLinkText link with correct content in Welsh" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextWelsh
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1TextWelsh
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2TextWelsh
    }

    s"has a subheading of $whatIsYourConnectionText in Welsh" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextWelsh
    }

    s"has an $ownerText radio in Welsh" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerTextWelsh
    }

    s"has an $occupierText radio in Welsh" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe true
      document.select(occupierLabelLocator).text shouldBe occupierTextWelsh
    }

    s"has an $ownerAndOccupierText radio in Welsh" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ClaimPropertyRelationshipController back takes the IP back to the relationship page with a preselected 'Owner' radio in Welsh" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = Welsh, userIsAgent = false, selectedRadio = Some("OWNER"))

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkText link with correct content in Welsh" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextWelsh
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextWelsh
    }

    s"has a $dontOwnOrOccupySummaryLinkText link with correct content in Welsh" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextWelsh
      document.select(dontOwnOrOccupySummaryContentP1Locator).text shouldBe dontOwnOrOccupySummaryContentP1TextWelsh
      document.select(dontOwnOrOccupySummaryContentP2Locator).text shouldBe dontOwnOrOccupySummaryContentP2TextWelsh
    }

    s"has a subheading of $whatIsYourConnectionText in Welsh" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextWelsh
    }

    s"has an $ownerText radio in Welsh" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe true
      document.select(ownerLabelLocator).text shouldBe ownerTextWelsh
    }

    s"has an $occupierText radio in Welsh" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierTextWelsh
    }

    s"has an $ownerAndOccupierText radio in Welsh" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ClaimPropertyRelationshipController back takes the Agent back to the relationship page with a preselected 'Owner and occupier' radio in Welsh" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = Welsh, userIsAgent = true, selectedRadio = Some("OWNER_OCCUPIER"))

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkTextAgent link with correct content in Welsh" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextAgentWelsh
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextAgentWelsh
    }

    s"has a $dontOwnOrOccupySummaryLinkTextAgent link with correct content in Welsh" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextAgentWelsh
      document
        .select(dontOwnOrOccupySummaryContentP1Locator)
        .text shouldBe dontOwnOrOccupySummaryContentP1TextAgentWelsh
      document
        .select(dontOwnOrOccupySummaryContentP2Locator)
        .text shouldBe dontOwnOrOccupySummaryContentP2TextAgentWelsh
    }

    s"has a subheading of $whatIsYourConnectionTextAgent in Welsh" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextAgentWelsh
    }

    s"has an $ownerText radio in Welsh" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerTextWelsh
    }

    s"has an $occupierText radio in Welsh" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierTextWelsh
    }

    s"has an $ownerAndOccupierText radio in Welsh" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe true
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ClaimPropertyRelationshipController back takes the Agent back to the relationship page with a preselected 'Occupier' radio in Welsh" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = Welsh, userIsAgent = true, selectedRadio = Some("OCCUPIER"))

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkTextAgent link with correct content in Welsh" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextAgentWelsh
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextAgentWelsh
    }

    s"has a $dontOwnOrOccupySummaryLinkTextAgent link with correct content in Welsh" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextAgentWelsh
      document
        .select(dontOwnOrOccupySummaryContentP1Locator)
        .text shouldBe dontOwnOrOccupySummaryContentP1TextAgentWelsh
      document
        .select(dontOwnOrOccupySummaryContentP2Locator)
        .text shouldBe dontOwnOrOccupySummaryContentP2TextAgentWelsh
    }

    s"has a subheading of $whatIsYourConnectionTextAgent in Welsh" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextAgentWelsh
    }

    s"has an $ownerText radio in Welsh" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerLabelLocator).text shouldBe ownerTextWelsh
    }

    s"has an $occupierText radio in Welsh" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe true
      document.select(occupierLabelLocator).text shouldBe occupierTextWelsh
    }

    s"has an $ownerAndOccupierText radio in Welsh" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  "ClaimPropertyRelationshipController back takes the Agent back to the relationship page with a preselected 'Owner' radio in Welsh" which {
    lazy val document =
      getBackToPropertyRelationshipPage(language = Welsh, userIsAgent = true, selectedRadio = Some("OWNER"))

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingLocator).text shouldBe headingTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionLocator).text shouldBe captionTextWelsh
    }

    "has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkLocator).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkLocator).attr("href") shouldBe checkYourAnswersHref
    }

    s"has a $ownOrOccupySummaryLinkTextAgent link with correct content in Welsh" in {
      document.select(ownOrOccupySummaryLinkLocator).text shouldBe ownOrOccupySummaryLinkTextAgentWelsh
      document.select(ownOrOccupySummaryContentLocator).text shouldBe ownOrOccupySummaryContentTextAgentWelsh
    }

    s"has a $dontOwnOrOccupySummaryLinkTextAgent link with correct content in Welsh" in {
      document.select(dontOwnOrOccupySummaryLinkLocator).text shouldBe dontOwnOrOccupySummaryLinkTextAgentWelsh
      document
        .select(dontOwnOrOccupySummaryContentP1Locator)
        .text shouldBe dontOwnOrOccupySummaryContentP1TextAgentWelsh
      document
        .select(dontOwnOrOccupySummaryContentP2Locator)
        .text shouldBe dontOwnOrOccupySummaryContentP2TextAgentWelsh
    }

    s"has a subheading of $whatIsYourConnectionTextAgent in Welsh" in {
      document.select(whatIsYourConnectionLocator).text shouldBe whatIsYourConnectionTextAgentWelsh
    }

    s"has an $ownerText radio in Welsh" in {
      document.getElementById(ownerRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerRadioLocator).hasAttr("checked") shouldBe true
      document.select(ownerLabelLocator).text shouldBe ownerTextWelsh
    }

    s"has an $occupierText radio in Welsh" in {
      document.getElementById(occupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(occupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(occupierLabelLocator).text shouldBe occupierTextWelsh
    }

    s"has an $ownerAndOccupierText radio in Welsh" in {
      document.getElementById(ownerAndOccupierRadioLocator).attr("type") shouldBe "radio"
      document.getElementById(ownerAndOccupierRadioLocator).hasAttr("checked") shouldBe false
      document.select(ownerAndOccupierLabelLocator).text shouldBe ownerAndOccupierTextWelsh
    }

    s"has a $continueButtonText button in Welsh" in {
      document.getElementById(continueButtonLocator).text shouldBe continueButtonTextWelsh
    }
  }

  private def commonSetup(userIsAgent: Boolean, selectedOptionFromCya: Option[String] = None, fromCya: Boolean) = {
    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

    val relationship: Option[PropertyRelationship] = selectedOptionFromCya.map {
      case "OWNER"          => PropertyRelationship(capacity = Owner, uarn = 1)
      case "OCCUPIER"       => PropertyRelationship(capacity = Occupier, uarn = 1)
      case "OWNER_OCCUPIER" => PropertyRelationship(capacity = OwnerOccupier, uarn = 1)
    }

    await(
      mockPropertyLinkingSessionRepository.saveOrUpdate(LinkingSession(
        address = "Test Address, Test Lane, T35 T3R",
        uarn = 1L,
        submissionId = "PL-123456",
        personId = 1L,
        earliestStartDate = LocalDate.of(2017, 4, 1),
        propertyRelationship = relationship,
        propertyOwnership = None,
        propertyOccupancy = None,
        hasRatesBill = None,
        clientDetails = if (userIsAgent) Some(ClientDetails(123, "Client Name")) else None,
        localAuthorityReference = "2050466366770",
        rtp = ClaimPropertyReturnToPage.FMBR,
        fromCya = Some(fromCya),
        isSubmitted = None
      )))
  }

  private def getPropertyRelationshipPage(language: Language, userIsAgent: Boolean) = {
    commonSetup(userIsAgent = userIsAgent, fromCya = false)

    val res = await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/capacity/relationship")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def postPropertyRelationshipPage(
        language: Language,
        userIsAgent: Boolean,
        selectedRadio: Option[String],
        fromCya: Boolean) = {
    commonSetup(userIsAgent = userIsAgent, fromCya = fromCya)

    stubFor {
      get("/vmv/rating-listing/api/properties/1")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testPropertyHistory).toString())
        }
    }

    val body = Map(
      "csrfToken" -> Seq("nocheck"),
      "capacity"  -> Seq(selectedRadio.getOrElse("")),
      "uarn"      -> Seq("1")
    )

    await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/capacity/relationship")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .withFollowRedirects(follow = false)
        .post(body)
    )

  }

  private def getBackToPropertyRelationshipPage(
        language: Language,
        userIsAgent: Boolean,
        selectedRadio: Option[String]) = {
    commonSetup(userIsAgent = userIsAgent, selectedOptionFromCya = selectedRadio, fromCya = true)

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
