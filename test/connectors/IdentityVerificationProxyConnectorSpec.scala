/*
 * Copyright 2017 HM Revenue & Customs
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
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play._
import org.scalatest.concurrent.ScalaFutures._
import org.scalacheck.Arbitrary._
import uk.gov.hmrc.play.http.HeaderCarrier
import org.mockito.Mockito._
import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import resources._
import uk.gov.hmrc.play.http.ws.WSHttp
import utils.{NoMetricsOneAppPerSuite, StubServicesConfig}

import scala.concurrent.Future

class IdentityVerificationProxyConnectorSpec extends FlatSpec with MustMatchers with MockitoSugar
  with GeneratorDrivenPropertyChecks with NoMetricsOneAppPerSuite {

  implicit val headerCarrier = HeaderCarrier()

  import scala.concurrent.ExecutionContext.Implicits.global

  "IdentityVerificationProxy" must "make a successful POST to Identity Verification Proxy Service" in {

    val mockLink = mock[Link]
    val mockHttp = mock[WSHttp]

    when(mockHttp.POST[Journey, Link](anyString(), any[Journey], any())(any(), any(), any())) thenReturn (Future.successful(mockLink))

    val connector = new IdentityVerificationProxyConnector(StubServicesConfig, mockHttp)
    forAll { (ivDetails: IVDetails, expiryDate: Option[LocalDate]) =>
      whenReady(connector.start("completionUrl", "failureUrl", ivDetails, expiryDate)) { link =>
        link must be(mockLink)
      }
    }
  }

  it must "handle an unsuccessful POST to Identity Verification Proxy Service" in {
    val mockEx = new RuntimeException("something went wrong")
    val mockHttp = mock[WSHttp]

    when(mockHttp.POST[Journey, Link](anyString(), any[Journey], any())(any(), any(), any())).thenReturn(Future.failed(mockEx))

    val connector = new IdentityVerificationProxyConnector(StubServicesConfig, mockHttp)
    forAll { (ivDetails: IVDetails, expiryDate: Option[LocalDate]) =>
      whenReady(connector.start("completionUrl", "failureUrl", ivDetails, expiryDate).failed) { ex =>
        ex must be(mockEx)
      }
    }
  }

}


