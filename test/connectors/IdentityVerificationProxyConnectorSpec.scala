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

package connectors

import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import models.IVDetails
import models.identityVerificationProxy.{Journey, Link}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.{Configs, GlobalExecutionContext}

import scala.concurrent.Future

class IdentityVerificationProxyConnectorSpec
    extends AnyFlatSpec with Matchers with MockitoSugar with ScalaCheckDrivenPropertyChecks with Configs
    with GlobalExecutionContext {

  import utils._

  implicit val headerCarrier = HeaderCarrier()

  "IdentityVerificationProxy" should "make a successful POST to Identity Verification Proxy Service" in {

    val mockLink = mock[Link]
    val mockHttp = mock[HttpClient]

    when(mockHttp.POST[Journey, Link](anyString(), any[Journey], any())(any(), any(), any(), any())) thenReturn Future
      .successful(mockLink)

    val connector = new IdentityVerificationProxyConnector(servicesConfig, mockHttp)
    forAll { (ivDetails: IVDetails) =>
      whenReady(connector.start(Journey("", "completionUrl", "failureUrl", ConfidenceLevel.L200, ivDetails))) { link =>
        link should be(mockLink)
      }
    }
  }

  it should "handle an unsuccessful POST to Identity Verification Proxy Service" in {
    val mockEx = new RuntimeException("something went wrong")
    val mockHttp = mock[HttpClient]

    when(mockHttp.POST[Journey, Link](anyString(), any[Journey], any())(any(), any(), any(), any()))
      .thenReturn(Future.failed(mockEx))

    val connector = new IdentityVerificationProxyConnector(servicesConfig, mockHttp)
    forAll { (ivDetails: IVDetails) =>
      whenReady(connector.start(Journey("", "completionUrl", "failureUrl", ConfidenceLevel.L200, ivDetails)).failed) {
        ex =>
          ex should be(mockEx)
      }
    }
  }

}
