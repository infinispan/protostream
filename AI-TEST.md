# Testing Instructions

## Test Framework

ProtoStream uses **JUnit 6** (Jupiter) across all modules.

| Module              | Test Focus                                      | Test Naming |
|---------------------|--------------------------------------------------|-------------|
| core                | Serialization, parsing, descriptors, utilities  | `*Test`     |
| processor-tests     | Annotation processor code generation            | `*Test`     |
| processor-tests-kotlin | Kotlin-specific annotation processor tests   | `*Test`     |
| integrationtests    | Cross-module integration                        | `*Test`     |

## Writing Tests

### Core Module Tests

Most core tests extend `AbstractProtoStreamTest`, which provides helper methods for creating a `SerializationContext` with sample domain schemas and marshallers pre-registered.

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MyFeatureTest extends AbstractProtoStreamTest {

   @Test
   public void testSomething() throws Exception {
      SerializationContext ctx = createContext();
      // use ctx to serialize/deserialize
   }
}
```

**Important**: JUnit 6 assertion methods place the message parameter **last** (unlike JUnit 4 where it was first):
```java
assertEquals(expected, actual, "message");
assertTrue(condition, "message");
assertThrows(ExpectedException.class, () -> codeUnderTest());
```

### Annotation Processor Tests

Processor tests use the `compile-testing` library to verify compile-time behavior:

```java
import org.junit.jupiter.api.Test;

@Test
public void testAnnotationProcessing() {
   Compilation compilation = Compiler.javac()
       .withProcessors(new ProtoSchemaAnnotationProcessor())
       .compile(JavaFileObjects.forSourceString("MyClass", source));
   assertThat(compilation).succeeded();
}
```

### Sample Domain Classes

The reusable sample test domain objects (`User`, `Address`, `Account`, etc.) with their `.proto` schema and marshallers live in `core`'s test sources under `org.infinispan.protostream.domain` and `org.infinispan.protostream.domain.marshallers` (register them via `MarshallerRegistration.registerMarshallers`, or extend `AbstractProtoStreamTest`). Use these in tests rather than creating ad-hoc domain classes when possible.

## Running Tests

```bash
# Run all tests in a module
./mvnw test -pl core -Dcheckstyle.skip

# Run a single test class
./mvnw test -pl core -Dtest=ProtobufUtilTest -Dcheckstyle.skip

# Run a single test method
./mvnw test -pl core -Dtest=ProtobufUtilTest#testMethodName -Dcheckstyle.skip

# Processor tests (must build core+processor first)
./mvnw install -pl core,processor -DskipTests -Dcheckstyle.skip
./mvnw test -pl processor-tests -Dcheckstyle.skip
```

### Fuzz Tests

`*FuzzTest` classes (Jazzer) run in regression mode as ordinary tests in every normal test run,
replaying the seed corpus. Run a coverage-guided fuzzing session with the `fuzz` profile, one fuzz
test per run: `./mvnw test -Pfuzz -pl core -Dtest=ProtobufMessageEditorFuzzTest`. See
[README-Build.md](README-Build.md) for corpus and crash-input handling.
