
# Copy-replace test files

In an attempt to maintain cross-Scala version consistency between Scala 2 and 3 tests,
we currently do a copy-replace of test files during compilation from Scala 2 test source files.
This means that we will try to write our tests to be both Scala 2 and 3 compatible, with a few
syntactic adjustments.

As part of this copy-replace we use text replacement to make the source files
compilable in Scala 3.

Namely, we need to change

```
ioHandling[SomeError] { implicit handle =>
```

to 

```
ioHandling[SomeError] {
```

because the latter will fail to compile because it doesn't work with context functions.

If you want to remove certain part of the code you can use 
```
/* start:scala-2-only */
...
/* end:scala-2-only */
```

This is implemented using build.sbt (See `editSourceCodeForScala3Compilation`) using SBT's `sourceGenerators` feature




