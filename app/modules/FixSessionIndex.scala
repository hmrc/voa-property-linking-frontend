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

package modules
import javax.inject.Inject

import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

class FixSessionIndex @Inject()(sessionRepo: SessionRepository)(implicit ec: ExecutionContext) extends MongoTask {
  override val upToVersion = 2

  override def run(version: Int): Future[Unit] = {
    for {
      _ <- sessionRepo.collection.indexesManager.drop("sessionTTL") recover { case _ => () }
      _ <- sessionRepo.collection.indexesManager.drop("workingSessionTTL") recover { case _ => () }
    } yield ()
  }
}
