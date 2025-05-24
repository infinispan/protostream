import org.infinispan.protostream.processor.ProtoSchemaAnnotationProcessor;

module org.infinispan.protostream.processor {
   requires java.compiler;
   requires org.infinispan.protostream.core;

   provides javax.annotation.processing.Processor with ProtoSchemaAnnotationProcessor;
}
