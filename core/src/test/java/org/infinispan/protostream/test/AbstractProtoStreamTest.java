package org.infinispan.protostream.test;

import org.infinispan.protostream.Configuration;
import org.infinispan.protostream.ConfigurationBuilder;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.domain.marshallers.MarshallerRegistration;
import org.infinispan.protostream.DescriptorParserException;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public abstract class AbstractProtoStreamTest {

   protected SerializationContext createContext() throws IOException, DescriptorParserException {
      SerializationContext ctx = ProtobufUtil.newSerializationContext(new ConfigurationBuilder().build());
      MarshallerRegistration.registerMarshallers(ctx);
      return ctx;
   }

   protected SerializationContext createContext(Configuration cfg) throws IOException, DescriptorParserException {
      SerializationContext ctx = ProtobufUtil.newSerializationContext(cfg);
      MarshallerRegistration.registerMarshallers(ctx);
      return ctx;
   }
}
