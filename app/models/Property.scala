/*
 * Copyright 2016 HM Revenue & Customs
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

package models

case class Address(lines: Seq[String], postcode: String, canReceiveCorrespondence: Boolean) {
  override def toString = (lines ++ Seq(postcode)) mkString ", "
}

case class Property(billingAuthorityReference: String, address: Address, bulkClass: BulkClass, isBank: Boolean)

sealed trait BulkClass extends NamedEnum {
  val key = "bulkClass"
}

object Shop extends BulkClass {
  val name = "shop"
}
object Office extends BulkClass {
  val name = "office"
}
object Industrial extends BulkClass {
  val name = "industrial"
}

object BulkClass extends NamedEnumSupport[BulkClass] {
  override def all: List[BulkClass] = List(Shop, Office, Industrial)
}

object SelfCertificationEnabled {
  lazy val selfCertifiableBulkClasses = Seq(Shop, Office, Industrial)

  def apply(sv: Property): Boolean = selfCertifiableBulkClasses.contains(sv.bulkClass) && !sv.isBank
}

trait NamedEnum {
  def name:String
  def key:String
  def msgKey: String = s"$key.$name"
}

trait NamedEnumSupport[E <: NamedEnum]{

  def all:List[E]

  def fromName(name: String): Option[E] = {
    all.find { _.name.equalsIgnoreCase(name) }
  }
}
