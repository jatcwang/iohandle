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
import cats.syntax.all.*
import iohandletest.testtypes.*
import io.github.jatcwang.iohandle.{ioHandling, ioAbort, IORaise, recoverUnhandled}
import munit.CatsEffectSuite

class IOHandleSpec extends CatsEffectSuite {

  test(".rescueWith success case") {
    val prog = ioHandling[MyError]:
      IO(Right("success"))
    .rescueWith:
      case MyError.NotFound() => IO.pure(Left("not found"))
      case MyError.Broken()   => IO.pure(Left("not good"))

    prog.assertEquals(Right("success"))
  }

  test(".rescueWith error case") {

    val prog = ioHandling[MyError]:
      ioAbort(MyError.NotFound())
        .map(_ => Right(()))
    .rescueWith:
      case MyError.NotFound() => IO.pure(Left("not found"))
      case MyError.Broken()   => IO.pure(Left("not good"))

    prog.assertEquals(Left("not found"))
  }

  test(".toEither error case") {
    val prog = ioHandling[MyError]:
      ioAbort(MyError.NotFound())
        .as("shouldn't have succeeded")
    .toEither

    prog.assertEquals(Left(MyError.NotFound()))
  }

  test("Nesting works") {
    val prog =
      ioHandling[MyError.NotFound]:
        ioHandling[MyError]:
          oops()
        .rescueWith:
          case MyError.NotFound() => IO.pure(Left("inner"))
          case MyError.Broken()   => IO.pure(Left("broken"))
      .rescueWith:
        case MyError.NotFound() => IO.pure(Left("outer"))

    prog.assertEquals(Left("inner"))
  }

  test("ioHandle recoverUnhandled") {
    val io = ioHandling[String]:
      boomStr.recoverUnhandled:
        case _ => Right(())
    .rescueWith(str => IO.pure(Left(str)))

    io.assertEquals(Left("boom!"))
  }

  def oops()(using IORaise[MyError]): IO[Int] = ioAbort(MyError.NotFound())
  def boomStr(using IORaise[String]): IO[Nothing] = ioAbort("boom!")
}
