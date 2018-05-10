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

import controllers.ControllerSpec
import models.messages.{Message, MessageCount, MessagePagination, MessageSearchResults}
import org.scalacheck.Arbitrary._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import resources._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.StubServicesConfig
import play.api.http.Status._

class MessagesConnectorSpec extends ControllerSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new MessagesConnector(mockWSHttp, StubServicesConfig) {
      override lazy val baseUrl: String = "tst-url"
    }
  }

  "getMessages" must "return valid message search results" in new Setup {
    val messageSearchResults = arbitrary[MessageSearchResults].sample.get

    mockHttpGET[MessageSearchResults]("tst-url", messageSearchResults)
    whenReady(connector.getMessages(1, MessagePagination()))(_ mustBe messageSearchResults)
  }

  "getMessage" must "return a valid message" in new Setup {
    val message = arbitrary[Message].sample.get

    mockHttpGET[Message]("tst-url", message)
    whenReady(connector.getMessage(1, "MESSAGE_ID"))(_ mustBe Some(message))
  }

  "countUnread" must "return the total and unread count of messages" in new Setup {
    val validMessageCount = MessageCount(5, 3)

    mockHttpGET[MessageCount]("tst-url", validMessageCount)
    whenReady(connector.countUnread(1))(_ mustBe validMessageCount)
  }

  "markAsRead" must "successfully mark a message as read" in new Setup {
    mockHttpPUT[JsValue, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.markAsRead("MESSAGE_ID", "GG_ID"))(_ mustBe ())
  }

}
