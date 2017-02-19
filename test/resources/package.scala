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
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, _}
import uk.gov.hmrc.domain.Nino
import java.{time => javatime}
import org.joda.time.{LocalDate, DateTime}

package object resources {

  implicit val arbitraryJavaLocalDate: Arbitrary[javatime.LocalDate] = Arbitrary(Gen.choose(0L, Long.MaxValue).map(javatime.LocalDate.ofEpochDay(_)))
  implicit val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary(Gen.choose(0L, Long.MaxValue).map(new LocalDate(_)))
  implicit val arbitraryDateTime: Arbitrary[DateTime] = Arbitrary(Gen.choose(0L, Long.MaxValue).map(new DateTime(_)))
  implicit val shortString = Gen.listOfN(20, Gen.alphaChar).map(_.mkString)

  private def positiveLong = Gen.choose(0L, Long.MaxValue)

  val propertyAddressGen: Gen[PropertyAddress] = for {
    lines <- Gen.nonEmptyListOf(shortString)
    postcode <- shortString
  } yield PropertyAddress(lines, postcode)

  implicit val arbitraryPropertyAddress = Arbitrary(for {p <- propertyAddressGen} yield p)

  val propertyGen: Gen[Property] = for {
    uarn <- positiveLong
    billingAuthorityReference <- shortString
    address <- propertyAddressGen
    specialCategoryCode <- shortString
    description <- shortString
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
    line1 <- shortString
    line2 <- shortString
    line3 <- shortString
    line4 <- shortString
    postcode <- shortString
  } yield Address(id, line1, line2, line3, line4, postcode)
  implicit val arbitraryAddress = Arbitrary(addressGen)


  val individualDetailsGen: Gen[IndividualDetails] = for {
    fistName <- shortString
    lastName <- shortString
    email <- shortString
    phone1 <- Gen.listOfN(8, Gen.numChar)
    address <- arbitrary[Address]
  } yield IndividualDetails(fistName, lastName, email, phone1.mkString, None, address)
  implicit val arbitraryIndividualDetails = Arbitrary(individualDetailsGen)

  val individualGen: Gen[DetailedIndividualAccount] = for {
    externalId <- shortString
    trustId <- shortString
    organisationId <- arbitrary[Int]
    individualId <- arbitrary[Int]
    individualDetails <- arbitrary[IndividualDetails]
  } yield DetailedIndividualAccount(externalId, trustId, organisationId, individualId, individualDetails)
  implicit val arbitraryIndividual = Arbitrary(individualGen)

  val groupAccountGen: Gen[GroupAccount] = for {
    id <- arbitrary[Int]
    groupId <- shortString
    companyName <- shortString
    address <- arbitrary[Address]
    email <- shortString
    phone <-  Gen.listOfN(8, Gen.numChar)
    isSmallBusiness <- arbitrary[Boolean]
    isAgent <- arbitrary[Boolean]
    agentCode <- arbitrary[Long]
  } yield GroupAccount(id, groupId, companyName, address, email, phone.mkString, isSmallBusiness, isAgent, agentCode)
  implicit val arbitraryGroupAccount = Arbitrary(groupAccountGen)

  val capacityGen: Gen[Capacity] = for {
    capacity <- arbitrary[CapacityType]
    fromDate <- arbitrary[LocalDate]
    toDate <- arbitrary[Option[LocalDate]]
  } yield Capacity(capacity, fromDate, toDate)
  implicit val arbitraryCapacity = Arbitrary(capacityGen)

  val assessmentGen: Gen[Assessment] = for {
    linkId <- arbitrary[Int]
    asstRef <- positiveLong
    listYear <- shortString
    uarn <- positiveLong
    effectiveDate <- arbitrary[LocalDate]
    rateableValue <- positiveLong
    address <- arbitrary[ PropertyAddress]
    billingAuthorityReference <- shortString
    capacity <- arbitrary[Capacity]
  } yield models.Assessment(linkId, asstRef, listYear, uarn, effectiveDate, rateableValue, address, billingAuthorityReference, capacity) 
  implicit val arbitraryAssessment = Arbitrary(assessmentGen)

  val agentPermissionGen: Gen[AgentPermission] = Gen.oneOf(StartAndContinue, ContinueOnly, NotPermitted)
  implicit val agentPermissionType = Arbitrary(agentPermissionGen)

  val representationStatusGen: Gen[RepresentationStatus] = Gen.oneOf(RepresentationApproved, RepresentationPending)
  implicit val representationStatusType = Arbitrary(representationStatusGen)

  val party: Gen[Party] = for {
    organisationName <- arbitrary[String]
    agentCode <- arbitrary[Long]
  } yield models.Party(agentCode, organisationName)
  implicit val arbitraryParty = Arbitrary(party)

  val propertyLinkGen: Gen[PropertyLink] = for {
    linkId <- arbitrary[Int]
    uarn <- positiveLong
    organisationId <- arbitrary[Int]
    address <- arbitrary[PropertyAddress]
    capacity <- arbitrary[Capacity]
    linkedDate <- arbitrary[DateTime]
    pending <- arbitrary[Boolean]
    assessment <- Gen.nonEmptyListOf(arbitrary[Assessment])
    agents <- Gen.listOf(arbitrary[Party])
  } yield PropertyLink(linkId, uarn, organisationId, address, capacity,
    linkedDate, pending, assessment, agents)
  implicit val arbitraryPropertyLink = Arbitrary(propertyLinkGen)


  val ninoGen: Gen[Nino] = for {
    prefix <- (for {
      prefix1 <- Gen.oneOf(('A' to 'Z').filterNot(List('D', 'F', 'I', 'Q', 'U', 'V').contains))
      prefix2 <- Gen.oneOf(('A' to 'Z').filterNot(List('D', 'F', 'I', 'O', 'Q', 'U', 'V').contains))
    } yield (s"$prefix1$prefix2")) suchThat (!List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ").contains(_))
    number <- Gen.listOfN(6, Gen.numChar)
    suffix <- Gen.oneOf('A' to 'D')
  } yield Nino(s"$prefix${number.mkString}$suffix".grouped(2).mkString(" "))
  implicit val arbitraryNino = Arbitrary(ninoGen)

  val ivDetailsGen: Gen[IVDetails] = for {
    firstName <- shortString
    lastName <- shortString
    dob <- arbitrary[LocalDate]
    nino <- arbitrary[Nino]
  } yield IVDetails(firstName, lastName, dob, nino)
  implicit val arbitraryIVDetails = Arbitrary(ivDetailsGen)

}