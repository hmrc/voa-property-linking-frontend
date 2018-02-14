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
import utils.Formatters.{buildQueryParams, buildUppercaseQueryParams}

case class AgentPropertiesPagination(address: Option[String] = None,
                                     baref: Option[String] = None,
                                     agentName: Option[String] = None,
                                     pageNumber: Int = 1,
                                     pageSize: Int = 5,
                                     totalResults: Int = 10,
                                     sortField: AgentPropertiesSortField = AgentPropertiesSortField.LocalAuthorityReference,
                                     sortOrder: SortOrder = SortOrder.Descending) {


  def startPoint: Int = (pageNumber - 1) * pageSize + 1

  def reverseSorting: AgentPropertiesPagination = copy(sortOrder = sortOrder.reverse)

  def previousPage: AgentPropertiesPagination = copy(pageNumber = pageNumber - 1)

  def nextPage: AgentPropertiesPagination = copy(pageNumber = pageNumber + 1)

//  lazy val queryString: String =
//    s"""
//       |${address.filter(_.nonEmpty).fold("")(a => s"address=$a&")}
//       |${localAuthorityReference.filter(_.nonEmpty).fold("")(lar => s"localAuthorityReference=$lar&")}
//       |${agentName.filter(_.nonEmpty).fold("")(an => s"agentName=$an&")}
//       |page=$startPoint&
//       |size=$pageSize&
//       |sortField=$sortField&
//       |sortOrder=$sortOrder
//       |""".stripMargin.replaceAll("\n", "")

  override val toString = s"startPoint=$startPoint&pageSize=$pageSize&requestTotalRowCount=true"  +
//    buildUppercaseQueryParams("sortfield", Some(sortField.name)) +
//    buildUppercaseQueryParams("sortorder", Some(sortOrder.name)) +
    buildQueryParams("address", address) +
    buildQueryParams("baref", baref) +
    buildQueryParams("agent", agentName)
}

object AgentPropertiesPagination {
  implicit val queryStringBindable: QueryStringBindable[AgentPropertiesPagination] = new QueryStringBindable[AgentPropertiesPagination] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AgentPropertiesPagination]] = {
      def bindParam[T](key: String)(implicit qsb: QueryStringBindable[T]): Option[Either[String, T]] = qsb.bind(key, params)

      for {
        address <- bindParam[Option[String]]("address")
        baref <- bindParam[Option[String]]("baref")
        agentName <- bindParam[Option[String]]("agentName")
        pageNumber <- bindParam[Int]("pageNumber")
        pageSize <- bindParam[Int]("pageSize")
        totalResults <- bindParam[Int]("totalResults")
        sortField <- bindParam[AgentPropertiesSortField]("sortField")
        sortOrder <- bindParam[SortOrder]("sortOrder")
      } yield {
        (address, baref, agentName, pageNumber, pageSize, totalResults, sortField, sortOrder) match {
          case (Right(a), Right(bar), Right(an), Right(pn), Right(ps), Right(tr), Right(sf), Right(so)) =>
            Right(AgentPropertiesPagination(a, bar, an, pn, ps, tr, sf, so))
          case _ => Left("Unable to bind to AgentPropertiesPagination")
        }
      }
    }

    override def unbind(key: String, value: AgentPropertiesPagination): String =
      s"""
         |address=${value.address.getOrElse("")}&
         |baref=${value.baref.getOrElse("")}&
         |agentName=${value.agentName.getOrElse("")}&
         |pageNumber=${value.pageNumber}&
         |pageSize=${value.pageSize}&
         |sortField=${value.sortField}&
         |sortOrder=${value.sortOrder}
         |""".stripMargin.replaceAll("\n", "")
  }
}









