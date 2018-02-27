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

import models.{AgentPermission, SortOrder, StartAndContinue}
import play.api.mvc.QueryStringBindable
import utils.Formatters.{buildQueryParams, buildUppercaseQueryParams}

case class AgentPropertiesPagination(agentCode: Long,
                                     checkPermission: AgentPermission = StartAndContinue,
                                     challengePermission: AgentPermission = StartAndContinue,
                                     address: Option[String] = None,
                                     agentNameFilter: Option[String] = None,
                                     pageNumber: Int = 1,
                                     pageSize: Int = 15,
                                     sortField: AgentPropertiesSortField = AgentPropertiesSortField.Address,
                                     sortOrder: SortOrder = SortOrder.Ascending) {

  def startPoint: Int = (pageNumber - 1) * pageSize + 1

  def reverseSorting: AgentPropertiesPagination = copy(sortOrder = sortOrder.reverse)

  def previousPage: AgentPropertiesPagination = copy(pageNumber = pageNumber - 1)

  def nextPage: AgentPropertiesPagination = copy(pageNumber = pageNumber + 1)

  def clear: AgentPropertiesPagination = copy(address = None, agentNameFilter = None)

  lazy val queryString = s"agentCode=$agentCode&startPoint=$startPoint&pageSize=$pageSize&requestTotalRowCount=true" +
    s"&checkPermission=${checkPermission.name}&challengePermission=${challengePermission.name}" +
    buildUppercaseQueryParams("sortfield", Some(sortField.name)) +
    buildUppercaseQueryParams("sortorder", Some(sortOrder.name)) +
    buildQueryParams("address", address) +
    buildQueryParams("agent", agentNameFilter)
}

object AgentPropertiesPagination {
  implicit val queryStringBindable: QueryStringBindable[AgentPropertiesPagination] = new QueryStringBindable[AgentPropertiesPagination] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AgentPropertiesPagination]] = {
      def bindParam[T](key: String)(implicit qsb: QueryStringBindable[T]): Option[Either[String, T]] = qsb.bind(key, params)

      for {
        agentCode <- bindParam[Long]("agentCode")
        checkPermission <- bindParam[String]("checkPermission")
        challengePermission <- bindParam[String]("challengePermission")
        address <- bindParam[Option[String]]("address")
        agentName <- bindParam[Option[String]]("agentName")
        pageNumber <- bindParam[Int]("pageNumber")
        pageSize <- bindParam[Int]("pageSize")
        sortField <- bindParam[AgentPropertiesSortField]("sortField")
        sortOrder <- bindParam[SortOrder]("sortOrder")
      } yield {
        (agentCode, checkPermission, challengePermission, address, agentName, pageNumber, pageSize, sortField, sortOrder) match {
          case (Right(ac), Right(cp1), Right(cp2), Right(addr), Right(an), Right(pn), Right(ps), Right(sf), Right(so)) =>
            Right(AgentPropertiesPagination(
              agentCode = ac,
              checkPermission = AgentPermission.fromName(cp1).getOrElse(StartAndContinue),
              challengePermission = AgentPermission.fromName(cp2).getOrElse(StartAndContinue),
              address = addr,
              agentNameFilter = an,
              pageNumber = pn,
              pageSize = ps,
              sortField = sf,
              sortOrder = so))
          case _ => Left("Unable to bind to AgentPropertiesPagination")
        }
      }
    }

    override def unbind(key: String, value: AgentPropertiesPagination): String =
      s"""
         |agentCode=${value.agentCode}&
         |checkPermission=${value.checkPermission}&
         |challengePermission=${value.challengePermission}&
         |address=${value.address.getOrElse("")}&
         |agentName=${value.agentNameFilter.getOrElse("")}&
         |pageNumber=${value.pageNumber}&
         |pageSize=${value.pageSize}&
         |sortField=${value.sortField}&
         |sortOrder=${value.sortOrder}
         |""".stripMargin.replaceAll("\n", "")
  }
}
