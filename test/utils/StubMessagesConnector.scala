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

package utils

import connectors.MessagesConnector
import models.messages.{MessageCount, MessagePagination, MessageSearchResults}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object StubMessagesConnector extends MessagesConnector(StubHttp, StubServicesConfig) {
  override def getMessages(orgId: Long, pagination: MessagePagination)
                          (implicit hc: HeaderCarrier): Future[MessageSearchResults] = Future.successful {
    MessageSearchResults(0, 0, Nil)
  }

  override def countUnread(orgId: Long)(implicit hc: HeaderCarrier): Future[MessageCount] = Future.successful(MessageCount(0, 0))
}
