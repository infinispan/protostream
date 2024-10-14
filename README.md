# ![ProtoStream](./protostream_logo.png)

![Maven Central Version](https://img.shields.io/maven-central/v/org.infinispan.protostream/protostream?versionPrefix=5&style=for-the-badge)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/infinispan/protostream/test_report.yaml?branch=main&style=for-the-badge)
[![License](https://img.shields.io/github/license/infinispan/infinispan?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
![Supported JVM Versions](https://img.shields.io/badge/JVM-17-green.svg?style=for-the-badge&logo=openjdk)
[![Javadoc](https://img.shields.io/badge/Javadoc-online-green.svg?style=for-the-badge&logo=openjdk)](http://www.javadoc.io/doc/org.infinispan.protostream/protostream)
[![Project Chat](https://img.shields.io/badge/zulip-join_chat-pink.svg?style=for-the-badge&logo=zulip)](https://infinispan.zulipchat.com/)

ProtoStream is a Java serialization library based on the [Protocol Buffers](https://developers.google.com/protocol-buffers/) data format. 
It is open source software released under the [Apache License, v2.0](https://www.apache.org/licenses/LICENSE-2.0 "The Apache License, v2.0").
ProtoStream is part of the [Infinispan](https://github.com/infinispan/infinispan) platform, but can be used standalone.
For more information about Infinispan visit the project's website on [https://infinispan.org](https://infinispan.org "the Infinispan project page").
Documentation on how to use ProtoStream in your projects is available [https://infinispan.org/docs/stable/titles/encoding/encoding.html#marshalling_user_types](here).

Features
--------
* annotate your Java classes, enums and records to automatically generate the `.proto` schema
* provide adapters for third-party classes
* compile-time generation of high-performance protocol buffers serializers / deserializers
* Protocol Buffers 2 and 3
* support for custom annotations
* provides a library of ready-made adapters for common JDK classes (`BigDecimal`, `UUID`, `ArrayList`, `LocalDateTime` and more)
* programmatic `.proto` schema generation
* built-in backwards compatibility checks to ensure schemas use [https://protobuf.dev/programming-guides/dos-donts/](Protocol Buffers best practices)

Usage
-----

If you are using Maven, add this dependency to your `pom.xml` file:
   
```xml
<dependency>
   <groupId>org.infinispan.protostream</groupId>
   <artifactId>protostream</artifactId>
   <version>5.0.12.Final</version>
</dependency>
```

If you are using Gradle, add this dependency to your build file:

```
dependencies { 
    implementation 'org.infinispan.protostream:protostream:5.0.12.Final'
}
```


Annotation processor
--------------------

The Java compiler discovers annotation processors differently, depending on the version and the options in use.
With Java up to version 21, it's enough to put the `protostream-processor` dependency on the classpath and it 
will be automatically discovered by the service loader mechanism. Another way, which is mandatory since Java 22,
is to use the `--processor-path` option.

```shell
javac --processor-path /path/to/protostream-processor-5.0.5.Final.jar:... ...
```

Using Maven:
```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.13.0</version>
  <configuration>
    <!-- Annotation processors -->
    <annotationProcessorPaths>
      <annotationProcessorPath>
        <groupId>org.infinispan.protostream</groupId>
        <artifactId>protostream-processor</artifactId>
        <version>5.0.5.Final</version>
      </annotationProcessorPath>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

Using Gradle:
```
dependencies {
    annotationProcessor 'org.infinispan.protostream:protostream-processor:5.0.5.Final'
    ...
}
```

The annotation processor supports some configuration options:

* `protostream.debug` prints out debug information during the processing phase.
* `protostream.fullyqualifiedannotations` when generating the `.proto` files, the annotation names are stripped of the package name. This flag keeps the fully-qualified annotation names in the generated files.

You can pass these arguments to `javac` using the `-Aoption=value` argument. 
The following `pom.xml` snippet shows how to do it with Maven:

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.12.1</version>
  <configuration>
    <compilerArgs>
      <arg>-Aprotostream-debug=true</arg>
    </compilerArgs>
    <!-- Annotation processors -->
    <annotationProcessorPaths>
      <annotationProcessorPath>
        <groupId>org.infinispan.protostream</groupId>
        <artifactId>protostream-processor</artifactId>
        <version>5.0.5.Final</version>
      </annotationProcessorPath>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

Requirements
------------

Target runtime platform is Java 17 or newer.

Requires Java 17 or newer to build.

Maven 3.6.0 or newer.

Bugs
----
Bug reports go [here](https://issues.jboss.org/projects/IPROTO)

