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

package controllers

import models.searchApi.AgentPropertiesFilter.Both
import play.api.mvc.{AnyContent, QueryStringBindable, Request, Result}
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import utils.Formatters._
import utils.QueryParamUtils._

import scala.concurrent.Future

trait ValidPagination extends PropertyLinkingController {

  val errorHandler: FrontendErrorHandler

  protected def withValidPagination(page: Int, pageSize: Int, getTotal: Boolean = true)(
        default: Pagination => Future[Result])(implicit request: Request[AnyContent]): Future[Result] =
    if (page <= 0 || pageSize < 10 || pageSize > 100) {
      Future.successful(BadRequest(errorHandler.badRequestTemplate))
    } else {
      default(Pagination(pageNumber = page, pageSize = pageSize, resultCount = getTotal))
    }
}

case class PaginationParams(startPoint: Int, pageSize: Int, requestTotalRowCount: Boolean)

object DefaultPaginationParams extends PaginationParams(startPoint = 1, pageSize = 15, requestTotalRowCount = true)

object PaginationParams {
  implicit def queryStringBindable(
        implicit intBinder: QueryStringBindable[Int],
        booleanBinder: QueryStringBindable[Boolean]): QueryStringBindable[PaginationParams] =
    new QueryStringBindable[PaginationParams] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, PaginationParams]] =
        for {
          startPoint           <- intBinder.bind("startPoint", params)
          pageSize             <- intBinder.bind("pageSize", params)
          requestTotalRowCount <- booleanBinder.bind("requestTotalRowCount", params)
        } yield {
          (startPoint, pageSize, requestTotalRowCount) match {
            case (Right(sp), Right(ps), Right(rtrc)) => Right(PaginationParams(sp, ps, rtrc))
            case _                                   => Left("Unable to bind PaginationParams")
          }
        }

      override def unbind(key: String, value: PaginationParams): String = toQueryString(value)
    }
}

case class Pagination(pageNumber: Int, pageSize: Int, totalResults: Long = 0, resultCount: Boolean = true) {
  def startPoint: Int = pageSize * (pageNumber - 1) + 1
  override val toString = s"startPoint=$startPoint&pageSize=$pageSize&requestTotalRowCount=$resultCount"
}

case class PaginationSearchSort(
      pageNumber: Int,
      pageSize: Int,
      requestTotalRowCount: Boolean = false,
      sortfield: Option[String] = None,
      sortorder: Option[String] = None,
      status: Option[String] = None,
      address: Option[String] = None,
      baref: Option[String] = None,
      agent: Option[String] = None,
      client: Option[String] = None,
      totalResults: Long = 0,
      agentAppointed: String = Both.name) {

  def reverseSortOrder: Option[String] =
    sortorder match {
      case Some(paramValue) if paramValue.toUpperCase == "ASC" => Some("DESC");
      case _                                                   => Some("ASC")
    }

  def valueOfSortorder: String = sortorder.getOrElse("ASC").toUpperCase
  def valueOfSortfield: String = sortfield.getOrElse("").trim
  def valueOfStatus: String = status.getOrElse("").trim
  def valueOfAddress: String = address.getOrElse("").trim
  def valueOfBaref: String = baref.getOrElse("").trim
  def valueOfAgent: String = agent.getOrElse("").trim
  def valueOfClient: String = client.getOrElse("").trim

  def valuesOfSearchParameters: String =
    valueOfStatus + valueOfAddress + valueOfBaref + valueOfAgent + valueOfClient

  def startPoint: Int = pageSize * (pageNumber - 1) + 1
  override val toString = s"startPoint=$startPoint&pageSize=$pageSize&requestTotalRowCount=$requestTotalRowCount" +
    buildUppercaseQueryParams("sortfield", sortfield) +
    buildUppercaseQueryParams("sortorder", sortorder) +
    buildUppercaseQueryParams("status", status) +
    buildQueryParams("address", address) +
    buildQueryParams("baref", baref) +
    buildQueryParams("agent", agent) +
    buildQueryParams("client", client) +
    buildQueryParams("agentAppointed", Some(agentAppointed))

}
