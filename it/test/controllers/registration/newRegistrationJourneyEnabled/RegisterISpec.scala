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

package controllers.registration.newRegistrationJourneyEnabled

import base.{HtmlComponentHelpers, ISpecBase}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.test.Helpers._
import utils.ListYearsHelpers

class RegisterISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  override lazy val extraConfig: Map[String, String] =
    Map("featureFlags.newRegistrationJourneyEnabled" -> "true")

  val titleText = "Register to check and challenge your business rates valuation - Valuation Office Agency - GOV.UK"
  val headingText = "Register to check and challenge your business rates valuation"
  val registerForThisServiceText = "Register for this service to:"
  val checkYourPropertyDetailsText = "check your property details"
  val challengeYourValuationText = "challenge your valuation"
  val appointAnAgentText = "appoint an agent to act on your behalf"
  val alreadyCreatedAnAccountText = "Sign in if you have already created an account for this service."
  val ifOtherPeopleText = "If other people from your organisation need online sign in details and you’ve already created them, you will need to add them to your business Government Gateway account."


  val titleTextWelsh = "Cofrestrwch i wirio a herio prisiad eich ardrethi busnes - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Cofrestrwch i wirio a herio prisiad eich ardrethi busnes"
  val registerForThisServiceTextWelsh = "Cofrestrwch ar gyfer y gwasanaeth hwn i:"
  val checkYourPropertyDetailsTextWelsh = "wirio manylion eich eiddo"
  val challengeYourValuationTextWelsh = "herio’ch prisiad"
  val appointAnAgentTextWelsh = "penodi asiant i weithredu ar eich rhan"
  val alreadyCreatedAnAccountTextWelsh = "Mewngofnodi os rydych eisoes wedi creu cyfrif ar gyfer y gwasanaeth hwn."
  val ifOtherPeopleTextWelsh = "Os oes angen manylion mewngofnodi ar-lein ar bobl eraill o’ch sefydliad a’ch bod eisoes wedi’u creu, bydd angen i chi eu hychwanegu at eich cyfrif busnes Porth y Llywodraeth."

  val headingSelector = "#main-content > div > div > h2"
  val registerForThisServiceSelector = "#main-content > div > div > p:nth-child(2)"
  val checkYourPropertyDetailsSelector = "#main-content > div > div > ul:nth-child(3) > li:nth-child(1)"
  val challengeYourValuationSelector = "#main-content > div > div > ul:nth-child(3) > li:nth-child(2)"
  val appointAnAgentSelector = "#main-content > div > div > ul:nth-child(3) > li:nth-child(3)"
  val alreadyCreatedAnAccountSelector = "#main-content > div > div > p:nth-child(4)"
  val alreadyCreatedAnAccountHrefSelector = "#main-content > div > div > p:nth-child(4) > a"
  val ifOtherPeopleSelector = "#main-content > div > div > p:nth-child(5)"
  val ifOtherPeopleHrefSelector = "#main-content > div > div > p:nth-child(5) > a"

  val alreadyCreatedAnAccountHref = "/business-rates-property-linking/login"
  val ifOtherPeopleHref = "/business-rates-property-linking/add-user-to-gg"


  "Register controller method" should {
    "Show an English registration screen with the correct text" which {

      lazy val document: Document = getRegisterPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text() shouldBe headingText
      }

      s"has '$registerForThisServiceText' text" in {
        document.select(registerForThisServiceSelector).text() shouldBe registerForThisServiceText
      }

      s"has '$checkYourPropertyDetailsText' text" in {
        document.select(checkYourPropertyDetailsSelector).text() shouldBe checkYourPropertyDetailsText
      }

      s"has '$challengeYourValuationText' text" in {
        document.select(challengeYourValuationSelector).text() shouldBe challengeYourValuationText
      }

      s"has '$appointAnAgentText' text" in {
        document.select(appointAnAgentSelector).text() shouldBe appointAnAgentText
      }

      s"has '$alreadyCreatedAnAccountText' text" in {
        document.select(alreadyCreatedAnAccountSelector).text() shouldBe alreadyCreatedAnAccountText
        document.select(alreadyCreatedAnAccountHrefSelector).attr("href") shouldBe alreadyCreatedAnAccountHref
      }

      s"has '$ifOtherPeopleText' text" in {
        document.select(ifOtherPeopleSelector).text() shouldBe ifOtherPeopleText
        document.select(ifOtherPeopleHrefSelector).attr("href") shouldBe ifOtherPeopleHref
      }

    }

    "Show a Welsh registration page with the correct text" which {

      lazy val document: Document = getRegisterPage(Welsh)

      s"has a title of $titleText in Welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of $headingText in Welsh" in {
        document.select(headingSelector).text() shouldBe headingTextWelsh
      }

      s"has '$registerForThisServiceText' text in Welsh" in {
        document.select(registerForThisServiceSelector).text() shouldBe registerForThisServiceTextWelsh
      }

      s"has '$checkYourPropertyDetailsText' text in Welsh" in {
        document.select(checkYourPropertyDetailsSelector).text() shouldBe checkYourPropertyDetailsTextWelsh
      }

      s"has '$challengeYourValuationText' text in Welsh" in {
        document.select(challengeYourValuationSelector).text() shouldBe challengeYourValuationTextWelsh
      }

      s"has '$appointAnAgentText' text in Welsh" in {
        document.select(appointAnAgentSelector).text() shouldBe appointAnAgentTextWelsh
      }

      s"has '$alreadyCreatedAnAccountText' text in Welsh" in {
        document.select(alreadyCreatedAnAccountSelector).text() shouldBe alreadyCreatedAnAccountTextWelsh
        document.select(alreadyCreatedAnAccountHrefSelector).attr("href") shouldBe alreadyCreatedAnAccountHref
      }

      s"has '$ifOtherPeopleText' text in Welsh" in {
        document.select(ifOtherPeopleSelector).text() shouldBe ifOtherPeopleTextWelsh
        document.select(ifOtherPeopleHrefSelector).attr("href") shouldBe ifOtherPeopleHref
      }
    }
  }

  private def getRegisterPage(language: Language): Document = {


    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
