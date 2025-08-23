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

import cats.effect.IO
import cats.mtl.Raise

package object iohandle {

  type IORaise[-E] = Raise[IO, E]

  def ioHandling[E]: IOHandlePartiallyApplied[E] = {
    val handle = impl.createIOHandle[E]
    new IOHandlePartiallyApplied[E](handle)
  }

  def ioAbort[E, E1 <: E](e: E1)(implicit raise: IORaise[E]): IO[Nothing] =
    raise.raise(e)

  def ioAbortIf[E](cond: Boolean, e: => E)(implicit raise: IORaise[E]): IO[Unit] =
    if (cond) raise.raise(e) else IO.unit

  def ioAbortIfNone[E, A](opt: Option[A], e: => E)(implicit raise: IORaise[E]): IO[A] =
    opt match {
      case Some(a) => IO.pure(a)
      case None    => raise.raise(e)
    }

  def ioAbortIfSome[E](opt: Option[E])(implicit raise: IORaise[E]): IO[Unit] =
    opt match {
      case Some(err) => raise.raise(err)
      case None      => IO.unit
    }

  def ioAbortIfLeft[L, R](either: Either[L, R])(implicit raise: IORaise[L]): IO[R] =
    either match {
      case Left(e)  => raise.raise(e)
      case Right(a) => IO.pure(a)
    }

  def ioAbortIfRight[L, R](either: Either[L, R])(implicit raise: IORaise[R]): IO[L] =
    either match {
      case Left(a)  => IO.pure(a)
      case Right(e) => raise.raise(e)
    }

  implicit class IOExtensionForIOHandle[A](val io: IO[A]) extends AnyVal {
    def recoverUnexpected[B >: A](pf: PartialFunction[Throwable, B]): IO[B] =
      IOHandleExtensionImpl.recoverUnexpectedWith(io, pf.andThen(IO.pure))

    def recoverUnexpectedWith[B >: A](pf: PartialFunction[Throwable, IO[B]]): IO[B] =
      IOHandleExtensionImpl.recoverUnexpectedWith(io, pf)

    def handleUnexpected[B >: A](f: Throwable => B): IO[B] =
      IOHandleExtensionImpl.handleUnexpectedWith(io, f.andThen(IO.pure))

    def handleUnexpectedWith[B >: A](f: Throwable => IO[B]): IO[B] = IOHandleExtensionImpl.handleUnexpectedWith(io, f)
  }

  implicit class IOOptionOps[A](val ioOpt: IO[Option[A]]) extends AnyVal {
    def abortIfNone[E](e: => E)(implicit raise: IORaise[E]): IO[A] =
      ioOpt.flatMap {
        case Some(a) => IO.pure(a)
        case None    => raise.raise(e)
      }

    def abortIfSome(implicit raise: IORaise[A]): IO[Unit] =
      ioOpt.flatMap {
        case Some(err) => raise.raise(err)
        case None      => IO.unit
      }
  }

  implicit class IOEitherAbortIfLeftOps[L, R](val ioEither: IO[Either[L, R]]) extends AnyVal {
    def abortIfLeft(implicit raise: IORaise[L]): IO[R] =
      ioEither.flatMap {
        case Left(e)  => raise.raise(e)
        case Right(a) => IO.pure(a)
      }

    def abortIfRight(implicit raise: IORaise[R]): IO[L] =
      ioEither.flatMap {
        case Left(a)  => IO.pure(a)
        case Right(e) => raise.raise(e)
      }
  }

}
