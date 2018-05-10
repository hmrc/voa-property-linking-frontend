/*
 * Copyright 2018 HM Revenue & Customs
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

package connectors

import controllers.VoaPropertyLinkingSpec
import models.DraftCase
import uk.gov.hmrc.http.HeaderCarrier
import utils.StubServicesConfig
import scala.concurrent.ExecutionContext.global

class DraftCasesSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()
  implicit val ec = global

  class Setup {
    val connector = new DraftCases(mockWSHttp, StubServicesConfig)(ec) {
      override lazy val checkUrl: String = "tst-url"
    }
  }

  "get" must "return a sequence of draft cases" in new Setup {
    val draftCases = mock[Seq[DraftCase]]

    mockHttpGET[Seq[DraftCase]]("tst-url", draftCases)
    whenReady(connector.get(1))(_ mustBe draftCases)
  }

  "delete" must "delete a draft case" in new Setup {
    mockHttpDELETE[String]("tst-url", "DELETED")
    whenReady(connector.delete("DRAFT_CASE_ID"))(_ mustBe "DELETED")
  }

}
