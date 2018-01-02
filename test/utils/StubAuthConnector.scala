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

package utils

import connectors.VPLAuthConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object StubAuthConnector extends VPLAuthConnector(StubServicesConfig, StubHttp) {
  private var externalId: Option[String] = None
  private var groupId: Option[String] = None

  def stubExternalId(id: String): Unit = {
    externalId = Some(id)
  }

  override def getExternalId(ctx: AuthContext)(implicit hc: HeaderCarrier): Future[String] = Future.successful(externalId.getOrElse(throw new Exception("External id not stubbed")))

  def stubGroupId(groupId: String) = {
    this.groupId = Some(groupId)
  }

  override def getGroupId(authContext: AuthContext)(implicit hc: HeaderCarrier) = Future.successful {
    groupId.getOrElse(throw new Exception("Group id not stubbed"))
  }

  def reset() = {
    externalId = None
    groupId = None
  }
}
