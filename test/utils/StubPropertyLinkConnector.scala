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
import models._
import session.LinkingSessionRequest
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object StubPropertyLinkConnector extends PropertyLinkConnector(StubHttp) {

  private var stubbedLinks: Seq[PropertyLink] = Nil
  private var stubbedClientProperties: Seq[ClientProperty] = Nil

  override def linkToProperty(linkBasis: LinkBasis)(implicit request: LinkingSessionRequest[_]): Future[Unit] = Future.successful(())

  override def linkedProperties(organisationId: Int)(implicit hc: HeaderCarrier) = {
    Future.successful(stubbedLinks)
  }

  override def get(organisationId: Int, authorisationId: Long)(implicit hc: HeaderCarrier) = Future.successful {
    stubbedLinks.find(x => {x.authorisationId == authorisationId && x.organisationId == organisationId})
  }

  override def assessments(linkId: Long)(implicit hc: HeaderCarrier) = Future.successful {
    stubbedLinks.find(_.authorisationId == linkId) match {
      case Some(link) => link.assessment
      case None => Nil
    }
  }

  override def clientProperties(userOrgId: Long, agentOrgId: Int)(implicit hc: HeaderCarrier): Future[Seq[ClientProperty]] = {
    Future.successful(stubbedClientProperties)
  }
  def stubLink(link: PropertyLink) = {
    stubbedLinks :+= link
  }

  def stubClientProperties(clientProperty: ClientProperty) = {
    stubbedClientProperties :+= clientProperty
  }

  def reset() = {
    stubbedLinks = Nil
    stubbedClientProperties = Nil
  }
}
