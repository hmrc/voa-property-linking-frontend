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

package auditing

import config.AuditServiceConnector
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext

object AuditingService extends AuditingService {
  val auditConnector = AuditServiceConnector
}

trait AuditingService {

  def auditConnector: AuditConnector

  def sendEvent[A: Writes](auditType: String, obj: A)(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[_]): Unit = {
    val event = eventFor(auditType, obj)
    auditConnector.sendExtendedEvent(event)
  }

  private def eventFor[A: Writes](auditType: String, obj: A)(implicit hc: HeaderCarrier, request: Request[_]) = {
    ExtendedDataEvent(
      auditSource = "voa-property-linking-frontend",
      auditType = auditType,
      tags = hc.headers.toMap ++ Map("Ip Address" -> request.remoteAddress),
      detail = Json.toJson(obj)
    )
  }
}