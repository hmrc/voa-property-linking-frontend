/*
 * Copyright 2016 HM Revenue & Customs
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

import connectors.propertyLinking.PropertyLinkConnector
import connectors.{CapacityDeclaration, FileInfo, PropertyLink}
import models.{LinkBasis, Property}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object StubPropertyLinkConnector extends PropertyLinkConnector(StubHttp) {

  private var stubbedLinks: Seq[PropertyLink] = Nil

  override def linkToProperty(property: Property,
                              userId: String,
                              capacityDeclaration: CapacityDeclaration,
                              submissionId: String, flag: LinkBasis,
                              fileInfo: Option[FileInfo]
                             )(implicit hc: HeaderCarrier) = {
    Future.successful(())
  }

  override def get(linkId: String)(implicit hc: HeaderCarrier) = Future.successful {
    stubbedLinks.find(_.linkId == linkId)
  }

  def stubLink(link: PropertyLink) = {
    stubbedLinks :+= link
  }

  def reset() = {
    stubbedLinks = Nil
  }
}
