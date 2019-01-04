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

package utils

import connectors.MessagesConnector
import models.messages.{Message, MessageCount, MessagePagination, MessageSearchResults}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

object StubMessagesConnector extends MessagesConnector(StubHttp, StubServicesConfig) {

  private var stubbedMessageSearchResult: Option[MessageSearchResults] = None

  private var stubbedMessageCount: Option[MessageCount] = None

  private var stubbedMessage: Option[Message] = None

  def stubMessageSearchResults(messages: MessageSearchResults): Unit = {
    stubbedMessageSearchResult = Some(messages)
  }

  def stubMessageCount(messageCount: MessageCount): Unit = {
    stubbedMessageCount = Some(messageCount)
  }

  def stubMessage(message: Message): Unit = {
    stubbedMessage = Some(message)
  }

  def reset() {
    stubbedMessageSearchResult = None
    stubbedMessageCount = None
    stubbedMessage = None
  }

  override def getMessage(orgId: Long, messageId: String)
                          (implicit hc: HeaderCarrier): Future[Option[Message]] = Future.successful(stubbedMessage)

  override def getMessages(orgId: Long, pagination: MessagePagination)
                          (implicit hc: HeaderCarrier): Future[MessageSearchResults] = Future.successful(stubbedMessageSearchResult.getOrElse(MessageSearchResults(0, 0, Nil)))

  override def countUnread(orgId: Long)
                          (implicit hc: HeaderCarrier): Future[MessageCount] = Future.successful(stubbedMessageCount.getOrElse(MessageCount(0,0)))

  override def markAsRead(messageId: String, ggId: String)
                          (implicit hc: HeaderCarrier): Future[Unit] = Future.successful()


}
