# Building and Testing ProtoStream

This document describes how to build ProtoStream from source, run its tests, and run its
coverage-guided fuzzers. For contribution workflow (forking, branching, commit format) see
[CONTRIBUTING.md](CONTRIBUTING.md).

## Prerequisites

* **JDK 17 or later.** The compiler target is 17; newer JDKs work, just point `JAVA_HOME` at them.
* **Maven** — not required locally: the repository ships a Maven wrapper, so every command below
  starts with `./mvnw`.

## Building

```sh
# full build, skipping tests (fastest way to install all modules locally)
./mvnw install -DskipTests

# full build with tests and checkstyle
./mvnw install

# build one module and the modules it depends on
./mvnw install -pl core -am -DskipTests

# during development: skip code style checks
./mvnw install -DskipTests -Dcheckstyle.skip
```

## Modules

| Module                     | Purpose                                                          |
|----------------------------|------------------------------------------------------------------|
| `parent`                   | Parent POM: shared dependency and plugin versions                |
| `core`                     | Core library: annotations, descriptors, serialization runtime, `.proto` parsing, and the sample test domain |
| `processor`                | Annotation processor for compile-time marshaller/schema generation |
| `processor-tests`          | Tests for the annotation processor (Java)                        |
| `processor-tests-kotlin`   | Tests for the annotation processor (Kotlin)                      |
| `types`                    | Ready-made adapters for common JDK types                         |
| `integrationtests`         | Cross-module integration tests                                   |
| `maven-plugin`             | Maven plugin for `.proto` schema backward-compatibility checks   |

## Running tests

```sh
# all tests of a module
./mvnw test -pl core

# a single test class
./mvnw test -pl core -Dtest=ProtobufUtilTest

# a single test method
./mvnw test -pl core -Dtest=ProtobufUtilTest#testMethodName

# during development: skip code style checks
./mvnw test -pl core -Dcheckstyle.skip
```

The `processor-tests` and `processor-tests-kotlin` modules require `core` and `processor` to be
installed first:

```sh
./mvnw install -pl core,processor -DskipTests -Dcheckstyle.skip
./mvnw test -pl processor-tests
```

## Fuzzing

The wire-format parsing paths are covered by
[Jazzer](https://github.com/CodeIntelligenceTesting/jazzer) fuzz tests (see
[#755](https://github.com/infinispan/protostream/issues/755)). Each fuzz test pins an *invariant*
rather than specific inputs, e.g. "any byte sequence either parses or is rejected with
`MalformedProtobufException` — never an unchecked exception — and never loops". Current fuzz tests,
all in `core`:

| Fuzz test                    | Target                                                   |
|------------------------------|----------------------------------------------------------|
| `ProtobufMessageEditorFuzzTest` | `ProtobufMessageEditor` scanner/decoder: robustness, no-edit byte identity |
| `ProtobufUtilFuzzTest`       | `ProtobufUtil` wrapped-message envelope: split/join round-trip |
| `ProtobufParserFuzzTest`     | event-based `ProtobufParser`: no unchecked exceptions     |

### Regression mode (default)

A plain test run executes the fuzz tests in **regression mode**: Jazzer replays the seed corpus
committed under `core/src/test/resources/org/infinispan/protostream/<FuzzTest>Inputs/` (plus, when
`JAZZER_COVERAGE=1` — the default — the generated corpus in `core/.cifuzz-corpus/`, which is
gitignored). This behaves like ordinary unit tests and costs almost nothing in CI.

```sh
./mvnw test -pl core
```

### Fuzzing mode

Real coverage-guided fuzzing is enabled with the `fuzz` Maven profile, which passes
`JAZZER_FUZZ=1` to the test JVM:

```sh
./mvnw test -Pfuzz -pl core -Dtest=ProtobufMessageEditorFuzzTest
```

Notes:

* **One fuzz test per run.** In fuzzing mode libFuzzer keeps global state, so Jazzer executes only
  a single fuzz test per test run — select it explicitly with `-Dtest=<FuzzTest>`, as above.
* The `-Pfuzz` profile is the supported entry point. Setting `JAZZER_FUZZ=1` in your shell does
  *not* work, because the surefire configuration passes the variable to the forked test JVM
  explicitly and would override it.
* Each session runs for the duration given in the `@FuzzTest(maxDuration = ...)` annotation
  (currently 5 minutes). libFuzzer reports `-max_len` defaults to 4096 bytes, which is ample for
  wire-format scanning; raise it through Jazzer's libFuzzer arguments if deep-nesting inputs ever
  become relevant.

### Crashes and corpora

* **Crashing inputs** found in a fuzzing session are written to
  `core/src/test/resources/org/infinispan/protostream/<FuzzTest>Inputs/<testMethod>/` with a
  descriptive name. Committing them turns them into permanent regression cases — always do this
  after fixing the finding they exposed.
* **Seed corpus** files live directly in
  `core/src/test/resources/org/infinispan/protostream/<FuzzTest>Inputs/` and are shared by all fuzz
  methods of that class. They are marked binary via `.gitattributes` so git never converts line
  endings on checkout; add new ones the same way.
* **Generated corpus** entries (inputs libFuzzer found coverage-interesting, not crashes) go to
  `core/.cifuzz-corpus/`. They are gitignored; promote individual entries to the committed seed
  corpus when they capture a scenario worth keeping.

## Environment variables

| Variable          | Default | Meaning                                                                 |
|-------------------|---------|-------------------------------------------------------------------------|
| `JAZZER_FUZZ`     | `0`     | `0` = regression mode, `1` = fuzzing mode (set by the `fuzz` profile)   |
| `JAZZER_COVERAGE` | `1`     | in regression mode, also replay the generated `.cifuzz-corpus`         |
