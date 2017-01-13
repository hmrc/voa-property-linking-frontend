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

import connectors.propertyLinking.PropertyLinkConnector
import connectors.{CapacityDeclaration, FileInfo}
import models.{DetailedPropertyLink, LinkBasis, Property, PropertyLink}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object StubPropertyLinkConnector extends PropertyLinkConnector(StubHttp) {

  private var stubbedLinks: Seq[DetailedPropertyLink] = Nil

  override def linkToProperty(property: Property,
                              organisationId: Int,
                              individualId: Int,
                              capacityDeclaration: CapacityDeclaration,
                              submissionId: String, flag: LinkBasis,
                              fileInfo: Option[FileInfo]
                             )(implicit hc: HeaderCarrier) = {
    Future.successful(Unit)
  }

  override def get(organisationId: Int, linkId: Int)(implicit hc: HeaderCarrier) = Future.successful {
    stubbedLinks.find(x => {x.linkId == linkId && x.organisationId == organisationId})
  }

  def stubLink(link: DetailedPropertyLink) = {
    stubbedLinks :+= link
  }

  def reset() = {
    stubbedLinks = Nil
  }
}
