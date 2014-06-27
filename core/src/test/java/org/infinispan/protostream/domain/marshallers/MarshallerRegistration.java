package org.infinispan.protostream.domain.marshallers;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 * @author gustavonalle
 */
public class MarshallerRegistration {

   public static final String[] PROTOBUF_RES = new String[]{"/sample_bank_account/bank.proto", "/google/protobuf/descriptor.proto", "/infinispan/indexing.proto"};

   public static void registerMarshallers(SerializationContext ctx) throws IOException, DescriptorParserException {
      ctx.registerProtoFiles(FileDescriptorSource.fromResources(PROTOBUF_RES));
      ctx.registerMarshaller(new UserMarshaller());
      ctx.registerMarshaller(new GenderMarshaller());
      ctx.registerMarshaller(new AddressMarshaller());
      ctx.registerMarshaller(new AccountMarshaller());
      ctx.registerMarshaller(new LimitsMarshaller());
      ctx.registerMarshaller(new TransactionMarshaller());
   }
}
