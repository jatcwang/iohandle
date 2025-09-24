package examples

import cats.effect.{IO, IOApp}
import cats.implicits.toTraverseOps
import iohandle.{IORaise, ioAbortIfNone, ioAbortIf, ioHandling}

object MoreComplexErrorHandlingExample extends IOApp.Simple {

  private final case class UserDetails(userName: String, age: Int)

  val run: IO[Unit] = {
    val maybeUser: Option[UserDetails] = Some(UserDetails("Fox", 15))
    for {
      _ <- checkValue(maybeUser)
    } yield ()
  }

  private def checkValue(maybeUser: Option[UserDetails]): IO[Unit] = {
    ioHandling[UserValidationError] { implicit handle =>
      for {
        _ <- ioAbortIfNone(maybeUser, UserNameIsEmpty("No user name provided"))
        _ <- checkIfUserNameNotEmpty(maybeUser)
        _ <- checkUserNameAge(maybeUser = maybeUser, minAge = 15)
      } yield ()
    }.rescueWith { e =>
      IO.println(e.getMessage)
    }
  }

  private def checkIfUserNameNotEmpty(maybeUser: Option[UserDetails])(
    implicit raise: IORaise[UserValidationError]): IO[Option[Unit]] = {
    maybeUser.traverse { user =>
      ioAbortIf(user.userName.isEmpty, UserNameIsEmpty("User name is empty"))
    }
  }

  private def checkUserNameAge(maybeUser: Option[UserDetails], minAge: Int)(
    implicit raise: IORaise[UserValidationError]): IO[Option[Unit]] = {
    maybeUser.traverse { user =>
      ioAbortIf(user.age < minAge, MinAgeError(minAge))
    }
  }

  sealed trait UserValidationError extends RuntimeException

  private case class MinAgeError(minAge: Int) extends UserValidationError {
    override def getMessage: String = s"Min user age is: $minAge"
  }

  case class UserNameIsEmpty(error: String) extends UserValidationError

}
