## Tech Stack
* **Java Version:** 17 (use JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64/ to build with a newer JDK, compiler target is 17)
* **Build Tool:** Maven multi-module (use `./mvnw`)
* **Key Frameworks:** JUnit 4, Mockito, compile-testing

## Project Architecture

* **parent:** Parent POM with shared dependency and plugin versions
* **core:** Core library — annotations, descriptors, serialization runtime (`TagReader`/`TagWriter`), `.proto` schema parsing
* **processor:** Annotation processor for compile-time marshaller and `.proto` schema generation
* **processor-tests:** Tests for the annotation processor (Java)
* **processor-tests-kotlin:** Tests for the annotation processor (Kotlin)
* **types:** Pre-built adapters for common JDK types (`BigDecimal`, `UUID`, `LocalDateTime`, collections, etc.)
* **integrationtests:** Integration tests
* **sample-domain-definition:** Sample `.proto` domain definitions used across tests
* **sample-domain-implementation:** Sample marshaller implementations used across tests
* **maven-plugin:** Maven plugin for `.proto` schema backward compatibility checks

## Common Build Commands
* **Full build (skip tests):** `./mvnw install -DskipTests -Dcheckstyle.skip`
* **Build a single module:** `./mvnw install -pl core -DskipTests -Dcheckstyle.skip`
* **Build a module and its dependencies:** `./mvnw install -pl processor-tests -am -DskipTests -Dcheckstyle.skip`
* **Run a single test class:** `./mvnw test -pl core -Dtest=ProtobufUtilTest -Dcheckstyle.skip`
* **Run a single test method:** `./mvnw test -pl core -Dtest=ProtobufUtilTest#testMethodName -Dcheckstyle.skip`
* **Processor tests** depend on core+processor being built first:
  ```bash
  ./mvnw install -pl core,processor -DskipTests -Dcheckstyle.skip
  ./mvnw test -pl processor-tests -Dcheckstyle.skip
  ```

## Key Packages (core module)
* `org.infinispan.protostream` — Public API: `ProtobufUtil` (entry point), `SerializationContext`, `TagReader`/`TagWriter`, marshaller interfaces
* `org.infinispan.protostream.annotations` — User-facing annotations: `@ProtoSchema`, `@Proto`, `@ProtoField`, `@ProtoFactory`, `@ProtoAdapter`
* `org.infinispan.protostream.descriptors` — Proto schema object model: `FileDescriptor`, `Descriptor`, `FieldDescriptor`, `EnumDescriptor`
* `org.infinispan.protostream.impl` — Runtime implementation: `SerializationContextImpl`, `TagReaderImpl`/`TagWriterImpl`, proto file parser
* `org.infinispan.protostream.impl.json` — JSON-to-protobuf bridging

## Key Packages (processor module)
* `ProtoSchemaAnnotationProcessor` — Main JSR 269 processor, entry point for compile-time code generation
* `MarshallerSourceCodeGenerator` — Generates marshaller Java source code
* `CompileTimeProtoSchemaGenerator` — Generates `.proto` schema files
* `types/MirrorTypeFactory` — Compile-time implementation of the `XTypeFactory` abstraction (uses `javax.lang.model` instead of reflection)

## How Serialization Works
1. Users annotate classes with `@Proto`/`@ProtoField` and create a `@ProtoSchema`-annotated interface
2. At compile time, the annotation processor generates: marshaller classes, `.proto` schema files, and a `SerializationContextInitializer` implementation
3. At runtime, `ProtobufUtil.newSerializationContext()` creates a context, the generated initializer registers schemas and marshallers
4. `ProtobufUtil.toByteArray()`/`fromByteArray()` perform serialization via `TagWriter`/`TagReader`

## Development Standards
* **Style:** Functional programming patterns where possible; use Records for DTOs
* **Coding style:** Skip checkstyle with `-Dcheckstyle.skip` during development. Format code before committing.
* **Commit logs:** Must start with `[#00000] Summary` (issue number)
* **Git branches:** Name as `issueid/issue_summary`, use `origin/main` as upstream

## Related Projects
* **Infinispan:** The main consumer of ProtoStream — source code is in ../infinispan
