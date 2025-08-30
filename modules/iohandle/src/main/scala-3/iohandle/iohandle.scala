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
import cats.mtl.Raise
import scala.compiletime.summonFrom

type IORaise[-E] = Raise[IO, E]

inline def ioHandling[E]: IOHandlePartiallyApplied[E] = {
  val handle = impl.createIOHandle[E]
  new IOHandlePartiallyApplied[E](handle)
}

inline def ioAbort[E, E1 <: E](e: E1)(using raise: IORaise[E]): IO[Nothing] = raise.raise(e)

inline def ioAbortIf[E](cond: Boolean, e: => E)(using raise: IORaise[E]): IO[Unit] =
  if cond then raise.raise(e) else IO.unit

inline def ioAbortIfNone[E, A](opt: Option[A], e: => E)(using raise: IORaise[E]): IO[A] =
  opt match
    case Some(a) => IO.pure(a)
    case None    => raise.raise(e)

inline def ioAbortIfSome[E](opt: Option[E])(using raise: IORaise[E]): IO[Unit] =
  opt match
    case Some(err) => raise.raise(err)
    case None      => IO.unit

inline def ioAbortIfLeft[L, R](either: Either[L, R])(using raise: IORaise[L]): IO[R] =
  either match
    case Left(e)  => raise.raise(e)
    case Right(a) => IO.pure(a)

inline def ioAbortIfRight[L, R](either: Either[L, R])(using raise: IORaise[R]): IO[L] =
  either match
    case Left(a)  => IO.pure(a)
    case Right(e) => raise.raise(e)

extension [A](io: IO[A]) {
  inline def recoverUnexpected[B >: A](pf: PartialFunction[Throwable, B]): IO[B] =
    IOHandleExtensionImpl.recoverUnexpectedWith(io, pf.andThen(IO.pure))

  inline def recoverUnexpectedWith[B >: A](pf: PartialFunction[Throwable, IO[B]]): IO[B] =
    IOHandleExtensionImpl.recoverUnexpectedWith(io, pf)

  inline def handleUnexpected[B >: A](f: Throwable => B): IO[B] =
    IOHandleExtensionImpl.handleUnexpectedWith(io, f.andThen(IO.pure))

  inline def handleUnexpectedWith[B >: A](f: Throwable => IO[B]): IO[B] =
    IOHandleExtensionImpl.handleUnexpectedWith(io, f)
}

extension [A](ioOpt: IO[Option[A]]) {
  inline def abortIfNone[E](e: => E)(using raise: IORaise[E]): IO[A] =
    ioOpt.flatMap {
      case Some(a) => IO.pure(a)
      case None    => raise.raise(e)
    }

  inline def abortIfSome(using raise: IORaise[A]): IO[Unit] =
    ioOpt.flatMap {
      case Some(err) => raise.raise(err)
      case None      => IO.unit
    }
}

extension [L, R](ioEither: IO[Either[L, R]]) {
  inline def abortIfLeft(using raise: IORaise[L]): IO[R] =
    ioEither.flatMap {
      case Left(e)  => raise.raise(e)
      case Right(a) => IO.pure(a)
    }

  inline def abortIfRight(using raise: IORaise[R]): IO[L] =
    ioEither.flatMap {
      case Left(a)  => IO.pure(a)
      case Right(e) => raise.raise(e)
    }
}

private[iohandle] class IOHandlePartiallyApplied[E](val handle: IOHandle[E]) {
  inline def apply[A](inline f: IOHandle[E] ?=> IO[A]): IOHandlePendingRescue[E, A] = new IOHandlePendingRescue(
    convert(f),
    handle,
  )

  private inline def convert[A, B](inline f: A ?=> B): A => B =
    implicit a: A => f
}
