/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers

import com.builtamont.play.pdf.PdfGenerator
import config.ApplicationConfig
import models.searchApi._
import connectors.{AgentsConnector, Authenticated, DraftCases, GroupAccounts}
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import utils._

import scala.collection.JavaConverters._

class ManagePropertiesSpec extends VoaPropertyLinkingSpec {

  "The manage properties page" must "return redirect" in {

    setup()

    val res = TestDashboardController.manageProperties()(FakeRequest())
    status(res) mustBe SEE_OTHER
  }

  private def setup(numberOfLinks: Int = 15) = {
    val groupAccount: GroupAccount = arbitrary[GroupAccount]
    val individualAccount: DetailedIndividualAccount = arbitrary[DetailedIndividualAccount]

    var arbitraryOwnerAuthorisation: Seq[OwnerAuthorisation] = Nil

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individualAccount)))
    (1 to numberOfLinks) foreach { _ =>
      arbitraryOwnerAuthorisation :+= arbitrary[OwnerAuthorisation].copy(authorisationId = groupAccount.id.toLong)
    }

    StubPropertyLinkConnector.stubOwnerAuthResult(OwnerAuthResult(start =1,
      size = numberOfLinks,
      total = numberOfLinks,
      filterTotal = numberOfLinks,
      authorisations = arbitraryOwnerAuthorisation))
  }

  private object TestDashboardController extends Dashboard(
    mock[DraftCases],
    StubPropertyLinkConnector,
    mock[AgentsConnector],
    mock[GroupAccounts],
    StubAuthentication,
    mock[PdfGenerator]
  )
}
