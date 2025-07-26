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

package io.github.jatcwang.iohandle

import cats.effect.IO
import scala.compiletime.summonFrom

trait IOHandlePlatform {

  inline def ioHandling[E]: IOHandlePartiallyApplied[E] = {
    val handle = impl.createIOHandle[E]
    new IOHandlePartiallyApplied[E](handle)
  }

  // TODO:  eliminate allocation
  private[iohandle] class IOHandlePartiallyApplied[E](val handle: IOHandle[E]) {
    inline def apply[A](inline f: IOHandle[E] ?=> IO[A]): IOHandlePendingRescue[E, A] = new IOHandlePendingRescue(
      convert(f),
      handle,
    )

    private inline def convert[A, B](inline f: A ?=> B): A => B =
      implicit a: A => f
  }

  inline def ioAbort[E, E1 <: E](e: E1)(using raise: IORaise[E]): IO[Nothing] = raise.raise(e)

  extension [A](io: IO[A]) {
    def recoverUnhandled[B >: A](pf: PartialFunction[Throwable, B]): IO[B] = io.recoverWith {
      case e: Submarine[?] => IO.raiseError(e)
      case e: Throwable    => io.recover(pf)
    }

  }

}
