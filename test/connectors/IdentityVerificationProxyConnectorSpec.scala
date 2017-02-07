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
import org.apache.http.client.methods.HttpGet
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers._
import org.mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import resources._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpReads}
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.Future
import org.scalacheck.Arbitrary._
import play.api.libs.json.Writes
import play.mvc.Http

class IdentityVerificationProxyConnectorSpec extends PlaySpec with MockitoSugar with GeneratorDrivenPropertyChecks  {
  implicit val headerCarrier = HeaderCarrier()
  import scala.concurrent.ExecutionContext.Implicits.global
/*
  "IdentityVerificationProxy" must {

    "make a successful POST to Identity Verification Proxy Service" in {
      pending
      val mockLink = mock[Link]
      val mockHttp = mock[HttpGet with HttpPost]
      mockHttp.POST[Journey, Link](anyString, any[Journey], any[Seq[(String, String)]])(
        implicitly[Writes[Journey]], implicitly[HttpReads[Link]], implicitly[HeaderCarrier]). returns Future.successful(mockLink)

      val connector = new IdentityVerificationProxyConnector(mockHttp)
      forAll { (ivDetails: IVDetails, expiryDate: Option[LocalDate]) =>
       whenReady(connector.start("completionUrl", "failureUrl", ivDetails, expiryDate)) { link =>
         link must be (mockLink)
       }
      }
    }
  }
*/
}


