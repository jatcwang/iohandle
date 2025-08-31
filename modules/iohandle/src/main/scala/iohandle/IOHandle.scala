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

import cats.Applicative
import cats.data.EitherT
import cats.effect.IO
import cats.mtl.Handle

import scala.util.control.NoStackTrace
import cats.data.Ior

trait IOHandle[E] extends Handle[IO, E] { self =>
  override def applicative: Applicative[IO] = IO.asyncForIO

  final def imap[E3](f: E => E3)(g: E3 => E): Handle[IO, E3] = new IOHandle[E3] {
    override def handleWith[A](fa: IO[A])(f2: E3 => IO[A]): IO[A] =
      self.handleWith(fa)(e => f2(f(e)))

    override def raise[E2 <: E3, A](e: E2): IO[A] =
      self.raise(g(e))
  }
}

private[iohandle] class IOHandleImpl[E] private[iohandle] (marker: AnyRef) extends IOHandle[E] { self =>
  override def handleWith[A](fa: IO[A])(f: E => IO[A]): IO[A] =
    fa.handleErrorWith {
      case s: IOHandleErrorWrapper[?] if s.marker == marker => f(s.error.asInstanceOf[E])
      case e                                                => IO.raiseError(e)
    }

  override def raise[E2 <: E, A](e: E2): IO[A] = IO.raiseError(new IOHandleErrorWrapper(e, marker))
}

final class IOHandleErrorWrapper[E](val error: E, val marker: AnyRef) extends RuntimeException with NoStackTrace {
  override def getMessage: String =
    """You caught iohandle's "IOHandleErrorWrapper" exception, which is used to carry the underlying error specified by ioHandling.""" +
      " You should typically rethrow IOHandleErrorWrapper exception because ioHandling should be the one dealing with it." +
      " (Tip: import iohandle.* and recoverUnexpected/handleUnexpected will be available as extension methods on cats.effect.IO)." +
      " If you're seeing this exception outside of the scope set by ioHandling, it's possible that you have leaked an IOHandle/IORaise instance" +
      " and use it raise an exception outside of the scope it should be used"
}

private object impl {
  def createIOHandle[E]: IOHandle[E] =
    new IOHandleImpl[E](new AnyRef)
}

// TODO: eliminate allocation?
private[iohandle] class IOHandlePendingRescue[E, A](
  private val body: IOHandle[E] => IO[A],
  private val ioHandle: IOHandle[E],
) {

  def rescueWith(handler: E => IO[A]): IO[A] =
    ioHandle.handleWith(body(ioHandle))(handler)

  def rescue(handler: E => A): IO[A] =
    ioHandle.handleWith(body(ioHandle))(handler.andThen(IO.pure))

  def toEither: IO[Either[E, A]] = {
    ioHandle.handleWith[Either[E, A]](body(ioHandle).map(Right(_)))(e => IO.pure(Left(e)))
  }

  def toEitherT: EitherT[IO, E, A] = EitherT(toEither)

  def toIor: IO[Ior[E, A]] = {
    ioHandle.handleWith[Ior[E, A]](body(ioHandle).map(Ior.Right(_)))(e => IO.pure(Ior.Left(e)))
  }
}
