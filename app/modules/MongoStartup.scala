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

import java.time.LocalDateTime
import javax.inject.Singleton

import com.google.inject.multibindings.Multibinder
import com.google.inject.{AbstractModule, Inject, TypeLiteral}
import org.joda.time.Duration
import play.api.{Configuration, Environment, Logger}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import repositories.{MongoTaskRegister, MongoTaskRepo}
import uk.gov.hmrc.lock.{ExclusiveTimePeriodLock, LockMongoRepository, LockRepository}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

class MongoStartup(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    val mb = Multibinder.newSetBinder(binder(), new TypeLiteral[MongoTask]() {})
    mb.addBinding().to(classOf[ConvertTimestamps])
    mb.addBinding().to(classOf[FixSessionIndex])

    bind(classOf[MongoStartupRunner]).to(classOf[MongoStartupRunnerImpl]).asEagerSingleton()
  }
}

trait MongoStartupRunner extends ExclusiveTimePeriodLock {
  val db: DB
  val logger = Logger("MongoStartup")

  override val lockId: String = "MongoStartupLock"
  override val holdLockFor: Duration = Duration.standardMinutes(10)
  override def repo: LockRepository = LockMongoRepository(() => db)
}

@Singleton
class MongoStartupRunnerImpl @Inject() (reactiveMongoComponent: ReactiveMongoComponent,
                                        mongoTaskRepo: MongoTaskRepo,
                                        tasks: java.util.Set[MongoTask],
                                        val db: DB)
                                       (implicit ec: ExecutionContext) extends MongoStartupRunner {

  tryToAcquireOrRenewLock {
    tasks.asScala.foldLeft(logger.info("MongoStartup: running")) { (prev, task) =>
      prev.flatMap { _ =>
        (1 to task.upToVersion).foldLeft(logger.info(s"Processing ${task.name}...")) { (prev, version) =>
          prev.flatMap { _ =>
            mongoTaskRepo.find("taskName" -> task.name, "version" -> version).flatMap {
              case Nil => logger.info(s"Task ${task.name} version $version not yet executed - running")
                .map(_ => mongoTaskRepo.insert(MongoTaskRegister(task.name, version, LocalDateTime.now)))
                .flatMap(_ => task.run(version))
              case head :: _ => alreadyRun(task, version)(head)
            }
          }
        }
      }
    }
  }

  def alreadyRun(task: MongoTask, version: Int): MongoTaskRegister => Future[Unit] = { head =>
    logger.info(s"Mongo task ${task.name} version $version already ran at ${head.executionDateTime}")
  }

  implicit def toFuture[A](a: A): Future[A] = Future.successful(a)
}

trait MongoTask {
  val upToVersion: Int
  val name = this.getClass.getSimpleName

  def run(version: Int): Future[Unit]
}
