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

package connectors

import models._
import org.joda.time.DateTime

case class CapacityDeclaration(capacity: CapacityType, fromDate: DateTime, toDate: Option[DateTime] = None)

case class FileInfo(fileName: String, fileType: String)

case class PropertyLinkRequest(uarn: Long, userId: String, capacityDeclaration: CapacityDeclaration,
                               linkedDate: DateTime, linkBasis: LinkBasis,
                               specialCategoryCode: String, description: String, bulkClassIndicator: String,
                               fileInfo: Option[FileInfo])

case class PropertyLink(uarn: Long, userId: String, description: String, capacityDeclaration: CapacityDeclaration,
                        linkedDate: DateTime, pending: Boolean)

case class LinkedProperties(added: Seq[PropertyLink], pending: Seq[PropertyLink])

case class PropertyRepresentation(representationId: String, agentId: String, agentName: String, groupId: String,
                                  groupName: String, uarn: Long, address: Address, canCheck: AgentPermission,
                                  canChallenge: AgentPermission, pending: Boolean)


