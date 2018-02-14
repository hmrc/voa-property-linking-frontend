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

package models.searchApi

import models.SortOrder
import play.api.mvc.QueryStringBindable

case class AgentPropertiesPagination(address: Option[String] = None,
                             localAuthorityReference: Option[String] = None,
                             agentName: Option[String] = None,
                             pageNumber: Int = 1,
                             pageSize: Int = 15,
                             sortField: AgentPropertiesSortField = AgentPropertiesSortField.LocalAuthorityReference,
                             sortOrder: SortOrder = SortOrder.Descending) {


  lazy val startPoint: Int = (pageNumber - 1) * pageSize + 1

  def reverseSorting: AgentPropertiesPagination = copy(sortOrder = sortOrder.reverse)

  def previousPage: AgentPropertiesPagination = copy(pageNumber = pageNumber - 1)

  def nextPage: AgentPropertiesPagination = copy(pageNumber = pageNumber + 1)

  lazy val queryString: String =
    s"""
       |${address.filter(_.nonEmpty).fold("")(a => s"address=$a&")}
       |${localAuthorityReference.filter(_.nonEmpty).fold("")(lar => s"localAuthorityReference=$lar&")}
       |${agentName.filter(_.nonEmpty).fold("")(an => s"agentName=$an&")}
       |startPoint=$startPoint&
       |pageSize=$pageSize&
       |sortField=$sortField&
       |sortOrder=$sortOrder
       |""".stripMargin.replaceAll("\n", "")
}

object AgentPropertiesPagination {
  implicit val queryStringBindable: QueryStringBindable[AgentPropertiesPagination] = new QueryStringBindable[AgentPropertiesPagination] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AgentPropertiesPagination]] = {
      def bindParam[T](key: String)(implicit qsb: QueryStringBindable[T]): Option[Either[String, T]] = qsb.bind(key, params)

      for {
        address <- bindParam[Option[String]]("address")
        localAuthorityReference <- bindParam[Option[String]]("localAuthorityReference")
        agentName <- bindParam[Option[String]]("agentName")
        pageNumber <- bindParam[Int]("pageNumber")
        pageSize <- bindParam[Int]("pageSize")
        sortField <- bindParam[AgentPropertiesSortField]("sortField")
        sortOrder <- bindParam[SortOrder]("sortOrder")
      } yield {
        (address, localAuthorityReference, agentName, pageNumber, pageSize, sortField, sortOrder) match {
          case (Right(a), Right(lar), Right(an), Right(pn), Right(ps), Right(sf), Right(so)) =>
            Right(AgentPropertiesPagination(a, lar, an, pn, ps, sf, so))
          case _ => Left("Unable to bind to AgentPropertiesPagination")
        }
      }
    }

    override def unbind(key: String, value: AgentPropertiesPagination): String =
      s"""
         |address=${value.address.getOrElse("")}&
         |localAuthorityReference=${value.localAuthorityReference.getOrElse("")}&
         |agentName=${value.agentName.getOrElse("")}&
         |pageNumber=${value.pageNumber}&
         |pageSize=${value.pageSize}&
         |sortField=${value.sortField}&
         |sortOrder=${value.sortOrder}
         |""".stripMargin.replaceAll("\n", "")
  }
}









