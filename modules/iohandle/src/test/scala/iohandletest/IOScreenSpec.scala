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

import cats.data.{Ior, NonEmptyVector}
import cats.effect.IO
import iohandle.*
import iohandle.ioscreen.*
import munit.CatsEffectSuite

import scala.annotation.nowarn

class IOScreenSpec extends CatsEffectSuite {

  test("ioScreening: report and reject") {
    val prog = ioScreening[String] { ioScreen =>
      for {
        _ <- ioScreen.report("2")
        _ <- ioScreen.report("1")
        _ <- ioScreen.reject
        res <- IO.pure(42)
      } yield res
    }.toIor
    prog.assertEquals(Ior.Left(NonEmptyVector.of("2", "1")))
  }: @nowarn("msg=.*dead code.*")

  test("ioScreening: report (no reject)") {
    val prog = ioScreening[String] { ioScreen =>
      for {
        _ <- ioScreen.report("warn 1")
        _ <- ioScreen.report("warn 2")
        res <- IO.pure(42)
      } yield res
    }.toIor
    val expectedErrors = NonEmptyVector.of("warn 1", "warn 2")
    prog.assertEquals(Ior.both(expectedErrors, 42))
  }

  test("IOScreen scope should capture any reject called and not abort") {
    @nowarn("msg=.*dead code.*")
    def checkAndAbort(ioScreen: IOScreen[String]): IO[Unit] = {
      for {
        _ <- ioScreen.report("2")
        _ <- ioScreen.reject
      } yield ()
    }

    val prog = ioScreening[String] { ioScreen =>
      for {
        _ <- ioScreen.report("1")
        _ <- ioScreen.suppressReject(checkAndAbort(ioScreen))
        _ <- ioScreen.report("3")
      } yield 42
    }.toIor

    prog.assertEquals(Ior.both(NonEmptyVector.of("1", "2", "3"), 42))
  }

  test("traverseReporting: All entries succeed") {
    val inputs = List(1, 2, 3)

    val prog = ioScreening[String] { handle =>
      handle
        .traverseScreening(inputs) { num =>
          IO.pure(num * 10)
        }
    }.toIor

    val expectedResults = List(10, 20, 30)

    prog.assertEquals(Ior.Right(expectedResults))
  }

  test("traverseReporting: Partial success (some fails, some reports but still succeeds)") {
    val inputs = List(1, 2, 3, 4)

    val prog = ioScreening[String] { handle =>
      handle.traverseScreening(inputs) { num =>
        for {
          _ <- if (num % 2 != 0) handle.report(s"warn: $num not even") else IO.unit
          _ <- if (num > 2) handle.reportAndReject(s"fail: $num too big") else IO.unit
        } yield num
      }
    }.toIor

    val expectedErrors = NonEmptyVector.of("warn: 1 not even", "warn: 3 not even", "fail: 3 too big", "fail: 4 too big")
    val expectedResults = List(1, 2)

    prog.assertEquals(Ior.both(expectedErrors, expectedResults))
  }

  test("traverseReporting: All entries fail") {
    val inputs = List(1, 2)

    val prog = ioScreening[String] { handle =>
      handle.traverseScreening(inputs) { num =>
        handle.reportAndReject(s"$num failed")
      }
    }.toIor

    val expectedErrors = NonEmptyVector.of("1 failed", "2 failed")

    prog.assertEquals(Ior.Left(expectedErrors))
  }

  test("parZipScreening: Succeeds (no reports)") {
    def screen(i: Int)(ioScreen: IOScreen[String]): IO[Int] = IO.pure(i)
    def report(i: Int)(ioScreen: IOReport[String]): IO[Int] = IO.pure(i)

    val prog = ioScreening[String] { handle =>
      handle.parZipScreening(screen(1), report(2))
    }.toIor

    prog.assertEquals(Ior.Right((1, 2)))
  }: @nowarn("msg=.*never used.*")

  test("parZipScreening: Succeeds with some reports") {
    def screenWithReport(i: Int)(ioScreen: IOScreen[String]): IO[Int] =
      ioScreen.report(s"warn from $i") *> IO.pure(i)

    def screenWithoutReport(i: Int)(ioScreen: IOScreen[String]): IO[Int] = {
      val _ = ioScreen
      IO.pure(i)
    }

    val prog = ioScreening[String] { handle =>
      handle.parZipScreening(screenWithReport(10), screenWithoutReport(20))
    }.toIor

    val expectedErrors = NonEmptyVector.of("warn from 10")
    prog.assertEquals(Ior.both(expectedErrors, (10, 20)))
  }

  test("parZipScreening: Fails due to reject") {
    def screenWithReject(i: Int)(ioScreen: IOScreen[String]): IO[Int] =
      ioScreen.report(s"fail from $i") *> ioScreen.reject *> IO.pure(i)

    def screenWithoutReport(i: Int)(ioScreen: IOScreen[String]): IO[Int] =
      ioScreen.report(s"warn from $i") *> IO.pure(i)

    val prog = ioScreening[String] { handle =>
      handle.parZipScreening(screenWithReject(10), screenWithoutReport(20))
    }.toIor

    prog.map {
      case Ior.Left(errors) =>
        assertEquals(errors.sorted, NonEmptyVector.of("fail from 10", "warn from 20"))
      case _ => fail("Expected rejection")
    }
  }

}
