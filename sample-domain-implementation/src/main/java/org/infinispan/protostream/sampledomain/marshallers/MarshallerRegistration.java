package org.infinispan.protostream.sampledomain.marshallers;

import com.google.protobuf.Descriptors;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.sampledomain.Account;
import org.infinispan.protostream.sampledomain.Address;
import org.infinispan.protostream.sampledomain.Transaction;
import org.infinispan.protostream.sampledomain.User;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 */
public class MarshallerRegistration {

   public static final String PROTOBUF_RES = "/bank.protobin";

   public static void registerMarshallers(SerializationContext ctx) throws IOException, Descriptors.DescriptorValidationException {
      ctx.registerProtofile(PROTOBUF_RES);
      ctx.registerMarshaller(User.class, new UserMarshaller());
      ctx.registerMarshaller(User.Gender.class, new GenderMarshaller());
      ctx.registerMarshaller(Address.class, new AddressMarshaller());
      ctx.registerMarshaller(Account.class, new AccountMarshaller());
      ctx.registerMarshaller(Account.Limits.class, new LimitsMarshaller());
      ctx.registerMarshaller(Transaction.class, new TransactionMarshaller());
   }
}
