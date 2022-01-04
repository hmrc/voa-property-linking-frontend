/*
 * Copyright 2022 HM Revenue & Customs
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

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{currentMirror => runtimeMirror}

object QueryParamUtils {

  def toQueryString[T: TypeTag: ClassTag](t: T): String = {
    val classType = typeOf[T]
    val constructorMethod = classType.decl(termNames.CONSTRUCTOR).asMethod

    constructorMethod.paramLists.flatten
      .map(sym =>
        sym.name.toString -> (runtimeMirror.reflect(t).reflectMethod(classType.member(sym.name).asMethod)() match {
          case v: Option[Any] => v
          case any            => Some(any)
        }))
      .collect { case (key, Some(value)) => s"$key=${uriEncode(value.toString)}" }
      .mkString("&")
  }

  def uriEncode(value: String): String = URLEncoder.encode(value, UTF_8.name())
}
