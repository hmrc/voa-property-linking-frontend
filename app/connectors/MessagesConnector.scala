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

import javax.inject.Inject

import config.{ApplicationConfig, WSHttp}
import models.messages.{Message, MessageCount, MessagePagination, MessageSearchResults}
import play.api.libs.json.{JsNull, JsValue}
import uk.gov.hmrc.play.config.inject.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

class MessagesConnector @Inject()(http: WSHttp, conf: ServicesConfig)(implicit ec: ExecutionContext) {

  lazy val baseUrl: String = conf.baseUrl("property-linking") + "/property-linking"

  def getMessage(orgId: Long, messageId: String)(implicit hc: HeaderCarrier): Future[Option[Message]] = {
    http.GET[Message](s"$baseUrl/message/$orgId/$messageId") map {
      Some.apply
    }
  }

  def getMessages(orgId: Long, pagination: MessagePagination)
                 (implicit hc: HeaderCarrier): Future[MessageSearchResults] = {
    http.GET[MessageSearchResults](s"$baseUrl/messages?recipientOrgId=$orgId&${pagination.queryString}")
  }

  def countUnread(orgId: Long)(implicit hc: HeaderCarrier): Future[MessageCount] = {
    /*
    VTCCA-1846 Live Issue:
    The below is temporary as there is a live issue for users with large numbers of messages.
    The API call is timing out and preventing users from accessing the dashboard, once
    the longer term fix is in place we need to revert this.
    */

    if (Seq(1808).contains(orgId)) {
      Future.successful(MessageCount(0, 0))
    } else {
      http.GET[MessageCount](s"$baseUrl/unread-messages-count/$orgId")
    }
  }

  def markAsRead(messageId: String, ggId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PUT[JsValue, HttpResponse](s"$baseUrl/message/$messageId?readBy=$ggId", JsNull) map { _ => () }
  }
}
