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

package models.messages

import models.SortOrder
import play.api.mvc.QueryStringBindable

case class MessagePagination(pageNumber: Int, pageSize: Int, sortField: MessageSortField, sortOrder: SortOrder) {
  lazy val startPoint: Int = (pageNumber - 1) * pageSize + 1

  def reverseSorting: MessagePagination = copy(sortOrder = sortOrder.reverse)

  def previousPage: MessagePagination = copy(pageNumber = pageNumber - 1)

  def nextPage: MessagePagination = copy(pageNumber = pageNumber + 1)

  lazy val queryString: String =
    s"""
       |startPoint=$startPoint&
       |pageSize=$pageSize&
       |sortField=$sortField&
       |sortOrder=$sortOrder
       |""".stripMargin.replaceAll("\n", "")
}

object MessagePagination {
  implicit val queryStringBindable: QueryStringBindable[MessagePagination] = new QueryStringBindable[MessagePagination] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MessagePagination]] = {
      def bindParam[T](key: String)(implicit qsb: QueryStringBindable[T]): Option[Either[String, T]] = qsb.bind(key, params)

      for {
        pageNumber <- bindParam[Int]("pageNumber")
        pageSize <- bindParam[Int]("pageSize")
        sortField <- bindParam[MessageSortField]("sortField")
        sortOrder <- bindParam[SortOrder]("sortOrder")
      } yield {
        (pageNumber, pageSize, sortField, sortOrder) match {
          case (Right(pn), Right(ps), Right(sf), Right(so)) => Right(MessagePagination(pn, ps, sf, so))
          case _ => Left("Unable to bind to MessagePagination")
        }
      }
    }

    override def unbind(key: String, value: MessagePagination): String =
      s"""
         |pageNumber=${value.pageNumber}&
         |pageSize=${value.pageSize}&
         |sortField=${value.sortField}&
         |sortOrder=${value.sortOrder}
         |""".stripMargin.replaceAll("\n", "")
  }

  lazy val default: MessagePagination = MessagePagination(1, 15, MessageSortField.EffectiveDate, SortOrder.Descending)
}
