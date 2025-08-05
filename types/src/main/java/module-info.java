module org.infinispan.protostream.types {
   requires org.infinispan.protostream.core;
   requires org.jboss.logging;
   requires java.compiler;
   opens org.infinispan.protostream.types;
   opens org.infinispan.protostream.types.java;
   opens org.infinispan.protostream.types.protobuf;
   exports org.infinispan.protostream.types.java;
   exports org.infinispan.protostream.types.protobuf;
}
