package controllers.registration

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import org.jsoup.Jsoup
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}

import java.util.UUID

class RegistrationUserDetailsISpec extends ISpecBase {
  val testSessionId = s"stubbed-${UUID.randomUUID}"

  def getResult(affinityGroup: AffinityGroup, language: Language): WSResponse = {
    val testGroupId = "testGroupId"
    val testExternalId = "testExternalId"

    val authResponseBody = {
      Json.obj(
        "affinityGroup"   -> affinityGroup,
        "credentialRole"  -> "Admin",
        "optionalName"    -> Json.obj("name" -> "Test Name", "lastName" -> "whatever"),
        "email"           -> "test@test.com",
        "groupIdentifier" -> testGroupId,
        "externalId"      -> testExternalId,
        "confidenceLevel" -> 200
      )
    }.toString

    stubFor(
      post("/auth/authorise")
        .willReturn(okJson(authResponseBody))
    )

    affinityGroup match {
      case Individual =>
        stubFor(
          get(s"/property-linking/individuals?externalId=$testExternalId")
            .willReturn(notFound)
        )
      case Organisation =>
        stubFor(
          get(s"/property-linking/groups?groupId=$testGroupId")
            .willReturn(notFound)
        )
    }

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/complete-contact-details")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )
  }

  case object EnglishExpectedText {
    val expectedPageTitle = "Complete your contact details - Valuation Office Agency - GOV.UK"
    val expectedFirstNameFieldLabel = "First name"
    val expectedContactDetailsUsageText = "We use your contact details to send you correspondence related to the service and your account."
  }
  case object WelshExpectedText {
    val expectedPageTitle = "Cwblhewch eich manylion cyswllt - Valuation Office Agency - GOV.UK"
    val expectedFirstNameFieldLabel = "Enw cyntaf"
    val expectedContactDetailsUsageText = "Rydym yn defnyddio eich manylion cyswllt i anfon gohebiaeth atoch sy’n ymwneud â’r gwasanaeth a’ch cyfrif."
  }

  "GET /complete-contact-details" when {
    "the user is not already registered for VOA" when {
      "the user is requesting English" when {
        "the user is an indvidual" should {
          lazy val res: WSResponse = getResult(AffinityGroup.Individual, English)
          lazy val document = Jsoup.parse(res.body)

          "have a status of OK" in {
            res.status shouldBe OK
          }
          
          s"have a title of '${EnglishExpectedText.expectedPageTitle}'" in {
            document.title() shouldBe EnglishExpectedText.expectedPageTitle
          }

          "have a paragraph beneath the heading explaining about how contact details are used" in {
            val elementId = "contactDetailsUse"
            document.getElementById(elementId) should not be null
            document.getElementById(elementId).text shouldBe EnglishExpectedText.expectedContactDetailsUsageText
          }

          lazy val formGroups = document.getElementsByClass("govuk-form-group")
          "have a field to capture first name" which {
            lazy val firstNameGroup = formGroups.get(0)
            s"has a label of '${EnglishExpectedText.expectedFirstNameFieldLabel}'" in {
              firstNameGroup.getElementsByClass("govuk-label").text shouldBe EnglishExpectedText.expectedFirstNameFieldLabel
            }
          }
        }

        "the user is an administrator of an organisation" should {
          lazy val res: WSResponse = getResult(AffinityGroup.Organisation, English)
          lazy val document = Jsoup.parse(res.body)

          "have a status of OK" in {
            res.status shouldBe OK
          }

          s"have a title of '${EnglishExpectedText.expectedPageTitle}'" in {
            document.title() shouldBe EnglishExpectedText.expectedPageTitle
          }

          "have a paragraph beneath the heading explaining about how contact details are used" in {
            val elementId = "contactDetailsUse"
            document.getElementById(elementId) should not be null
            document.getElementById(elementId).text shouldBe EnglishExpectedText.expectedContactDetailsUsageText
          }

          lazy val formGroups = document.getElementsByClass("govuk-form-group")
          "have a field to capture first name" which {
            lazy val firstNameGroup = formGroups.get(0)
            s"has a label of '${EnglishExpectedText.expectedFirstNameFieldLabel}'" in {
              firstNameGroup.getElementsByClass("govuk-label").text shouldBe EnglishExpectedText.expectedFirstNameFieldLabel
            }
          }
        }
      }
      "the user is requesting Welsh" when {
        "the user is an indvidual" should {
          lazy val res: WSResponse = getResult(AffinityGroup.Individual, Welsh)
          lazy val document = Jsoup.parse(res.body)

          "have a status of OK" in {
            res.status shouldBe OK
          }

          s"have a title of '${WelshExpectedText.expectedPageTitle}'" in {
            document.title() shouldBe WelshExpectedText.expectedPageTitle
          }

          "have a paragraph beneath the heading explaining about how contact details are used" in {
            val elementId = "contactDetailsUse"
            document.getElementById(elementId) should not be null
            document.getElementById(elementId).text shouldBe WelshExpectedText.expectedContactDetailsUsageText
          }

          lazy val formGroups = document.getElementsByClass("govuk-form-group")
          "have a field to capture first name" which {
            lazy val firstNameGroup = formGroups.get(0)
            s"has a label of '${WelshExpectedText.expectedFirstNameFieldLabel}'" in {
              firstNameGroup.getElementsByClass("govuk-label").text shouldBe WelshExpectedText.expectedFirstNameFieldLabel
            }
          }
        }

        "the user is an administrator of an organisation" should {
          lazy val res: WSResponse = getResult(AffinityGroup.Organisation, Welsh)
          lazy val document = Jsoup.parse(res.body)

          "have a status of OK" in {
            res.status shouldBe OK
          }

          s"have a title of '${WelshExpectedText.expectedPageTitle}'" in {
            document.title() shouldBe WelshExpectedText.expectedPageTitle
          }

          "have a paragraph beneath the heading explaining about how contact details are used" in {
            val elementId = "contactDetailsUse"
            document.getElementById(elementId) should not be null
            document.getElementById(elementId).text shouldBe WelshExpectedText.expectedContactDetailsUsageText
          }

          lazy val formGroups = document.getElementsByClass("govuk-form-group")
          "have a field to capture first name" which {
            lazy val firstNameGroup = formGroups.get(0)
            s"has a label of '${WelshExpectedText.expectedFirstNameFieldLabel}'" in {
              firstNameGroup.getElementsByClass("govuk-label").text shouldBe WelshExpectedText.expectedFirstNameFieldLabel
            }
          }
        }
      }
    }
  }
}
