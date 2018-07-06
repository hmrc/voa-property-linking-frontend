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

package repositories

import javax.inject.Inject

import com.google.inject.Singleton
import models.messages.Message
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONString}
import uk.gov.hmrc.http.cache.client.NoSessionException
import uk.gov.hmrc.mongo.ReactiveRepository
import reactivemongo.json.ImplicitBSONHandlers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class PersonalDetailsSessionRepository @Inject()(db: DB) extends SessionRepository("personDetails", db)

@Singleton
class PropertyLinkingSessionRepository @Inject()(db: DB) extends SessionRepository("propertyLinking", db)

class SessionRepository @Inject()(formId: String, db: DB)
  extends ReactiveRepository[SessionData, String]("sessions", () => db, SessionData.format, implicitly[Format[String]])
    with SessionRepo {

  override def start[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit] = {
    saveOrUpdate[A](data)
  }

  override def saveOrUpdate[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit] = {
    for {
      sessionId <- getSessionId
      _ <- collection.update(
        BSONDocument("_id" -> BSONString(sessionId)),
        BSONDocument(
          "$set" -> BSONDocument(s"data.$formId" -> Json.toJson(data)),
          "$setOnInsert" -> BSONDocument("createdAt" -> BSONDateTime(System.currentTimeMillis))
        ),
        upsert = true
      )
    } yield {
      ()
    }
  }

  override def get[A](implicit rds: Reads[A], hc: HeaderCarrier): Future[Option[A]] = {
    for {
      sessionId <- getSessionId
      maybeOption <- findById(sessionId)
    } yield {
      maybeOption
        .map(_.data \ formId)
        .flatMap(x => x match {
          case JsDefined(value) => Some(value.as[A])
          case JsUndefined() => None
        })
    }
  }

  override def remove()(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      sessionId <- getSessionId
      _ <- collection.update(
        BSONDocument("_id" -> BSONString(sessionId)),
        BSONDocument(
          "$unset" -> BSONDocument(s"data.$formId" -> 1))
      )
    } yield {
      ()
    }
  }

  private val noSession = Future.failed[String](NoSessionException)

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq(("createdAt", IndexType.Ascending)),
      name = Some("sessionTTL"),
      options = BSONDocument("expireAfterSeconds" -> (2 hours).toSeconds)
    )
  )

  private def getSessionId(implicit hc: HeaderCarrier): Future[String] =
    hc.sessionId.fold(noSession)(c => Future.successful(c.value))

}

case class SessionData(_id: String, data: JsValue, createdAt: BSONDateTime = BSONDateTime(System.currentTimeMillis))

object SessionData {

  import reactivemongo.json.BSONFormats.BSONDateTimeFormat

  val format = Json.format[SessionData]
}

trait SessionRepo {

  def start[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit]

  def saveOrUpdate[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit]

  def get[A](implicit rds: Reads[A], hc: HeaderCarrier): Future[Option[A]]

  def remove()(implicit hc: HeaderCarrier): Future[Unit]
}


