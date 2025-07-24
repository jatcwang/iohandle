package iohandletest

object testtypes {
  sealed trait MyError
  object MyError {
    case class NotFound() extends MyError
    case class Broken() extends MyError
  }

}
