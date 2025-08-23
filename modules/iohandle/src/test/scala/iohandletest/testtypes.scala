/*
 * Copyright 2025 IOHandle
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

package iohandletest

import cats.effect.IO
import iohandle.IORaise
import iohandletest.testtypes.ExplodeError.{Bam, Boom}

object testtypes {
  sealed trait MyError
  object MyError {
    final case class NotFound() extends MyError
    final case class Broken() extends MyError
  }

  final case class ErrWithInfo(i: Int)

  sealed trait ExplodeError
  object ExplodeError {
    case class Boom() extends ExplodeError
    case class Bam() extends ExplodeError
  }

  def successWithoutExplosion(result: Int)(implicit raise: IORaise[ExplodeError]): IO[Int] = {
    val _ = raise
    IO(result)
  }

  def raiseBoom()(implicit raise: IORaise[Boom]): IO[Unit] = raise.raise(Boom())
  def raiseBam()(implicit raise: IORaise[Bam]): IO[Unit] = raise.raise(Bam())

  class FooException extends Exception("FooException happened")
  class BarException extends Exception("BarException happened")

  def failWithFoo: IO[Unit] = {
    IO.raiseError(new FooException)
  }
}
