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

  /** Creates an error-handling scope for the error type E. Within the scope, (typed) errors can be raised using
    * [[ioAbort]] and other similar methods
    *
    * {{{
    * val prog: IO[String] = ioHandling { implicit handle =>
    *     for {
    *       isSuccess <- checkSomething
    *       _ <- if (isSuccess) ioAbort(SomeError("oops")) else IO.unit
    *     } yield "succeeded"
    *   }
    *   .rescue {
    *     e => e.message
    *   }
    * }}}
    * @tparam E
    * @return
    */
  def ioHandling[E]: IOHandlePartiallyApplied[E] = {
    val handle = impl.createIOHandle[E]
    new IOHandlePartiallyApplied[E](handle)
  }

  /** Abort the execution with the provided error, akin to IO.raiseError. Requires an implicit IORaise[E] instance,
    * which you can obtain via calling [[ioHandling]]
    */
  def ioAbort[E, E1 <: E](e: E1)(implicit raise: IORaise[E]): IO[Nothing] =
    raise.raise(e)

  /** Abort the execution with the provided error if condition evaluates to true
    */
  def ioAbortIf[E](cond: Boolean, e: => E)(implicit raise: IORaise[E]): IO[Unit] =
    if (cond) raise.raise(e) else IO.unit

  /** If the provided option value is None, Abort the execution with the provided error
    */
  def ioAbortIfNone[E, A](opt: Option[A], e: => E)(implicit raise: IORaise[E]): IO[A] =
    opt match {
      case Some(a) => IO.pure(a)
      case None    => raise.raise(e)
    }

  /** If the provided option value is Some(e), Abort the execution with it
    */
  def ioAbortIfSome[E](opt: Option[E])(implicit raise: IORaise[E]): IO[Unit] =
    opt match {
      case Some(err) => raise.raise(err)
      case None      => IO.unit
    }

  /** Abort the execution if the provided Either value is a Left(e)
    */
  def ioAbortIfLeft[L, R](either: Either[L, R])(implicit raise: IORaise[L]): IO[R] =
    either match {
      case Left(e)  => raise.raise(e)
      case Right(a) => IO.pure(a)
    }

  /** Abort the execution if the provided Either value is a Right(e)
    */
  def ioAbortIfRight[L, R](either: Either[L, R])(implicit raise: IORaise[R]): IO[L] =
    either match {
      case Left(a)  => IO.pure(a)
      case Right(e) => raise.raise(e)
    }

  implicit class IOExtensionForIOHandle[A](val io: IO[A]) extends AnyVal {

    /** Like [[cats.effect.IO.recover]], but user do not need to handle [[iohandle.IOHandleErrorWrapper]] (it is
      * automatically re-raised without exposing it to the user)
      */
    def recoverUnexpected[B >: A](pf: PartialFunction[Throwable, B]): IO[B] =
      IOHandleExtensionImpl.recoverUnexpectedWith(io, pf.andThen(IO.pure))

    /** Like [[cats.effect.IO.recoverWith]], but user do not need to handle [[iohandle.IOHandleErrorWrapper]] (it is
      * automatically re-raised without exposing it to the user)
      */
    def recoverUnexpectedWith[B >: A](pf: PartialFunction[Throwable, IO[B]]): IO[B] =
      IOHandleExtensionImpl.recoverUnexpectedWith(io, pf)

    /** Like [[cats.effect.IO.handleError]], but user do not need to handle [[iohandle.IOHandleErrorWrapper]] (it is
      * automatically re-raised without exposing it to the user)
      */
    def handleUnexpected[B >: A](f: Throwable => B): IO[B] =
      IOHandleExtensionImpl.handleUnexpectedWith(io, f.andThen(IO.pure))

    /** Like [[cats.effect.IO.handleErrorWith]], but user do not need to deal with the error potentially being
      * [[iohandle.IOHandleErrorWrapper]] (it is automatically re-raised without exposing it to the user)
      */
    def handleUnexpectedWith[B >: A](f: Throwable => IO[B]): IO[B] = IOHandleExtensionImpl.handleUnexpectedWith(io, f)
  }

  /** Extension methods available on a IO[Option[A]] value, for convenience */
  implicit class IOOptionOps[A](val ioOpt: IO[Option[A]]) extends AnyVal {

    /** Abort the execution with the provided error if the IO result this is called on is a None.
      *
      * {{{
      *   for {
      *     user <- getUser(userId).abortIfNone(UserNotFound(userId))
      *   } yield user
      * }}}
      */
    def abortIfNone[E](e: => E)(implicit raise: IORaise[E]): IO[A] =
      ioOpt.flatMap {
        case Some(a) => IO.pure(a)
        case None    => raise.raise(e)
      }

    /** Abort the execution with result of the IO results in a Some(e)
      *
      * {{{
      *   for {
      *     _ <- getUser(userName).map(opt => opt.map(_ => UserNameAlreadyUsed(userName))).abortIfSome
      *     user <- createUser(userName, email)
      *   } yield user
      * }}}
      */
    def abortIfSome(implicit raise: IORaise[A]): IO[Unit] =
      ioOpt.flatMap {
        case Some(err) => raise.raise(err)
        case None      => IO.unit
      }
  }

  /** Extension methods available on a IO[Option[A]] value, for convenience */
  implicit class IOEitherAbortIfLeftOps[L, R](val ioEither: IO[Either[L, R]]) extends AnyVal {

    /** Abort the execution if the IO results in a Left(e)
      */
    def abortIfLeft(implicit raise: IORaise[L]): IO[R] =
      ioEither.flatMap {
        case Left(e)  => raise.raise(e)
        case Right(a) => IO.pure(a)
      }

    /** Abort the execution if the IO results in a Right(e)
      */
    def abortIfRight(implicit raise: IORaise[R]): IO[L] =
      ioEither.flatMap {
        case Left(a)  => IO.pure(a)
        case Right(e) => raise.raise(e)
      }
  }

}
