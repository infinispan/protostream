package org.infinispan.protostream.domain.marshallers;

import com.google.protobuf.Descriptors;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.domain.Account;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.Transaction;
import org.infinispan.protostream.domain.User;

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
      ctx.registerMarshaller(Transaction.class, new TransactionMarshaller());
   }
}
