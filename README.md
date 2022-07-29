# ProtoStream
[![Build Status](https://ci.infinispan.org/buildStatus/icon?job=Protostream%2Fmain)](https://ci.infinispan.org/job/Protostream/job/main/)
[![Maven Central](https://img.shields.io/badge/maven/central-4.4.3.Final-green.svg)](http://search.maven.org/#artifactdetails|org.infinispan.protostream|protostream|4.4.3.Final|)
[![Javadoc](https://img.shields.io/badge/Javadoc-online-green.svg)](http://www.javadoc.io/doc/org.infinispan.protostream/protostream)
[![License](https://img.shields.io/github/license/infinispan/infinispan.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java 8+](https://img.shields.io/badge/java-8+-blue.svg)](http://java.oracle.com)

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
   <version>4.5.0-SNAPSHOT</version>
</dependency>
```

Annotation processor
--------------------

The annotation processor should be automatically discovered by the compiler when
the `org.infinispan.protostream:protostream-processor` dependency is on the classpath using
the service loader.

The annotation processor supports some configuration options:

* `protostream.debug` prints out debug information during the processing phase.
* `protostream.fullyqualifiedannotations` when generating the `.proto` files, the annotation names are stripped of the package name. This flag keeps the fully-qualified annotation names in the generated files.

You can pass these arguments to `javac` using the `-Aoption=value` argument. 
The following `pom.xml` snippet shows how to do it with Maven:

```xml
<plugin>
   <groupId>org.apache.maven.plugins</groupId>
   <artifactId>maven-compiler-plugin</artifactId>
   <version>3.9.0</version>
   <configuration>
      <compilerArgs>
         <arg>-Aprotostream-debug=true</arg>
      </compilerArgs>
   </configuration>
</plugin>
```

Requirements
------------

Target runtime platform is Java 11.

Requires Java 11 or newer to build.

Maven 3.6.0 or newer.

Bugs
----
Bug reports go [here](https://issues.jboss.org/projects/IPROTO)

