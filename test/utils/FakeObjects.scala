/*
 * Copyright 2019 HM Revenue & Customs
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

package utils

import java.time.Instant
import java.util.UUID

import auth.Principal
import models._
import models.attachment._
import models.registration.{GroupAccountDetails, UserDetails}
import models.upscan._
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole, User}


trait FakeObjects {

  val ggExternalId = "gg-ext-id"
  val ggGroupId = "gg-group-id"
  val firstName = "Bob"
  val lastName = "Smith"
  val companyName = "ACME Ltd."
  val postCode = "BN12 6DL"
  val addressLine = "7 The Strand, Worthing, BN12 6DL"
  val address: Address = Address(Some(7L), "The Strand", "Worthing", "", "", postCode)
  val email = "some@email.com"
  val phone = "01293666666"
  val principal = Principal(ggExternalId, ggGroupId)
  val FILE_REFERENCE: String = "1862956069192540"
  val preparedUpload = PreparedUpload(Reference(FILE_REFERENCE), UploadFormTemplate("http://localhost/upscan", Map()))
  val fileMetadata = FileMetadata(FILE_REFERENCE, "application/pdf")
  val uploadedFileDetails = UploadedFileDetails(fileMetadata, preparedUpload)
  val fileUpscanMetaData: Map[String, UploadedFileDetails] = Map(FILE_REFERENCE -> uploadedFileDetails)
  val attachment = Attachment(UUID.randomUUID(), Instant.now(), "fileName", "image/jpeg", "DESTINATION", Map(), Initiated, List(), None, None, principal)
  val noEvidencelinkBasis: NoEvidenceFlag.type = NoEvidenceFlag
  val fileInfo = FileInfo("test.pdf", RatesBillType)
  val uploadEvidenceData = UploadEvidenceData(RatesBillFlag, Some(fileInfo), Some(Map(FILE_REFERENCE -> uploadedFileDetails)))
  val detailedIndividualAccount = DetailedIndividualAccount(ggExternalId, "", 1L, 2L, IndividualDetails("", "", "", "", None, 12))
  val individualUserDetails: UserDetails = userDetails(AffinityGroup.Individual)

  def groupAccount(agent: Boolean): GroupAccount = GroupAccount(1L, ggGroupId, ggExternalId, 1, email, phone, isAgent = agent, 300L)

  val groupAccountDetails = GroupAccountDetails(companyName, address, email, email, phone, isAgent = false)
  val testAccounts = Accounts(groupAccount(agent = true), detailedIndividualAccount)

  def userDetails(affinityGroup: AffinityGroup = AffinityGroup.Individual, credentialRole: CredentialRole = User): UserDetails = UserDetails(
    firstName = Some(firstName),
    lastName = Some(lastName),
    email = email,
    postcode = Some(postCode),
    groupIdentifier = ggGroupId,
    affinityGroup = affinityGroup,
    externalId = ggExternalId,
    credentialRole = credentialRole)

}