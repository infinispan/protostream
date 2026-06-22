# Testing Instructions

## Test Framework

ProtoStream uses **JUnit 4** across all modules.

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
public class MyFeatureTest extends AbstractProtoStreamTest {

   @Test
   public void testSomething() throws Exception {
      SerializationContext ctx = createContext();
      // use ctx to serialize/deserialize
   }
}
```

### Annotation Processor Tests

Processor tests use the `compile-testing` library to verify compile-time behavior:

```java
@Test
public void testAnnotationProcessing() {
   Compilation compilation = Compiler.javac()
       .withProcessors(new ProtoSchemaAnnotationProcessor())
       .compile(JavaFileObjects.forSourceString("MyClass", source));
   assertThat(compilation).succeeded();
}
```

### Sample Domain Classes

The `sample-domain-definition` and `sample-domain-implementation` modules provide reusable test domain objects (`User`, `Address`, `Account`, etc.) with their `.proto` schemas and marshallers. Use these in tests rather than creating ad-hoc domain classes when possible.

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
