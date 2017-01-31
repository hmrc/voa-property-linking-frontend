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

import connectors.CapacityDeclaration
import models._
import org.joda.time.{DateTime, LocalDate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, _}


package object resources {

  implicit val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary(Gen.choose(0L, Long.MaxValue).map(new LocalDate(_)))
  implicit val arbitraryDateTime: Arbitrary[DateTime] = Arbitrary(Gen.choose(0L, Long.MaxValue).map(new DateTime(_)))

  val propertyAddressGen: Gen[PropertyAddress] = for {
    lines <- Gen.nonEmptyListOf(Gen.alphaNumStr)
    postcode <- Gen.alphaNumStr
  } yield PropertyAddress(lines, postcode)

  implicit val arbitraryPropertyAddress = Arbitrary(for {p <- propertyAddressGen} yield p)

  val propertyGen: Gen[Property] = for {
    uarn <- arbitrary[Long]
    billingAuthorityReference <- arbitrary[String]
    address <- propertyAddressGen
    specialCategoryCode <- Gen.alphaNumStr
    description <- arbitrary[String]
    bulkClassIndicator <- arbitrary[Char].map(_.toString)
  } yield Property(uarn, billingAuthorityReference, address, specialCategoryCode, description, bulkClassIndicator)
  implicit val arbitraryProperty = Arbitrary(for {p <- propertyGen} yield p)

  val capacityTypeGen: Gen[CapacityType] = Gen.oneOf(Owner, OwnerOccupier, Occupier)
  implicit val arbitratyCapacityType = Arbitrary(capacityTypeGen)

  val capacityDeclarationGen: Gen[CapacityDeclaration] = for {
    capacity <- arbitrary[CapacityType]
    interestedBefore2017 <- arbitrary[Boolean]
    fromDate <-  arbitrary[Option[LocalDate]]
    stillInterested <- arbitrary[Boolean]
    toDate <- arbitrary[Option[LocalDate]]
  } yield  CapacityDeclaration(capacity, interestedBefore2017, fromDate, stillInterested, toDate)
  implicit val arbitraryCapacityDeclaration = Arbitrary(capacityDeclarationGen)

  val addressGen: Gen[Address] = for {
    id <- arbitrary[Option[Int]]
    line1 <- Gen.alphaNumStr
    line2 <- Gen.alphaNumStr
    line3 <- Gen.alphaNumStr
    line4 <- Gen.alphaNumStr
    postcode <- Gen.alphaNumStr
  } yield Address(id, line1, line2, line3, line4, postcode)
  implicit val arbitraryAddress = Arbitrary(addressGen)


  val individualDetailsGen: Gen[IndividualDetails] = for {
    fistName <- Gen.alphaNumStr
    lastName <- Gen.alphaNumStr
    email <- Gen.alphaNumStr
    phone1 <- Gen.listOfN(8, Gen.numChar)
    address <- arbitrary[Address]
  } yield IndividualDetails(fistName, lastName, email, phone1.mkString, None, address)
  implicit val arbitraryIndividualDetails = Arbitrary(individualDetailsGen)

  val individualGen: Gen[DetailedIndividualAccount] = for {
    externalId <- Gen.alphaNumStr
    trustId <- Gen.alphaNumStr
    organisationId <- arbitrary[Int]
    individualId <- arbitrary[Int]
    individualDetails <- arbitrary[IndividualDetails]
  } yield DetailedIndividualAccount(externalId, trustId, organisationId, individualId, individualDetails)
  implicit val arbitraryIndividual = Arbitrary(individualGen)

  val groupAccountGen: Gen[GroupAccount] = for {
    id <- arbitrary[Int]
    groupId <- Gen.alphaNumStr
    companyName <- Gen.alphaNumStr
    address <- arbitrary[Address]
    email <- Gen.alphaNumStr
    phone <-  Gen.listOfN(8, Gen.numChar)
    isSmallBusiness <- arbitrary[Boolean]
    agentCode <- arbitrary[Option[String]]
  } yield GroupAccount(id, groupId, companyName, address, email, phone.mkString, isSmallBusiness, agentCode)
  implicit val arbitraryGroupAccount = Arbitrary(groupAccountGen)

  val capacityGen: Gen[Capacity] = for {
    capacity <- arbitrary[CapacityType]
    fromDate <- arbitrary[LocalDate]
    toDate <- arbitrary[Option[LocalDate]]
  } yield Capacity(capacity, fromDate, toDate)
  implicit val arbitraryCapacity = Arbitrary(capacityGen)

  val assessmentGen: Gen[Assessment] = for {
    linkId <- arbitrary[Int]
    asstRef <- arbitrary[Long]
    listYear <- arbitrary[String]
    uarn <- arbitrary[Long]
    effectiveDate <- arbitrary[LocalDate]
    rateableValue <- arbitrary[Long]
    address <- arbitrary[ PropertyAddress]
    billingAuthorityReference <- arbitrary[String]
    capacity <- arbitrary[Capacity]
  } yield models.Assessment(linkId, asstRef, listYear, uarn, effectiveDate, rateableValue, address, billingAuthorityReference, capacity) 
  implicit val arbitraryAssessment = Arbitrary(assessmentGen)
  
  val detailedPropertyLinkGen: Gen[DetailedPropertyLink] = for {
    linkId <- arbitrary[Int]
    uarn <- arbitrary[Long]
    organisationId <- arbitrary[Int]
    description <- Gen.alphaNumStr
    agentNames <- Gen.listOf(Gen.alphaNumStr)
    canAppointAgent <- arbitrary[Boolean]
    address <- arbitrary[PropertyAddress]
    capacity <- arbitrary[Capacity]
    linkedDate <- arbitrary[DateTime]
    pending <- arbitrary[Boolean]
    assessment <- Gen.listOf(arbitrary[Assessment])
  } yield DetailedPropertyLink(linkId, uarn, organisationId, description, agentNames, canAppointAgent, address, capacity,
    linkedDate, pending, assessment)
  implicit val arbitraryDetailedPropertyLink = Arbitrary(detailedPropertyLinkGen)
}