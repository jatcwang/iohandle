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

package examples

import cats.effect.{IO, IOApp}
import iohandle.{IORaise, ioAbort, ioHandling}

/** This example shows the usage of IORaise in function signatures to accurately codify the error that can be raised
  * within the function
  */
object BasicHandlingExample extends IOApp.Simple {

  val run: IO[Unit] =
    for
      _ <- checkNumber(7)
      _ <- checkNumber(12)
      _ <- checkNumber(14)
    yield ()

  def checkNumber(num: Int): IO[Unit] =
    ioHandling[NumberCheckError]:
      (
        for {
          _ <- checkEven(num)
          _ <- checkDivisbleBy7(num)
          _ <- IO.println(s"$num is even and divisible by 7!")
        } yield ()
      ).tapError[NumberCheckError] { e =>
        IO.println(s"There is a number check error: ${e.getMessage()}")
      }
    .rescueWith: e =>
      IO.unit // just ignore the error

  def checkEven(num: Int)(implicit raise: IORaise[NotEven]): IO[Unit] =
    if (num % 2 != 0) ioAbort(NotEven(num)) else IO.unit

  def checkDivisbleBy7(num: Int)(implicit raise: IORaise[NotDivisbleBy7]): IO[Unit] =
    if (num % 7 != 0) ioAbort(NotDivisbleBy7(num)) else IO.unit

  sealed trait NumberCheckError extends RuntimeException
  case class NotEven(num: Int) extends NumberCheckError {
    override def getMessage: String = s"$num is not even"
  }
  case class NotDivisbleBy7(num: Int) extends NumberCheckError {
    override def getMessage: String = s"$num is not divisible by 7"
  }
}
