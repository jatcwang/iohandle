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
import BasicHandlingExample.{NumberCheckError, checkDivisbleBy7, checkEven}
import iohandle.*

object InterceptingErrorExample extends IOApp.Simple {

  override def run: IO[Unit] = {
    for {
      res1 <- checkNumberWithIntercept(7)
      _ <- IO.println(res1)

      res2 <- checkNumberWithIntercept(12)
      _ <- IO.println(res2)

      res3 <- checkNumberWithIntercept(14)
      _ <- IO.println(res3)
    } yield ()
  }

  def checkNumberWithIntercept(num: Int): IO[String] =
    ioHandling[NumberCheckError] { implicit handle =>
      for {
        _ <- checkEven(num)
        _ <- checkDivisbleBy7(num)
          .tapError[NumberCheckError] { e => // You can intercept errors (e.g. perform additional logging)
            IO.println(s"Intercept (and re-raise): ${e.getMessage}")
          }
      } yield s"Good result: $num is even and divisible by 7!"
    }
      .rescueWith { e =>
        IO.pure(s"Bad result: ${e.getMessage}")
      }

}
