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

import cats.data.EitherT
import cats.effect.*
import iohandletest.testtypes.*
import io.github.jatcwang.iohandle.{ioAbort, ioHandling}
import munit.CatsEffectSuite

class IOHandleSpec extends CatsEffectSuite {

  test(".rescueWith success case") {

    val prog = ioHandling[MyError] { implicit handle =>
      val _ = handle
      IO("success")
    }
      .rescueWith {
        case MyError.NotFound() => IO.pure("not found")
        case MyError.Broken() => IO.pure("not good")
      }

    prog.assertEquals("success")
  }

  test(".rescueWith error case") {

    val prog = ioHandling[MyError] { implicit handle =>
      ioAbort(MyError.NotFound())
        .as("shouldn't have succeeded")
    }
      .rescueWith {
        case MyError.NotFound() => IO.pure("not found")
        case MyError.Broken() => IO.pure("not good")
      }

    prog.assertEquals("not found")
  }

  test(".toEither error case") {

    val prog: IO[Either[MyError, String]] = ioHandling[MyError] { implicit handle =>
      ioAbort(MyError.NotFound())
        .as("shouldn't have succeeded")
    }.toEither

    prog.assertEquals(Left(MyError.NotFound()))
  }


  test(".toEitherT success case") {
    val prog = ioHandling[MyError] { implicit handle =>
      val _ = handle
      IO("success")
    }.toEitherT

    prog.value.assertEquals(Right("success"))
  }

  test(".toEitherT error case") {
    val prog: EitherT[IO, MyError, String] = ioHandling[MyError] { implicit handle =>
      ioAbort(MyError.NotFound())
        .as("shouldn't have succeeded")
    }.toEitherT

    prog.value.assertEquals(Left(MyError.NotFound()))
  }

}
