import cats.effect.{IO, IOApp, Ref}

import scala.concurrent.duration.*
import io.github.jatcwang.iohandle.ioHandling

/*
  Use
 */
object StupidFizzBuzzWithHandler extends IOApp.Simple {

  sealed trait CustomError
  case class ErrorCaseOne() extends CustomError

  val run: IO[Unit] =
    for {
      ctr <- Ref.of[IO, Int](1)

      wait = IO.sleep(1.second)
      poll = ioHandling[CustomError] { implicit handle =>
        val expression = wait *> ctr.get
        expression
          .map(_ > 10)
          .ifM(handle.raise(ErrorCaseOne()), expression)

      }.rescueWith {
        case _: ErrorCaseOne =>
          IO.pure(-1)
      }

      _ <- poll.flatMap(IO.println(_)).foreverM.start
      _ <- poll.map(_ % 3 == 0).ifM(IO.println("fizz"), IO.unit).foreverM.start

      _ <- (wait *> ctr.update(_ + 1)).foreverM.void
    } yield ()
}