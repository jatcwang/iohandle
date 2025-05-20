package example
import io.github.jatcwang.iohandle.*
import cats.mtl.syntax.raise.*
import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {

  class MyError(val msg: String) extends Throwable

  class MyDetailedError(override val msg: String) extends MyError(msg)

  override def run: IO[Unit] =
//    ioHandle[MyError]:
//      for
//        _ <- go
//        _ <- MyDetailedError("asdf").raise
//      yield ()
//    .rescue: e =>
//      IO.println(e.msg)
//    .void

    ioHandling[MyError]:
      for
        _ <- hey(1)
        res <- IO.pure("success")
      yield res
    .toEither
      .map:
        case Left(e)  => println(e.msg)
        case Right(s) => println(s)

  def hey[A](a: A)(using IORaise[MyError]): IO[A] =
    ioAbort(MyDetailedError("MyDetailedError happened"))

}
