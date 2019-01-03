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

import java.io.File

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.libs.Files.TemporaryFile

trait FileUploadTestHelpers extends MockitoSugar {

  protected def mockTemporaryFile(wrapped: File) = {
    val tmpFile = mock[TemporaryFile]
    when(tmpFile.file).thenReturn(wrapped)
    when(tmpFile.clean()).thenReturn(true)
    tmpFile
  }

  val validFilePath = "aFakeFile.jpg"
  val validMimeType = "image/jpeg"

  val validFile = {
    val `10MB` = 10485760L
    val fakeFile = mock[File]
    when(fakeFile.length()).thenReturn(`10MB`)
    mockTemporaryFile(fakeFile)
  }

  val largeFilePath = "aLargeFakeFile.jpg"

  val largeFile = {
    val `15MB` = 15728640L
    val fakeFile = mock[File]
    when(fakeFile.length()).thenReturn(`15MB`)
    mockTemporaryFile(fakeFile)
  }

  val unsupportedFilePath = "aFakeFile.png"
  val unsupportedMimeType = "image/png"

  val unsupportedFile = {
    val `10MB` = 10485760L
    val fakeFile = mock[File]
    when(fakeFile.length()).thenReturn(`10MB`)
    mockTemporaryFile(fakeFile)
  }

}
