package org.infinispan.protostream.sampledomain.marshallers;

import com.google.protobuf.Descriptors;
import org.infinispan.protostream.SerializationContext;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 */
public class MarshallerRegistration {

   public static final String PROTOBUF_RES = "/sample_bank_account/bank.protobin";

   public static void registerMarshallers(SerializationContext ctx) throws IOException, Descriptors.DescriptorValidationException {
      ctx.registerProtofile(PROTOBUF_RES);
      ctx.registerMarshaller(new UserMarshaller());
      ctx.registerMarshaller(new GenderMarshaller());
      ctx.registerMarshaller(new AddressMarshaller());
      ctx.registerMarshaller(new AccountMarshaller());
      ctx.registerMarshaller(new LimitsMarshaller());
      ctx.registerMarshaller(new TransactionMarshaller());
   }
}
