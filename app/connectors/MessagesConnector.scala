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

import javax.inject.Inject

import config.ApplicationConfig
import models.messages.{MessageCount, MessagePagination, MessageSearchResults}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class MessagesConnector @Inject()(http: WSHttp, conf: ServicesConfig, config: ApplicationConfig)(implicit ec: ExecutionContext) {

  lazy val baseUrl: String = conf.baseUrl("property-linking") + "/property-linking"

  def getMessages(orgId: Long, pagination: MessagePagination)
                 (implicit hc: HeaderCarrier): Future[MessageSearchResults] = {
    http.GET[MessageSearchResults](s"$baseUrl/messages?recipientOrgId=$orgId&${pagination.queryString}")
  }

  def countUnread(orgId: Long)(implicit hc: HeaderCarrier): Future[MessageCount] = {
    if(config.messagesEnabled) {
      http.GET[MessageCount](s"$baseUrl/unread-messages-count/$orgId")
    } else {
      //return fake value when messaging is disabled, so every Dashboard action doesn't need to check the config, etc.
      Future.successful(MessageCount(0, 0))
    }
  }
}
