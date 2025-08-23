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

trait IOHandlePlatform {

  def ioHandling[E]: IOHandlePartiallyApplied[E] = {
    val handle = impl.createIOHandle[E]
    new IOHandlePartiallyApplied[E](handle)
  }

  private[iohandle] class IOHandlePartiallyApplied[E](val handle: IOHandle[E]) {
    def apply[A](f: IOHandle[E] => IO[A]): IOHandlePendingRescue[E, A] = new IOHandlePendingRescue(
      f,
      handle,
    )
  }

  def ioAbort[E, E1 <: E](e: E1)(implicit raise: IORaise[E]): IO[Nothing] =
    raise.raise(e)

  // FIXME: Make AnyVal (need to be in companion object though??)
  implicit class IOHandleExtensionForCatsEffectIO[A](io: IO[A]) {
    def recoverUnexpected[B >: A](pf: PartialFunction[Throwable, B]): IO[B] =
      IOHandleExtensionImpl.recoverUnexpectedWith(io, pf.andThen(IO.pure))

    def recoverUnexpectedWith[B >: A](pf: PartialFunction[Throwable, IO[B]]): IO[B] =
      IOHandleExtensionImpl.recoverUnexpectedWith(io, pf)

    def handleUnexpected[B >: A](f: Throwable => B): IO[B] =
      IOHandleExtensionImpl.handleUnexpectedWith(io, f.andThen(IO.pure))

    def handleUnexpectedWith[B >: A](f: Throwable => IO[B]): IO[B] = IOHandleExtensionImpl.handleUnexpectedWith(io, f)
  }

}
