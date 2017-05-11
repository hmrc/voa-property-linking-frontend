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

package repositories

import javax.inject.Inject

import com.google.inject.{ImplementedBy, Singleton}
import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID, BSONString}
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class PropertyLinkingSessionRepository @Inject()(db: DB) extends SessionRepository("propertyLinking", db)

class SessionRepository @Inject()(formId: String, db: DB)
  extends ReactiveRepository[SessionData, String]("sessions", () => db, SessionData.format, implicitly[Format[String]])
    with SessionRepo with AtomicUpdate[SessionData] {

  override def start[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit] = {
    saveOrUpdate[A](data)
  }

  override def saveOrUpdate[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit] = {
   for {
     sessionId <- getSessionId
     _ <- collection.update(
       BSONDocument("_id" -> BSONString(sessionId)),
       BSONDocument(
         "$set" -> BSONDocument(s"data.${formId}" -> Json.toJson(data)),
         "$setOnInsert" -> BSONDocument("createdAt" -> Json.toJson(DateTime.now()))
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
      val aaa = maybeOption.map(_.data \ formId)
      val tmp: Option[Map[String, A]] = maybeOption.map(_.data.as[Map[String , A]])
      tmp.flatMap( _.get(formId))
    }
  }

  override def remove()(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      sessionId <- getSessionId
      _ <- removeById(sessionId)
    } yield {
      ()
    }
  }

  private val noSession = Future.failed[String](NoSessionException)

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq(("createdAt",  IndexType.Ascending)),
      name = Some("sessionTTL"),
      options = BSONDocument("createdAt" -> (2 hours).toSeconds)
    )
  )

  private def getSessionId(implicit hc: HeaderCarrier): Future[String] =
    hc.sessionId.fold(noSession)(c => Future.successful(c.value))

  override def isInsertion(suppliedId: BSONObjectID, returned: SessionData): Boolean =
    suppliedId.equals(returned._id)

}

case class SessionData(_id: String, data: JsValue, createdAt: DateTime = DateTime.now())
object SessionData {
  val format = Json.format[SessionData]
}
case object NoSessionException extends Exception("Could not find sessionId in HeaderCarrier")

trait SessionRepo {

  def start[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit]

  def saveOrUpdate[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit]

  def get[A](implicit rds: Reads[A], hc: HeaderCarrier): Future[Option[A]]

  def remove()(implicit hc: HeaderCarrier): Future[Unit]
}


