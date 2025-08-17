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

private[iohandle] object IOHandleExtensionImpl {
  def recoverUnexpectedWith[A, B >: A](io: IO[A], pf: PartialFunction[Throwable, IO[B]]): IO[B] = io.handleErrorWith {
    case e: Submarine[?] => IO.raiseError(e)
    case e: Throwable    => pf.applyOrElse(e, IO.raiseError)
  }

  def handleUnexpectedWith[A, B >: A](io: IO[A], f: Throwable => IO[B]): IO[B] = io.handleErrorWith {
    case e: Submarine[?] => IO.raiseError(e)
    case e: Throwable    => f(e)
  }
}
