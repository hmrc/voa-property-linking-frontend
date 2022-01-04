/*
 * Copyright 2022 HM Revenue & Customs
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
import models.searchApi.AgentPropertiesFilter.Both
import play.api.mvc.QueryStringBindable
import utils.Formatters.{buildQueryParams, buildUppercaseQueryParams}

case class AgentPropertiesParameters(
      agentCode: Long,
      address: Option[String] = None,
      agentNameFilter: Option[String] = None,
      pageNumber: Int = 1,
      pageSize: Int = 15,
      sortField: AgentPropertiesSortField = AgentPropertiesSortField.Address,
      sortOrder: SortOrder = SortOrder.Ascending,
      agentAppointed: String = Both.name) {

  def startPoint: Int = (pageNumber - 1) * pageSize + 1

  def reverseSorting: AgentPropertiesParameters = copy(sortOrder = sortOrder.reverse)

  def previousPage: AgentPropertiesParameters = copy(pageNumber = pageNumber - 1)

  def nextPage: AgentPropertiesParameters = copy(pageNumber = pageNumber + 1)

  def clear: AgentPropertiesParameters = copy(address = None, agentNameFilter = None)

  lazy val queryString =
    s"agentCode=$agentCode&startPoint=$startPoint&pageSize=$pageSize&requestTotalRowCount=true" +
      buildUppercaseQueryParams("sortfield", Some(sortField.name)) +
      buildUppercaseQueryParams("sortorder", Some(sortOrder.name)) +
      buildQueryParams("address", address) +
      buildQueryParams("agent", agentNameFilter) +
      s"&agentAppointed=$agentAppointed"

}

object AgentPropertiesParameters {
  implicit val queryStringBindable: QueryStringBindable[AgentPropertiesParameters] =
    new QueryStringBindable[AgentPropertiesParameters] {
      override def bind(
            key: String,
            params: Map[String, Seq[String]]): Option[Either[String, AgentPropertiesParameters]] = {
        def bindParam[T](key: String)(implicit qsb: QueryStringBindable[T]): Option[Either[String, T]] =
          qsb.bind(key, params)

        for {
          agentCode      <- bindParam[Long]("agentCode")
          address        <- bindParam[Option[String]]("address")
          agentName      <- bindParam[Option[String]]("agentName")
          pageNumber     <- bindParam[Int]("pageNumber")
          pageSize       <- bindParam[Int]("pageSize")
          sortField      <- bindParam[AgentPropertiesSortField]("sortField")
          sortOrder      <- bindParam[SortOrder]("sortOrder")
          agentAppointed <- bindParam[String]("agentAppointed")
        } yield {
          (agentCode, address, agentName, pageNumber, pageSize, sortField, sortOrder, agentAppointed) match {
            case (Right(ac), Right(addr), Right(an), Right(pn), Right(ps), Right(sf), Right(so), Right(aa)) =>
              Right(
                AgentPropertiesParameters(
                  agentCode = ac,
                  address = addr,
                  agentNameFilter = an,
                  pageNumber = pn,
                  pageSize = ps,
                  sortField = sf,
                  sortOrder = so,
                  agentAppointed = aa
                ))
            case _ => Left("Unable to bind to AgentPropertiesParameters")
          }
        }
      }

      override def unbind(key: String, value: AgentPropertiesParameters): String =
        s"""
           |agentCode=${value.agentCode}&
           |address=${value.address.getOrElse("")}&
           |agentName=${value.agentNameFilter.getOrElse("")}&
           |pageNumber=${value.pageNumber}&
           |pageSize=${value.pageSize}&
           |sortField=${value.sortField}&
           |sortOrder=${value.sortOrder}&
           |agentAppointed=${value.agentAppointed}
           |""".stripMargin.replaceAll("\n", "")
    }
}
