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

package iohandle

import cats.effect.IO
import cats.effect.implicits.*
import cats.implicits.*

// Note: This file is "cross-compiled" to Scala 3 via sbt sourceGenerator + search and replace
package object ioscreen {
  implicit class IOScreenOps[E](val ioScreen: IOScreen[E]) extends AnyVal {

    /** For every element in the input, evaluate the IO action. Any reported errors during the IO action execution are
     * accumulated, and any screening aborts (from calling IOScreen#reject) are suppressed
     */
    def traverseScreening[A, B](inputs: List[A])(f: A => IO[B]): IO[List[B]] =
      inputs
        .traverse { a =>
          ioScreen.suppressReject(f(a))
        }
        .flatMap { resultOptions =>
          val results = resultOptions.flatten
          if (results.isEmpty)
            ioScreen.reject
          else
            IO.pure(results)
        }

    /** Execute two IOs in parallel, accumulating errors and suppressing any aborts from both of them
     */
    def parZipScreening[A0, A1](f0: IOScreen[E] => IO[A0], f1: IOScreen[E] => IO[A1]): IO[(A0, A1)] =
      (ioScreen.suppressReject(f0(ioScreen)), ioScreen.suppressReject(f1(ioScreen))).parFlatMapN {
        case (Some(r0), Some(r1)) => IO.pure((r0, r1))
        case _                    => ioScreen.reject
      }

    def parZipScreening[A0, A1, A2](
      f0: IOScreen[E] => IO[A0],
      f1: IOScreen[E] => IO[A1],
      f2: IOScreen[E] => IO[A2],
    ): IO[(A0, A1, A2)] =
      (
        ioScreen.suppressReject(f0(ioScreen)),
        ioScreen.suppressReject(f1(ioScreen)),
        ioScreen.suppressReject(f2(ioScreen)),
      ).parFlatMapN {
        case (Some(r0), Some(r1), Some(r2)) => IO.pure((r0, r1, r2))
        case _                              => ioScreen.reject
      }

    def parZipScreening[A0, A1, A2, A3](
      f0: IOScreen[E] => IO[A0],
      f1: IOScreen[E] => IO[A1],
      f2: IOScreen[E] => IO[A2],
      f3: IOScreen[E] => IO[A3],
    ): IO[(A0, A1, A2, A3)] =
      (
        ioScreen.suppressReject(f0(ioScreen)),
        ioScreen.suppressReject(f1(ioScreen)),
        ioScreen.suppressReject(f2(ioScreen)),
        ioScreen.suppressReject(f3(ioScreen)),
      )
        .parFlatMapN {
          case (Some(r0), Some(r1), Some(r2), Some(r3)) => IO.pure((r0, r1, r2, r3))
          case _                                        => ioScreen.reject
        }

    def parZipScreening[A0, A1, A2, A3, A4](
      f0: IOScreen[E] => IO[A0],
      f1: IOScreen[E] => IO[A1],
      f2: IOScreen[E] => IO[A2],
      f3: IOScreen[E] => IO[A3],
      f4: IOScreen[E] => IO[A4],
    ): IO[(A0, A1, A2, A3, A4)] =
      (
        ioScreen.suppressReject(f0(ioScreen)),
        ioScreen.suppressReject(f1(ioScreen)),
        ioScreen.suppressReject(f2(ioScreen)),
        ioScreen.suppressReject(f3(ioScreen)),
        ioScreen.suppressReject(f4(ioScreen)),
      ).parFlatMapN {
        case (Some(r0), Some(r1), Some(r2), Some(r3), Some(r4)) => IO.pure((r0, r1, r2, r3, r4))
        case _                                                  => ioScreen.reject
      }
  }

}
