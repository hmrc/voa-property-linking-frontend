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

package controllers

import config.Global
import play.api.mvc.{AnyContent, Request, Result}
import play.api.mvc.Results.BadRequest
import play.twirl.api.Html
import utils.Formatters._

import scala.collection.immutable.IndexedSeq
import scala.collection.immutable.NumericRange.Inclusive
import scala.concurrent.Future

trait ValidPagination extends PropertyLinkingController {
  protected def withValidPagination(page: Int, pageSize: Int, getTotal: Boolean = true)
                                   (default: Pagination => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    if (page <= 0 || pageSize < 10 || pageSize > 100) {
      BadRequest(Global.badRequestTemplate)
    } else {
      default(Pagination(pageNumber = page, pageSize = pageSize, resultCount = getTotal))
    }
  }

  protected def withValidPaginationSearchSort(page: Int,
                                              pageSize: Int,
                                              requestTotalRowCount: Boolean = true,
                                              sortfield: Option[String] = None,
                                              sortorder: Option[String] = None,
                                              status: Option[String] = None,
                                              address: Option[String] = None,
                                              baref: Option[String] = None,
                                              agent: Option[String] = None,
                                              client: Option[String] = None)
                                   (default: PaginationSearchSort => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    if (page <= 0 || pageSize < 10 || pageSize > 100) {
      BadRequest(Global.badRequestTemplate)
    } else {
      default(PaginationSearchSort(pageNumber = page,
        pageSize = pageSize,
        requestTotalRowCount = requestTotalRowCount,
        sortfield = sortfield,
        sortorder = sortorder,
        status = status,
        address = address,
        baref = baref,
        agent = agent,
        client = client))
    }
  }
}

case class Pagination(pageNumber: Int, pageSize: Int, totalResults: Long = 0, resultCount: Boolean = true) {
  def startPoint: Int = pageSize * (pageNumber - 1) + 1
  override val toString = s"startPoint=$startPoint&pageSize=$pageSize&requestTotalRowCount=$resultCount"

  def maxPage: Int = (totalResults / pageSize).toInt

  def pageination = (1 to maxPage).map(html)

  private def html(int: Int): Html = {
    if (int == pageNumber)
      Html(s"<li value='1'>$int</li>")
    else
      Html(s"""<li class="Bob" value=$int>$int</li>""")
  }

  private def previous: List[Html] = {
    if (pageNumber == 1)
      List(Html("<li>Previous</li>"))
    else
      List(Html(s"""<li class="bob" value="${pageNumber - 1}">Previous</li>"""))
  }

  private def next: List[Html] = {
    if (pageNumber == maxPage)
      List(Html("<li>Next</li>"))
    else
      List(Html(s"""<li class="bob" value="${pageNumber + 1}">Next</li>"""))
  }
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
    totalResults: Long = 0
) {

  def reverseSortOrder: Option[String] = {

    sortorder match
      { case Some(paramValue) if paramValue.toUpperCase == "ASC" => Some("DESC") ;
        case _ => Some("ASC")
      }
  }

  def valueOfSortorder : String = sortorder.getOrElse("ASC").toUpperCase
  def valueOfSortfield: String = sortfield.getOrElse("").trim
  def valueOfStatus: String = status.getOrElse("").trim
  def valueOfAddress: String = address.getOrElse("").trim
  def valueOfBaref: String = baref.getOrElse("").trim
  def valueOfAgent: String = agent.getOrElse("").trim
  def valueOfClient: String = client.getOrElse("").trim

  def valuesOfSearchParameters : String =
    valueOfStatus + valueOfAddress + valueOfBaref + valueOfAgent + valueOfClient


  def startPoint: Int = pageSize * (pageNumber - 1) + 1
  override val toString = s"startPoint=$startPoint&pageSize=$pageSize&requestTotalRowCount=$requestTotalRowCount" +
    buildUppercaseQueryParams("sortfield", sortfield) +
    buildUppercaseQueryParams("sortorder", sortorder) +
    buildUppercaseQueryParams("status", status) +
    buildQueryParams("address", address) +
    buildQueryParams("baref", baref) +
    buildQueryParams("agent", agent) +
    buildQueryParams("client", client)

  def maxPage: Int = (totalResults / pageSize).toInt

  def getHtml: List[Html] = {
    val list = (1 to maxPage).map(html).toList
    if (list.isEmpty)
      List(Html("""<a class="next pX">Next</a>"""), Html("""<a class="pX value" id="current" data-dt-idx="1">1</a><a class="pX value" data-dt-idx="2">2</a><a class="pX value" data-dt-idx="3">3</a><a class="pX value" data-dt-idx="4">4</a><a class="pX value" data-dt-idx="5">5</a><a>...</><a class="pX value" data-dt-idx="8">8</a>"""), Html("""<a class="previous pX" >Previous</a>"""))
    else
      previous ::: list ::: next
  }

  private def previous: List[Html] = {
    if (pageNumber == 1)
      List(Html("""<a class="pagination disabled">Previous</a>"""))
    else
      List(Html(s"""<a class="pagination" data-dt-idx="${pageNumber - 1}" >Previous</a>"""))
  }

  private def next: List[Html] = {
    if (pageNumber == maxPage)
      List(Html("""<a class="pagination" >Next</a>"""))
    else
      List(Html(s"""<a class="pagination" data-dt-idx="${pageNumber + 1}" >Next<a>"""))
  }

  private def html(int: Int): Html = {
    if (int == pageNumber)
      Html(s"""<a class="pagination" id="current" data-dt-idx="$int" >$int</a>""")
    else
      Html(s"""<a class="pagination" data-dt-idx="$int" >$int</a>""")
  }
}

