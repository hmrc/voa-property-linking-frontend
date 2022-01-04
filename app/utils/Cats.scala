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

import cats.instances.{FutureInstances, ListInstances, OptionInstances, VectorInstances}
import cats.syntax._

trait Cats
    extends OptionInstances //commonly used wrapper types - Option, List, Vector, Future
    with ListInstances //for when we're traversing/validating lists
    with VectorInstances //for when we're traversing/validating vectors
    with FutureInstances //require an implicit execution context in scope
    with ValidatedSyntax // for Validated stuff in Binders, etc.
    with EitherSyntax //useful conversion between Either/Option/Try/Validated
    with ApplySyntax //map2..22 , mapN
    with ShowSyntax //for turning things to String without relying on Object#toString
    with FunctorSyntax //for .widen syntax for eitherT to uplift the type to a Lowest common parent
    with TraverseSyntax //.traverse and .sequence extensions

object Cats extends Cats
