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

import cats.data.{EitherT, Ior}
import cats.effect.*
import iohandle.*
import munit.CatsEffectSuite
import iohandletest.testtypes.MyError
import iohandletest.testtypes.MyError.{Broken, NotFound}

/** Test to test extension methods on cats.effect.IO, e.g. recoverUnexpected Note that this file is copied and made
  * Scala 3 compatible when building this project in Scala 3 (e.g. string replacement removing "implicit handle =>").
  * Search for sourceGenerators in build.sbt to see how it's done
  */
class IOHandleSpec extends CatsEffectSuite {

  test(".rescueWith success case") {

    val prog = ioHandling[MyError] { implicit handle =>
      /* start:scala-2-only */
      val _ = handle
      /* end:scala-2-only */
      IO("success")
    }
      .rescueWith {
        case NotFound() => IO.pure("not found")
        case Broken()   => IO.pure("not good")
      }

    prog.assertEquals("success")
  }

  test(".rescueWith error case") {

    val prog = ioHandling[MyError] { implicit handle =>
      ioAbort(NotFound())
        .as("shouldn't have succeeded")
    }
      .rescueWith {
        case NotFound() => IO.pure("not found")
        case Broken()   => IO.pure("broken")
      }

    prog.assertEquals("not found")
  }

  test(".toEither error case") {
    val prog: IO[Either[MyError, String]] = ioHandling[MyError] { implicit handle =>
      ioAbort(NotFound())
        .as("shouldn't have succeeded")
    }.toEither

    prog.assertEquals(Left(NotFound()))
  }

  test(".toEitherT success case") {
    val prog = ioHandling[MyError] { implicit handle =>
      /* start:scala-2-only */
      val _ = handle
      /* end:scala-2-only */
      IO("success")
    }.toEitherT

    prog.value.assertEquals(Right("success"))
  }

  test(".toEitherT error case") {
    val prog: EitherT[IO, MyError, String] = ioHandling[MyError] { implicit handle =>
      ioAbort(NotFound())
        .as("shouldn't have succeeded")
    }.toEitherT

    prog.value.assertEquals(Left(NotFound()))
  }

  test(".toIor error case") {

    val prog = ioHandling[MyError] { implicit handle =>
      ioAbort(NotFound())
        .as("shouldn't have succeeded")
    }

    prog.toIor.assertEquals(Ior.Left(NotFound()))
  }

  test("ioAbortIf: succeeds if condition is false") {
    val prog = ioHandling[MyError] { implicit handle =>
      ioAbortIf(false, Broken())
    }.toEither

    prog.assertEquals(Right(()))
  }

  test("ioAbortIf: aborts with provided error when condition is true") {
    val prog = ioHandling[MyError] { implicit handle =>
      ioAbortIf(true, Broken())
    }.toEither

    prog.assertEquals(Left(Broken()))
  }

  test("ioAbortIfNone: Succeeds and return the value in Some") {
    val prog = ioHandling[MyError] { implicit handle =>
      ioAbortIfNone(Option(42), NotFound())
    }.toEither

    prog.assertEquals(Right(42))
  }

  test("ioAbortIfNone: None raises the provided error") {
    val prog: IO[Either[MyError, Int]] = ioHandling[MyError] { implicit handle =>
      ioAbortIfNone(Option.empty[Int], NotFound())
    }.toEither

    prog.assertEquals(Left(NotFound()))
  }

  test("ioAbortIfSome[E]: Suceeds if None") {
    val prog = ioHandling[MyError] { implicit handle =>
      ioAbortIfSome[MyError](Option.empty[MyError])
    }.toEither

    prog.assertEquals(Right(()))
  }

  test("ioAbortIfSome[E]: raises the contained error in Some") {
    val prog = ioHandling[MyError] { implicit handle =>
      ioAbortIfSome[MyError](Option(NotFound()))
    }.toEither

    prog.assertEquals(Left(NotFound()))
  }

  test("IO[Option].abortIfSome: Succeeds if None") {
    val prog = ioHandling[MyError] { implicit handle =>
      IO.pure(Option.empty[MyError]).abortIfSome
    }.toEither

    prog.assertEquals(Right(()))
  }

  test("IO[Option].abortIfSome: Some raises the contained error") {
    val prog = ioHandling[MyError] { implicit handle =>
      IO.pure(Option(NotFound())).abortIfSome
    }.toEither

    prog.assertEquals(Left(NotFound()))
  }

  test("IO[Either].abortIfLeft: Right succeeds and returns value") {
    val prog = ioHandling[MyError] { implicit handle =>
      IO.pure[Either[MyError, Int]](Right(42)).abortIfLeft
    }.toEither

    prog.assertEquals(Right(42))
  }

  test("IO[Either].abortIfLeft: Left aborts with provided error") {
    val prog = ioHandling[MyError] { implicit handle =>
      IO.pure[Either[MyError, Int]](Left(NotFound())).abortIfLeft
    }.toEither

    prog.assertEquals(Left(NotFound()))
  }

  test("IO[Either].abortIfRight: Left succeeds and returns value") {
    val prog = ioHandling[MyError] { implicit handle =>
      IO.pure[Either[Int, MyError]](Left(42)).abortIfRight
    }.toEither

    prog.assertEquals(Right(42))
  }

  test("IO[Either].abortIfRight: Right aborts with provided error") {
    val prog = ioHandling[MyError] { implicit handle =>
      IO.pure[Either[Int, MyError]](Right(NotFound())).abortIfRight
    }.toEither

    prog.assertEquals(Left(NotFound()))
  }

  test("ioAbortIfLeft: Right succeeds and returns value") {
    val prog = ioHandling[MyError] { implicit handle =>
      ioAbortIfLeft(Right(42))
    }.toEither

    prog.assertEquals(Right(42))
  }

  test("ioAbortIfLeft: Left aborts with provided error") {
    val prog = ioHandling[MyError] { implicit handle =>
      ioAbortIfLeft(Left(NotFound()))
    }.toEither

    prog.assertEquals(Left(NotFound()))
  }

  test("ioAbortIfRight: Left succeeds and returns value") {
    val prog = ioHandling[MyError] { implicit handle =>
      ioAbortIfRight(Left[Int, MyError](42))
    }.toEither

    prog.assertEquals(Right(42))
  }

  test("ioAbortIfRight: Right aborts with provided error") {
    val prog = ioHandling[MyError] { implicit handle =>
      ioAbortIfRight(Right[Int, MyError](NotFound()))
    }.toEither

    prog.assertEquals(Left(NotFound()))
  }

}
