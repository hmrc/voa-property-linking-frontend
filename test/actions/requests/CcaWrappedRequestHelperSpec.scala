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

package actions.requests

import actions.registration.requests.RequestWithUserDetails
import config.ApplicationConfig
import models.analytics.GoogleAnalyticsUserData
import models.{DetailedIndividualAccount, GroupAccount, IndividualDetails}
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.twirl.api.Html
import tests.BaseUnitSpec
import utils.{Configs, FakeViews, StubMessageControllerComponents}

class CcaWrappedRequestHelperSpec extends BaseUnitSpec with FakeViews with StubMessageControllerComponents {

  import actions.requests.CcaWrappedRequestHelper._

  implicit lazy val messageApi: MessagesApi = StubMessageControllerComponents.messagesApi
  implicit val appConfig: ApplicationConfig = Configs.applicationConfig

  abstract class Setup(sameName: Boolean = false, isAgent: Boolean = false) {
    val firstName = "first"
    val secondName = "second"
    val personName = s"$firstName $secondName"
    val companyName = "Company Name"

    val testCompanyName: String = if (sameName) personName else companyName

    val testGroupAccount: GroupAccount = groupAccount(isAgent).copy(companyName = testCompanyName)

    val testDetailedIndividualAccount: DetailedIndividualAccount =
      DetailedIndividualAccount(
        externalId = ggExternalId,
        trustId = Some("trustId"),
        organisationId = testGroupAccount.id,
        individualId = 2L,
        details = IndividualDetails(
          firstName = firstName,
          lastName = secondName,
          email = "email@asd.com",
          phone1 = "123123123",
          phone2 = None,
          addressId = 12L
        )
      )

    val testRequest: BasicAuthenticatedRequest[_] =
      BasicAuthenticatedRequest(testGroupAccount, testDetailedIndividualAccount, FakeRequest())

    val googleAnalyticsData: GoogleAnalyticsUserData = GoogleAnalyticsUserData(
      personId = testDetailedIndividualAccount.individualId.toString,
      loggedIn = "Yes",
      ccaAgent = if (isAgent) "Yes" else "No"
    )
  }

  "shouldShowTimeoutDialog" should {
    "be true for a GG-authenticated request" in new Setup {
      val request = new RequestWithUserDetails(individualUserDetails, FakeRequest())
      request.shouldShowTimeoutDialog shouldBe true
    }

    "be false for a plain request" in new Setup {
      FakeRequest().shouldShowTimeoutDialog shouldBe false
    }
  }

  "layout" should {
    "render the timeout dialog for a GG-authenticated request" in new Setup {
      val request = new RequestWithUserDetails(individualUserDetails, FakeRequest())
      val rendered = layout(
        pageTitle = "Test page",
        showTopNavigation = false
      )(
        Html("<p>hello</p>")
      )(request, messagesApi.preferred(request), appConfig)

      rendered.body should include("meta name=\"hmrc-timeout-dialog\"")
      rendered.body should include("keep-alive")
    }
  }

  "your details name" when {
    "full name is the same as company name" should {
      "return just full name" in new Setup(sameName = true) {
        testRequest.yourDetailsName.value shouldBe personName
      }
    }

    "full name is different from company name" should {
      "return 'full name - company name'" in new Setup(sameName = false) {
        testRequest.yourDetailsName.value shouldBe s"$personName - $companyName"
      }
    }
  }

  "googleAnalyticsUserData" when {
    "organisation is not an agent" should {
      "contain the correct user data" in new Setup(isAgent = false) {
        testRequest.googleAnalyticsUserData shouldBe googleAnalyticsData
      }
    }

    "organisation is an agent" should {
      "contain the correct user data" in new Setup(isAgent = true) {
        testRequest.googleAnalyticsUserData shouldBe googleAnalyticsData
      }
    }
  }
}
