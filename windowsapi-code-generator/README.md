# Windows API Code Generation Library

This project implements the Java code generation.
It is the core shared by the Maven and the Gradle plugin.


## Testing

The tests consist of several components:

### Unit tests

Unit tests test several classes that are straight-forward to test in isolation.

The code contains many asserts, and the unit tests must be run with assertions enabled.
The asserts verify many assumptions about the metadata, e.g. that certain combination
of elements do not exist. During development, they helped to understand metadata aspects
that are not documented in detail. Now they ensure that additions to the metadata
introducing yet unsupported elements or combinations do not go undetected.

These unit tests run on all platforms.


### Full Build

`FullBuild` (`net.codecrete.windowsapi.special.FullBuild`) is a program generating the code
for the entire Windows API. The code is written to a subdirectory of `integration-tests/full-build`
– a Java project. The code can then be compiled.

This turned out to be a very powerful test. It not only ensures that the generated code is
compilable. It also reveals most errors in the metadata processing as the code – despite being
syntactically correct – no longer compiles as it refers to missing or unsuitable classes
or functions.

The full build program and the full build project run on all platforms.


### Windows API Tests

These are unit tests using generated Java code. They exercise different elements
such as functions, COM interfaces etc.

As they actually call Windows API functions, they only run on Windows.
