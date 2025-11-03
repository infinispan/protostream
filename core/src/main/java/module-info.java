module org.infinispan.protostream.core {
   requires java.compiler;
   requires jdk.unsupported;
   requires com.fasterxml.jackson.core;
   requires static com.fasterxml.jackson.databind;
   requires static com.google.errorprone.annotations;
   requires org.jboss.logging;
   requires static org.jboss.logging.annotations;
   opens org.infinispan.protostream;
   exports org.infinispan.protostream;
   exports org.infinispan.protostream.annotations;
   exports org.infinispan.protostream.config;
   exports org.infinispan.protostream.containers;
   exports org.infinispan.protostream.descriptors;
   exports org.infinispan.protostream.exception;
   exports org.infinispan.protostream.schema;
   exports org.infinispan.protostream.annotations.impl to org.infinispan.protostream.processor;
   exports org.infinispan.protostream.annotations.impl.types to org.infinispan.protostream.processor;
   exports org.infinispan.protostream.impl to org.infinispan.protostream.processor, org.jboss.logging;
}
