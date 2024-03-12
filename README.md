# ProtoStream
[![Build Status](https://ci.infinispan.org/buildStatus/icon?job=Protostream%2Fmain)](https://ci.infinispan.org/job/Protostream/job/main/)
[![Maven Central](https://img.shields.io/badge/maven/central-5.0.1.Final-green.svg)](http://search.maven.org/#artifactdetails|org.infinispan.protostream|protostream|5.0.1.Final|)
[![Javadoc](https://img.shields.io/badge/Javadoc-online-green.svg)](http://www.javadoc.io/doc/org.infinispan.protostream/protostream)
[![License](https://img.shields.io/github/license/infinispan/infinispan.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java 8+](https://img.shields.io/badge/java-17+-blue.svg)](http://java.oracle.com)

ProtoStream is a serialization library based on [Protobuf](https://developers.google.com/protocol-buffers/) data format. It is open source software released under the
[Apache License, v2.0](https://www.apache.org/licenses/LICENSE-2.0 "The Apache License, v2.0").
ProtoStream is part of the [Infinispan](https://github.com/infinispan/infinispan) data grid platform. For more information about Infinispan visit the project's
website on [https://infinispan.org](https://infinispan.org "The Infinispan project page")


Usage (via Maven)
-----------------

Add this dependency to your `pom.xml` file:
   
```xml
<dependency>
   <groupId>org.infinispan.protostream</groupId>
   <artifactId>protostream</artifactId>
   <version>5.0.1.Final</version>
</dependency>
```

Annotation processor
--------------------

The Java compiler discovers annotation processors differently, depending on the version and the options in use.
With Java up to version 21, it's enough to put the `protostream-processor` dependency on the classpath and it 
will be automatically discovered by the service loader mechanism. Another way, which is mandatory since Java 22,
is to use the `--processor-path` option.

```shell
javac --processor-path /path/to/protostream-processor-5.0.1.Final.jar:... ...
```

Using Maven:
```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.12.1</version>
  <configuration>
    <!-- Annotation processors -->
    <annotationProcessorPaths>
      <annotationProcessorPath>
        <groupId>org.infinispan.protostream</groupId>
        <artifactId>protostream-processor</artifactId>
        <version>5.0.1.Final</version>
      </annotationProcessorPath>
    </annotationProcessorPaths>
  </configuration>
</plugin>
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
        <version>5.0.1.Final</version>
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

