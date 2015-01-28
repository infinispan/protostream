package org.infinispan.protostream.test;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.AnnotationElement;
import org.infinispan.protostream.domain.marshallers.MarshallerRegistration;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public abstract class AbstractProtoStreamTest {

   protected SerializationContext createContext() throws IOException, DescriptorParserException {
      return createContext(new Configuration.Builder());
   }

   //todo [anistor] should accept undefined annotations too
   //todo [anistor] all annotation fields should be required unless they have defaults
   protected SerializationContext createContext(Configuration.Builder cfgBuilder) throws IOException, DescriptorParserException {
      cfgBuilder.messageAnnotation("Indexed")
            .attribute(AnnotationElement.Annotation.DEFAULT_ATTRIBUTE).booleanType().defaultValue(true);
      cfgBuilder.fieldAnnotation("IndexedField")
            .attribute("index").booleanType().defaultValue(true)
            .attribute("store").booleanType().defaultValue(true);
      SerializationContext ctx = ProtobufUtil.newSerializationContext(cfgBuilder.build());
      MarshallerRegistration.registerMarshallers(ctx);
      return ctx;
   }
}
