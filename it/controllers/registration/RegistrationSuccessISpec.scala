package controllers.registration

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import utils.ListYearsHelpers

class RegistrationSuccessISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  val titleText = "You’ve successfully registered - Valuation Office Agency - GOV.UK"
  val headingText = "You’ve successfully registered"
  val voaIdText = "VOA Personal ID: 3164"
  val youShouldKeepText = "You should keep a note of this number in a safe place as you’ll need it if you want to reset your password."
  val youWillNotText = "You will not need it to sign in to the check and challenge service – you should continue to use your Government Gateway details to sign in."
  val ifAnyoneElseText = "If anyone else wants to register on behalf of the business, you’ll first need to add them to your Government Gateway account as an administrator or an assistant."
  val addThemTooText = "add them to your Government Gateway account"
  val weUseYourText = "We use your contact details to send you correspondence related to the service and your account."
  val infoProvidedText = "Information provided using this service is only for the purposes of checking and, if necessary, challenging and appealing the rating of non-domestic property. Use for any other purpose is a breach of the terms and conditions of service. Unlawful access may be prosecuted under the relevant legislation, including the Computer Misuse Act 1990 or the Fraud Act 2006."
  val termsLinkText = "terms and conditions"
  val continueText = "Continue"

  val titleTextWelsh = "Rydych chi wedi cofrestru’n llwyddiannus - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Rydych chi wedi cofrestru’n llwyddiannus"
  val voaIdTextWelsh = "Cyfeirnod adnabod personol y VOA (ID): 3164"
  val youShouldKeepTextWelsh = "Dylech gadw nodyn o’r rhif hwn mewn lle diogel gan y bydd ei angen arnoch os ydych am ailosod eich cyfrinair."
  val youWillNotTextWelsh = "Ni fydd angen i chi fewngofnodi i’r gwasanaeth gwirio a herio – dylech barhau i ddefnyddio eich manylion Porth y Llywodraeth i fewngofnodi."
  val ifAnyoneElseTextWelsh = "Os oes unrhyw un arall eisiau cofrestru ar ran y busnes, bydd angen i chi eu hychwanegu i’ch cyfrif Porth y Llywodraeth fel gweinyddwr neu gynorthwyydd yn gyntaf."
  val addThemTooTextWelsh = "eu hychwanegu i’ch cyfrif Porth y Llywodraeth"
  val weUseYourTextWelsh = "Rydym yn defnyddio’ch manylion cyswllt i anfon gohebiaeth atoch sy’n ymwneud â’ch cyfrif a’r gwasanaeth."
  val infoProvidedTextWelsh = "Dim ond at ddibenion gwirio y defnyddir y wybodaeth a ddarperir, ac os oes angen, herio ac apelio ardrethi eiddo annomestig. Mae’r defnydd ar gyfer unrhyw bwrpas arall yn torri telerau ac amodau’r gwasanaeth. Gellir erlyn mynediad anghyfreithlon dan y ddeddfwriaeth berthnasol, gan gynnwys Deddf Camddefnyddio Cyfrifiaduron 1990 neu Ddeddf Twyll 2006."
  val termsLinkTextWelsh = "telerau ac amodau’r"
  val continueTextWelsh = "Parhau"

  val headingSelector = "h1.govuk-panel__title"
  val voaIdSelector = "h2.govuk-panel__title"
  val youShouldKeepSelector = "#main-content > div > div > p:nth-child(2)"
  val youWillNotSelector = "#main-content > div > div > p:nth-child(3)"
  val continueSelector = "#continue"

  val addThemTooHref = "/business-rates-property-linking/add-user-to-gg"
  val termsLinkHref = "terms-and-conditions"
  val continueButtonHref = "/business-rates-dashboard/home"

  "RegistrationController success method" should {
    "Show an English registration confirmation screen with the correct text for an individual + admin" which {

      val weUseYourSelector = "#main-content > div > div > p:nth-child(4)"
      val infoProvidedSelector = "#main-content > div > div > p:nth-child(5)"
      val termsLinkSelector = "#main-content > div > div > p:nth-child(5) > a"

      lazy val document: Document = getSuccessPage(English, Individual)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text() shouldBe headingText
      }

      s"has a small header of the $voaIdText" in {
        document.select(voaIdSelector).text() shouldBe voaIdText
      }

      s"has text on the screen of $youShouldKeepText" in {
        document.select(youShouldKeepSelector).text() shouldBe youShouldKeepText
      }

      s"has text on the screen of $youWillNotText" in {
        document.select(youWillNotSelector).text() shouldBe youWillNotText
      }

      s"has text on the screen of $weUseYourText" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourText
      }

      s"has text on the screen of $infoProvidedText" in {
        document.select(infoProvidedSelector).text() shouldBe infoProvidedText
      }

      s"has a $termsLinkText link" in {
        document.select(termsLinkSelector).text() shouldBe termsLinkText
        document.select(termsLinkSelector).attr("href") shouldBe termsLinkHref
      }

      s"has a $continueText button" in {
        document.select(continueSelector).text() shouldBe continueText
        document.select(continueSelector).attr("href") shouldBe continueButtonHref
      }
    }

    "Show an English registration confirmation screen with the correct text for an organisation + admin" which {

      val ifAnyoneElseSelector = "#main-content > div > div > p:nth-child(4)"
      val addThemTooLinkSelector = "#main-content > div > div > p:nth-child(4) > a"
      val weUseYourSelector = "#main-content > div > div > p:nth-child(5)"
      val infoProvidedSelector = "#main-content > div > div > p:nth-child(6)"
      val termsLinkSelector = "#main-content > div > div > p:nth-child(6) > a"

      lazy val document: Document = getSuccessPage(English, Organisation)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text() shouldBe headingText
      }

      s"has a small header of the $voaIdText" in {
        document.select(voaIdSelector).text() shouldBe voaIdText
      }

      s"has text on the screen of $youShouldKeepText" in {
        document.select(youShouldKeepSelector).text() shouldBe youShouldKeepText
      }

      s"has text on the screen of $youWillNotText" in {
        document.select(youWillNotSelector).text() shouldBe youWillNotText
      }

      s"has text on the screen of $ifAnyoneElseText" in {
        document.select(ifAnyoneElseSelector).text() shouldBe ifAnyoneElseText
      }

      s"has a $addThemTooText link" in {
        document.select(addThemTooLinkSelector).text() shouldBe addThemTooText
        document.select(addThemTooLinkSelector).attr("href") shouldBe addThemTooHref
      }

      s"has text on the screen of $weUseYourText" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourText
      }

      s"has text on the screen of $infoProvidedText" in {
        document.select(infoProvidedSelector).text() shouldBe infoProvidedText
      }

      s"has a $termsLinkText link" in {
        document.select(termsLinkSelector).text() shouldBe termsLinkText
        document.select(termsLinkSelector).attr("href") shouldBe termsLinkHref
      }

      s"has a $continueText button" in {
        document.select(continueSelector).text() shouldBe continueText
        document.select(continueSelector).attr("href") shouldBe continueButtonHref
      }
    }

    "show a welsh registration confirmation screen with the correct text for an individual + admin" which {

      val weUseYourSelector = "#main-content > div > div > p:nth-child(4)"
      val infoProvidedSelector = "#main-content > div > div > p:nth-child(5)"
      val termsLinkSelector = "#main-content > div > div > p:nth-child(5) > a"

      lazy val document: Document = getSuccessPage(Welsh, Individual)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of $headingText in welsh" in {
        document.select(headingSelector).text() shouldBe headingTextWelsh
      }

      s"has a small header of the $voaIdText in welsh" in {
        document.select(voaIdSelector).text() shouldBe voaIdTextWelsh
      }

      s"has text on the screen of $youShouldKeepText in welsh" in {
        document.select(youShouldKeepSelector).text() shouldBe youShouldKeepTextWelsh
      }

      s"has text on the screen of $youWillNotText in welsh" in {
        document.select(youWillNotSelector).text() shouldBe youWillNotTextWelsh
      }

      s"has text on the screen of $weUseYourText in welsh" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourTextWelsh
      }

      s"has text on the screen of $infoProvidedText in welsh" in {
        document.select(infoProvidedSelector).text() shouldBe infoProvidedTextWelsh
      }

      s"has a $termsLinkText link in welsh" in {
        document.select(termsLinkSelector).text() shouldBe termsLinkTextWelsh
        document.select(termsLinkSelector).attr("href") shouldBe termsLinkHref
      }

      s"has a $continueText button in welsh" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
        document.select(continueSelector).attr("href") shouldBe continueButtonHref
      }
    }

    "show a welsh registration confirmation screen with the correct text for an organisation + admin" which {

      val ifAnyoneElseSelector = "#main-content > div > div > p:nth-child(4)"
      val addThemTooLinkSelector = "#main-content > div > div > p:nth-child(4) > a"
      val weUseYourSelector = "#main-content > div > div > p:nth-child(5)"
      val infoProvidedSelector = "#main-content > div > div > p:nth-child(6)"
      val termsLinkSelector = "#main-content > div > div > p:nth-child(6) > a"

      lazy val document: Document = getSuccessPage(Welsh, Organisation)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of $headingText in welsh" in {
        document.select(headingSelector).text() shouldBe headingTextWelsh
      }

      s"has a small header of the $voaIdText in welsh" in {
        document.select(voaIdSelector).text() shouldBe voaIdTextWelsh
      }

      s"has text on the screen of $youShouldKeepText in welsh" in {
        document.select(youShouldKeepSelector).text() shouldBe youShouldKeepTextWelsh
      }

      s"has text on the screen of $youWillNotText in welsh" in {
        document.select(youWillNotSelector).text() shouldBe youWillNotTextWelsh
      }

      s"has text on the screen of $ifAnyoneElseText in welsh" in {
        document.select(ifAnyoneElseSelector).text() shouldBe ifAnyoneElseTextWelsh
      }

      s"has a $addThemTooText link in welsh" in {
        document.select(addThemTooLinkSelector).text() shouldBe addThemTooTextWelsh
        document.select(addThemTooLinkSelector).attr("href") shouldBe addThemTooHref
      }

      s"has text on the screen of $weUseYourText in welsh" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourTextWelsh
      }

      s"has text on the screen of $infoProvidedText in welsh" in {
        document.select(infoProvidedSelector).text() shouldBe infoProvidedTextWelsh
      }

      s"has a $termsLinkText link in welsh" in {
        document.select(termsLinkSelector).text() shouldBe termsLinkTextWelsh
        document.select(termsLinkSelector).attr("href") shouldBe termsLinkHref
      }

      s"has a $continueText button in welsh" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
        document.select(continueSelector).attr("href") shouldBe continueButtonHref
      }
    }

  }

  private def getSuccessPage(language: Language, affinityGroup: AffinityGroup): Document = {

    stubsSetup(affinityGroup)

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/create-success?personId=3164")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def stubsSetup(affinityGroup: AffinityGroup): StubMapping = {

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    val authResponseBody = s"""{ "affinityGroup": "$affinityGroup", "credentialRole": "Admin", "optionalName": {"name": "Test Name", "lastName": "whatever"}, "email": "test@test.com", "groupIdentifier": "group-id", "externalId": "external-id", "confidenceLevel": 200}"""

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody(authResponseBody)
        }
    }

  }

}
