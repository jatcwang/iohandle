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
import iohandle.*
import iohandletest.testtypes.ExplodeError.Boom
import iohandletest.testtypes.*
import iohandletest.testtypes.MyError.NotFound
import munit.CatsEffectSuite

/** Test to test extension methods on cats.effect.IO, e.g. recoverUnexpected Note that this file is copied and made
  * Scala 3 compatible when building this project in Scala 3 (e.g. string replacement removing "implicit handle =>").
  * Search for sourceGenerators in build.sbt to see how it's done
  */
class IOExtensionSpec extends CatsEffectSuite {
  test("recoverUnexpected: success case (no raised or thrown error)") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      successWithoutExplosion(1)
        .recoverUnexpected { case _ => fail("shouldn't have gotten here") }
    }.toEither

    prog.assertEquals(Right(1))
  }

  test("recoverUnexpected: error from Raise shouldn't be seen by handler") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      raiseBoom()
        .flatMap(_ => successWithoutExplosion(1))
        .recoverUnexpected { case _ => 10 }
    }.toEither

    prog.assertEquals(Left(Boom()))
  }

  test("recoverUnexpected: failure (recovered)") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      failWithFoo
        .flatMap(_ => successWithoutExplosion(1))
        .recoverUnexpected { case _: FooException => 10 }
    }.toEither

    prog.assertEquals(Right(10))
  }

  test("recoverUnexpected: failure (recovered fall through)") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      failWithFoo
        .flatMap(_ => successWithoutExplosion(1))
        .recoverUnexpected { case _: BarException => 10 }
    }.toEither

    prog.intercept[FooException]
  }

  test("recoverUnexpectedWith: success case (no raised or thrown error)") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      successWithoutExplosion(1)
        .recoverUnexpectedWith { case _ => fail("shouldn't have gotten here") }
    }.toEither

    prog.assertEquals(Right(1))
  }

  test("recoverUnexpectedWith: error from Raise shouldn't be seen by handler") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      raiseBoom()
        .flatMap(_ => successWithoutExplosion(1))
        .recoverUnexpectedWith { case _ => IO(10) }
    }.toEither

    prog.assertEquals(Left(Boom()))
  }

  test("recoverUnexpectedWith: failure (recovered)") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      failWithFoo
        .flatMap(_ => successWithoutExplosion(1))
        .recoverUnexpectedWith { case _: FooException => IO(10) }
    }.toEither

    prog.assertEquals(Right(10))
  }

  test("recoverUnexpectedWith: failure (recovered fall through)") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      failWithFoo
        .flatMap(_ => successWithoutExplosion(1))
        .recoverUnexpectedWith { case _: BarException => IO(10) }
    }.toEither

    prog.intercept[FooException]
  }

  test("handleUnexpected: success case (no raised or thrown error)") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      successWithoutExplosion(1)
        .handleUnexpected { _ => fail("shouldn't have gotten here") }
    }.toEither

    prog.assertEquals(Right(1))
  }

  test("handleUnexpected: error from Raise shouldn't be seen by handler") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      raiseBoom()
        .flatMap(_ => successWithoutExplosion(1))
        .handleUnexpected { _ => 10 }
    }.toEither

    prog.assertEquals(Left(Boom()))
  }

  test("handleUnexpectedWith: success case (no raised or thrown error)") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      successWithoutExplosion(1)
        .handleUnexpectedWith { _ => fail("shouldn't have gotten here") }
    }.toEither

    prog.assertEquals(Right(1))
  }

  test("handleUnexpectedWith: error from Raise shouldn't be seen by handler") {
    val prog = ioHandling[ExplodeError] { implicit handle =>
      raiseBoom()
        .flatMap(_ => successWithoutExplosion(1))
        .handleUnexpectedWith { _ => IO(10) }
    }.toEither

    prog.assertEquals(Left(Boom()))
  }

  test("abortIfNone: IO[Option] success case returns the inner value") {
    val prog = ioHandling[MyError] { implicit handle =>
      IO.pure(Option(42)).abortIfNone(NotFound())
    }.toEither

    prog.assertEquals(Right(42))
  }

  test("abortIfNone: IO[Option] None raises the provided error") {
    val prog: IO[Either[MyError, Int]] = ioHandling[MyError] { implicit handle =>
      IO.pure(Option.empty[Int]).abortIfNone(NotFound())
    }.toEither

    prog.assertEquals(Left(NotFound()))
  }

}
