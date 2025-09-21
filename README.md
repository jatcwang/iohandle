# IOHandle - Ergonomic error handling for cats.effect.IO 

IOHandle is a small library that provides ergonomic type-safe error handling
for cats-effect IO.

It is based on cats-mtl's [Handle](https://typelevel.org/cats-mtl/mtl-classes/handle.html) and [Raise](https://typelevel.org/cats-mtl/mtl-classes/raise.html) capabilities,
but specialized for `cats.effect.IO` with some additional helpers and user-friendliness.

<!-- TOC -->
* [Installation](#installation)
* [Usage](#usage)
    * [Example: Uploading a file](#example-uploading-a-file)
  * [How it works](#how-it-works)
    * [`IOHandle` and `IORaise` capability](#iohandle-and-ioraise-capability)
* [Inspirations & Comparisons](#inspirations--comparisons)
<!-- TOC -->

# Installation

```
libraryDepedencies ++= Seq("com.github.jatcwang" %% "iohandle" % "<VERSION>")
```

# Usage

1. Call `ioHandling[E]` and start an error-handling scope for the provided error (`E`)
1. Anywhere within the scope, you can call `ioAbort(e: E)` to abort the execution
1. Wrap it all up by specifying how the typed error is finally handled. You can..
  - Transform/Process the error using `.rescue` / `.rescueWith`
  - Convert the result to an `Either[E, A]` using `.toEither`
1. Extra note: Instead of calling methods like `handleErrorWith` to handle untyped exceptions from an IO, switch to using `handleUnexpectedWith`.
  This is because `ioAbort` internally uses `IO.raiseError` with a special exception, so these extension methods will help you avoid accidentally interfering with IOHandle's error processing.
  See **How it works** section for more details.

If you squint a little bit, it is similar to `try-catch` except it works with `IO`

Scala 3:
```scala
val prog: IO[String] = 
  ioHandling[SomeError]:
    for
      isSuccess <- checkSomething
      _ <- if (isSuccess) ioAbort(SomeError("oops")) else IO.unit
    yield "success!"
  .rescue:
     e => e.message
```

### Example: Uploading a file

Let's look an example of handling a user's file upload, where possible errors are FileTooLarge and QuotaExceeded

Scala 3:
```scala
import cats.effect.IO
import iohandle.{ioHandling, ioAbort, ioAbortIf}

def uploadFile(userId: UserId, parentPath: Path, file: File): IO[Either[UploadError, String]] =
  ioHandling[UploadError]:
    for
      _ <- if (file.size > MaxPerFileBytes) 
             ioAbort(FileTooLarge(MaxPerFileBytes, file.size))
           else IO.unit

      used <- getUsedQuota(userId)
      remaining = MaxUserQuotaBytes - used
      // ioAbortIf is a equivalent to `if (..) ioAbort(..) else IO.unit`
      _ <- ioAbortIf(remaining < file.size, QuotaExceeded(userId, remaining))

      url <- saveToStorage(userId, file)
    yield url
  .toEither
```

Scala 2:
```scala
import cats.effect.IO
import iohandle.*

def uploadFile(userId: UserId, parentPath: Path, file: File): IO[Either[UploadError, String]] =
  ioHandling[UploadError] { implicit handle =>
    for {
      _ <- if (file.size > MaxPerFileBytes) 
             ioAbort(FileTooLarge(MaxPerFileBytes, file.size))
           else IO.unit

      used <- getUsedQuota(userId)
      remaining = MaxUserQuotaBytes - used
      _ <- ioAbortIf(remaining < file.size, QuotaExceeded(userId, remaining))

      url <- saveToStorage(userId, file)
    }
    yield url
  }
  .toEither
```

## How it works

When `ioHandling[E]` is called, a "capability" value of type `IOHandle`  is created with a unique marker.

When `ioAbort` is called with your domain error `myError`, it wraps your error value in a special exception `IOHandleErrorWrapper(myError, marker)`
and throws it using `IO.raiseError`. When `ioHandling` checks for errors, it matches `IOHandleErrorWrapper` and compares its marker to
the one it created. If they match, it knows it can extract an error of type `E` from the caught `IOHandleErrorWrapper`.

If we deconstruct all the code surrounding `ioHandling`, below is essentially what it boils down to:
```scala
def doStuff(input: Int)(using IORaise[MyError]): IO[Int] = ...

val uniqueMarker = new Object // java.lang.Object are compared by reference

given IORaise[MyError] = new IORaise[MyError] {
  def raise(e: MyError): IO[Nothing]
}

doStuff(42)
  .handleErrorWith {
    case s: IOHandleErrorWrapper[?] if s.marker == marker => 
      // Because the marker matched the one we created above, we know the error is of type MyError
      val myError = s.error.asInstanceOf[MyError]
      // ... do stuff with myError
    case e => 
      // For any other types of exceptions, or IOHandleErrorWrapper with a different marker, 
      // re-throw them because they'll be handled by their own handlers
      IO.raiseError(e)
  }
```

### IOHandle and IORaise capability

**IORaise[E]** allows you to raise an error of type `E`.

* It is contravariant, which means if you have a `IORaise[ParentError]`, the same `IORaise` instance can act as `IORaise[SubError]`.
  This is useful for limiting what error each function can raise.
* It is a specialization of [`cats.mtl.Raise`](https://typelevel.org/cats-mtl/mtl-classes/raise.html) for the effect type `cats.effect.IO`

**IOHandle[E]** capability extends `IORaise[E]`, allowing you to **intercept** and handle error of type `E` in addition to just raising them.

* In most cases a function only require the `IORaise[E]` capability, so we recommend doing just that.
* It is a specialization of [`cats.mtl.Handle`](https://typelevel.org/cats-mtl/mtl-classes/handle.html) for the effect type `cats.effect.IO`

# Inspirations & Comparisons

- [cats-mtl's "Submarine Error Handling"](https://typelevel.org/blog/2025/09/02/custom-error-types.html)
  - This library uses the same mechanism as detailed in the blog post, with some minor API differences and user-friendliness
- ValdemarGr's [catch-effect](https://github.com/ValdemarGr/catch-effect) library
    - Difference: We rely on `IO.raiseError` instead of IO cancellation
