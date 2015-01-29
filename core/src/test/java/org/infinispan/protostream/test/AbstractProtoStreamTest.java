package org.infinispan.protostream.test;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
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

   protected SerializationContext createContext(Configuration.Builder cfgBuilder) throws IOException, DescriptorParserException {
      SerializationContext ctx = ProtobufUtil.newSerializationContext(cfgBuilder.build());
      MarshallerRegistration.registerMarshallers(ctx);
      return ctx;
   }
}
