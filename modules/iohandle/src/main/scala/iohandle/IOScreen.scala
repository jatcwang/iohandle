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
import cats.data.{Ior, NonEmptyVector}
import cats.effect.{IO, Ref}

import scala.util.control.NoStackTrace

/** Capability for reporting validation errors */
trait IOReport[-E] {
  def report(e: E): IO[Unit]
}

/** Capability for reporting validation errors and "rejecting", which will abort the execution */
trait IOScreen[E] extends IOReport[E] {

  /** Abort the execution. Before calling reject, you should've reported at least one error using [[report]] */
  def reject: IO[Nothing]

  /** Suppress any rejects from this IOScreen instance within the execution of the provided IO. Evaluates to None if the
    * IO was rejected during execution.
    */
  def suppressReject[A](io: IO[A]): IO[Option[A]]

  /** Execute the given IO and collect any errors reported 
    * If no errors are reported and there was no IOScreen rejection, return Ior.Right with the IO's result. 
    * If errors were reported but reject wasn't called, returns Ior.Both with both the errors and the IO's result
    * If errors were reported and reject was called, returns Ior.Left with the errors
    * 
    * Note that only rejects from this IOScreen instance are considered. Reject from other IOScreen instances
    * is treated no differently from any other exceptions raised.
    */
  def handle[A](io: IO[A]): IO[Ior[NonEmptyVector[E], A]]

  /** Helper method equivalent to reporting an error then immediately calling reject */
  final def reportAndReject(e: E): IO[Nothing] = report(e).flatMap(_ => reject)
}

final class IOScreenRejectException(val marker: Marker) extends Throwable with NoStackTrace {
  override def getMessage: String = s"fixme"
}

final private[iohandle] class IOScreenImpl[E](marker: Marker, accumulated: Ref[IO, Vector[E]]) extends IOScreen[E] {
  override def suppressReject[A](io: IO[A]): IO[Option[A]] =
    io.map(Some(_)).recoverWith {
      case e: IOScreenRejectException if e.marker == marker =>
        IO.pure(None)
    }

  override def handle[A](io: IO[A]): IO[Ior[NonEmptyVector[E], A]] = {
    io.attempt.flatMap {
      case Left(e: IOScreenRejectException) if e.marker == marker =>
        accumulated.get.flatMap { v =>
          NonEmptyVector.fromVector(v) match {
            case None =>
              IO.raiseError(
                new RuntimeException("IOScreenRejectException raised but no error has been accumulated yet"),
              )
            case Some(nev) => IO.pure(Ior.Left(nev))
          }
        }
      case Left(e) => IO.raiseError(e)
      case Right(a) =>
        accumulated.get.flatMap { errs =>
          IO.pure(
            NonEmptyVector.fromVector(errs) match {
              case Some(nev) => Ior.Both(nev, a)
              case None      => Ior.Right(a)
            },
          )
        }
    }
  }

  override def report(e: E): IO[Unit] = accumulated.update(_.appended(e))

  override def reject: IO[Nothing] = IO.raiseError(new IOScreenRejectException(marker))
}

object IOScreen {

  class IOScreenPartiallyApplied[E] {
    def apply[A](body: IOScreen[E] => IO[A]): IOScreenPendingRescue[E, A] = {
      val marker = new Marker
      val IOScreen = for {
        accumulator <- Ref.of[IO, Vector[E]](Vector.empty)
      } yield new IOScreenImpl[E](marker, accumulator)
      new IOScreenPendingRescue(body, IOScreen)
    }
  }

  class IOScreenPendingRescue[E, A](body: IOScreen[E] => IO[A], mkIOScreen: IO[IOScreen[E]]) {
    def toEither: IO[Either[NonEmptyVector[E], A]] = {
      for {
        ioScreen <- mkIOScreen
        res <- ioScreen.handle[A](body(ioScreen)).map(_.toEither)
      } yield res
    }

    def toIor: IO[Ior[NonEmptyVector[E], A]] = {
      for {
        ioScreen <- mkIOScreen
        res <- ioScreen.handle[A](body(ioScreen))
      } yield res
    }
  }

}
