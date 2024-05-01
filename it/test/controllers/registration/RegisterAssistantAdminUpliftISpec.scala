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

package controllers.registration

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.{Address, GroupAccount}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.ListYearsHelpers

class RegisterAssistantAdminUpliftISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  val titleText = "Confirm your organisation details - Valuation Office Agency - GOV.UK"
  val headingText = "Confirm your organisation details"
  val weUseYourText =
    "We use your organisation details to send you correspondence related to the service and your account."
  val yourOrgText = "Your organisation details"
  val orgNameText = "Organisation name"
  val postalAddressText = "Postal address"
  val phoneNumText = "Telephone number"
  val emailText = "Correspondence email address"
  val confirmText = "Confirm"

  val titleTextWelsh = "Cadarnhau manylion eich sefydliad - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Cadarnhau manylion eich sefydliad"
  val weUseYourTextWelsh =
    "Rydym yn defnyddio manylion eich sefydliad i anfon gohebiaeth atoch yn ymwneud â’r gwasanaeth a’ch cyfrif."
  val yourOrgTextWelsh = "Manylion eich sefydliad"
  val orgNameTextWelsh = "Enw sefydliad"
  val postalAddressTextWelsh = "Cyfeiriad post"
  val phoneNumTextWelsh = "Rhif ffôn"
  val emailTextWelsh = "Cyfeiriad gohebiaeth e-bost"
  val confirmTextWelsh = "Cadarnhau"

  val headingSelector = "#main-content > div > div > h1"
  val weUseYourSelector = "#main-content > div > div > p.govuk-body"
  val yourOrgSelector = "#main-content > div > div > h2"
  val orgNameSelector = "#main-content > div > div > table > tbody > tr:nth-child(1) > th"
  val orgNameValueSelector = "#main-content > div > div > table > tbody > tr:nth-child(1) > td"
  val postalAddressSelector = "#main-content > div > div > table > tbody > tr:nth-child(2) > th"
  val postalAddressValueSelector = "#main-content > div > div > table > tbody > tr:nth-child(2) > td"
  val phoneNumSelector = "#main-content > div > div > table > tbody > tr:nth-child(3) > th"
  val phoneNumValueSelector = "#main-content > div > div > table > tbody > tr:nth-child(3) > td"
  val emailSelector = "#main-content > div > div > table > tbody > tr:nth-child(4) > th"
  val emailValueSelector = "#main-content > div > div > table > tbody > tr:nth-child(4) > td"
  val confirmSelector = "button.govuk-button"

  "RegistrationController show method for a new assistant to the organisation" should {
    "Show an English registration contact details screen with the correct text" which {

      lazy val document: Document = getSuccessPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text() shouldBe headingText
      }

      s"has text on the screen of $weUseYourText" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourText
      }

      s"has a small header on the screen of $yourOrgText" in {
        document.select(yourOrgSelector).text() shouldBe yourOrgText
      }

      s"has a table row for the $orgNameText" in {
        document.select(orgNameSelector).text() shouldBe orgNameText
        document.select(orgNameValueSelector).text() shouldBe "Test name"
      }

      s"has a table row for the $postalAddressText" in {
        document.select(postalAddressSelector).text() shouldBe postalAddressText
        document.select(postalAddressValueSelector).text() shouldBe "Test 1 Test 2 Test 3 Test 4 TF4 9ER"
      }

      s"has a table row for the $phoneNumText" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumText
        document.select(phoneNumValueSelector).text() shouldBe "1234567890"
      }

      s"has a table row for the $emailText" in {
        document.select(emailSelector).text() shouldBe emailText
        document.select(emailValueSelector).text() shouldBe "test@email.com"
      }

      s"has a $confirmText button" in {
        document.select(confirmSelector).text() shouldBe confirmText
      }
    }

    "Show a Welsh registration contact details screen with the correct text" which {

      lazy val document: Document = getSuccessPage(Welsh)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of $headingText in welsh" in {
        document.select(headingSelector).text() shouldBe headingTextWelsh
      }

      s"has text on the screen of $weUseYourText in welsh" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourTextWelsh
      }

      s"has a small header on the screen of $yourOrgText in welsh" in {
        document.select(yourOrgSelector).text() shouldBe yourOrgTextWelsh
      }

      s"has a table row for the $orgNameText in welsh" in {
        document.select(orgNameSelector).text() shouldBe orgNameTextWelsh
        document.select(orgNameValueSelector).text() shouldBe "Test name"
      }

      s"has a table row for the $postalAddressText in welsh" in {
        document.select(postalAddressSelector).text() shouldBe postalAddressTextWelsh
        document.select(postalAddressValueSelector).text() shouldBe "Test 1 Test 2 Test 3 Test 4 TF4 9ER"
      }

      s"has a table row for the $phoneNumText in welsh" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumTextWelsh
        document.select(phoneNumValueSelector).text() shouldBe "1234567890"
      }

      s"has a table row for the $emailText in welsh" in {
        document.select(emailSelector).text() shouldBe emailTextWelsh
        document.select(emailValueSelector).text() shouldBe "test@email.com"
      }

      s"has a $confirmText button in welsh" in {
        document.select(confirmSelector).text() shouldBe confirmTextWelsh
      }
    }
  }

  private def getSuccessPage(language: Language): Document = {

    stubsSetup

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/complete-contact-details")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def stubsSetup: StubMapping = {

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    val authResponseBody =
      """{ "affinityGroup": "Organisation", "credentialRole": "User", "optionalItmpName": {"givenName": "Test First Name", "familyName": "Test Last Name"}, "email": "test@test.com", "groupIdentifier": "1", "externalId": "3", "confidenceLevel": 200}"""

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody(authResponseBody)
        }
    }

    val testGroup = GroupAccount(
      id = 1L,
      groupId = "1L",
      companyName = "Test name",
      addressId = 1L,
      email = "test@email.com",
      phone = "1234567890",
      isAgent = false,
      agentCode = Some(1L))

    stubFor(
      get("/property-linking/groups?groupId=1")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testGroup).toString())
        }
    )

    stubFor(
      get("/property-linking/individuals?externalId=3")
        .willReturn(notFound)
    )

    val testAddress = Address(
      addressUnitId = Some(1L),
      line1 = "Test 1",
      line2 = "Test 2",
      line3 = "Test 3",
      line4 = "Test 4",
      postcode = "TF4 9ER")

    stubFor(
      get("/property-linking/address/1")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAddress).toString())
        }
    )
  }

  "RegistrationController post method for a new admin in organisation" should {
    "redirect to confirmation page" in {
      stubsSetup

      val authResponseBody =
        """{ "affinityGroup": "Organisation", "credentialRole": "User", "optionalItmpName": {"givenName": "Test First Name", "familyName": "Test Last Name"}, "email": "test@test.com", "groupIdentifier": "1", "externalId": "3", "confidenceLevel": 200}"""

      stubFor {
        post("/auth/authorise")
          .willReturn {
            aResponse.withStatus(OK).withBody(authResponseBody)
          }
      }

      stubFor {
        get("/property-linking/individuals?externalId=3")
          .willReturn {
            aResponse.withStatus(OK).withBody(Json.toJson(detailedIndividualAccount).toString())
          }
      }

      val testGroup = GroupAccount(
        id = 1L,
        groupId = "1L",
        companyName = "Test name",
        addressId = 1L,
        email = "test@email.com",
        phone = "1234567890",
        isAgent = false,
        agentCode = None)

      stubFor {
        get("/property-linking/groups?groupId=1")
          .willReturn {
            aResponse.withStatus(OK).withBody(Json.toJson(testGroup).toString())
          }
      }

      stubFor {
        post("/property-linking/groups")
          .willReturn {
            aResponse.withStatus(OK).withBody(Json.obj("id" -> 1L).toString())
          }
      }

      stubFor {
        post("/property-linking/address")
          .willReturn {
            aResponse.withStatus(OK).withBody(Json.obj("id" -> 1L).toString())
          }
      }

      stubFor {
        post("/property-linking/individuals")
          .willReturn {
            aResponse.withStatus(OK).withBody(Json.obj("id" -> 1L).toString())
          }
      }

      postForm("/business-rates-property-linking/create-confirmation?personId=2")

    }

  }

  private def postForm(redirectUrl: String) = {

    val res = await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/complete-existing-business-contact-details-uplift")
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = ""))

    res.status shouldBe SEE_OTHER
    res.headers("Location").head shouldBe redirectUrl

  }
}
