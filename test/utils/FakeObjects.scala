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
import models.{FileInfo, _}
import models.attachment._
import models.registration.UserInfo
import models.upscan._
import uk.gov.hmrc.auth.core.{Admin, AffinityGroup, User}


trait FakeObjects {

  val preparedUpload = PreparedUpload(Reference("1862956069192540"), UploadFormTemplate("http://localhost/upscan", Map()))
  val FILE_REFERENCE: String = "1862956069192540"
  val fileMetadata = FileMetadata(FILE_REFERENCE, "application/pdf")
  val uploadedFileDetails = UploadedFileDetails(fileMetadata, preparedUpload)
  val fileUpscanMetaData = Map(FILE_REFERENCE -> uploadedFileDetails)
  val attachment = Attachment(UUID.randomUUID(), Instant.now(), "fileName", "image/jpeg", "DESTINATION", Map(), Initiated, List(), None, None, Principal("externalId", "groupId"))
  val noEvidencelinkBasis = NoEvidenceFlag
  val rateBillLinkBasis = RatesBillFlag
  val fileInfo = FileInfo("test.pdf", RatesBillType)
  val uploadEvidenceData = UploadEvidenceData(rateBillLinkBasis, Some(fileInfo), Some(Map(FILE_REFERENCE -> uploadedFileDetails)))
  val detailedIndividualAccount = DetailedIndividualAccount("externalId", "", 1l, 2l, IndividualDetails("", "", "", "", None, 12))
  val groupAccount = GroupAccount(1L, "externalId", "externalId", 1, "test@gmail.com", "01293666666", true, 300L)
  val testAccounts = Accounts(groupAccount, detailedIndividualAccount)
  val testIndividualInfo = UserInfo(firstName = Some("Bob"),
    lastName = Some("Smith"),
    email = "bob@smith.com",
    postcode = Some("AB12 3CD"),
    groupIdentifier = "GroupIdenfifier",
    affinityGroup = AffinityGroup.Individual,
    gatewayId = "",
    credentialRole = User)

  val testOrganisationInfo = UserInfo(firstName = Some("Bob"),
    lastName = Some("Smith"),
    email = "bob@smith.com",
    postcode = Some("AB12 3CD"),
    groupIdentifier = "GroupIdenfifier",
    affinityGroup = AffinityGroup.Organisation,
    gatewayId = "",
    credentialRole = Admin)

  val testAgentInfo = UserInfo(firstName = Some("Bob"),
    lastName = Some("Smith"),
    email = "bob@smith.com",
    postcode = Some("AB12 3CD"),
    groupIdentifier = "GroupIdenfifier",
    affinityGroup = AffinityGroup.Agent,
    gatewayId = "",
    credentialRole = Admin)
}