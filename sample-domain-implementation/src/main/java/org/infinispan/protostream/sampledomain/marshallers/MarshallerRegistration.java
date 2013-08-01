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

   public static void registerMarshallers(SerializationContext ctx) throws IOException, Descriptors.DescriptorValidationException {
      ctx.registerProtofile("/bank.protobin");
      ctx.registerMarshaller(User.class, new UserMarshaller());
      ctx.registerEnumEncoder(User.Gender.class, new GenderEncoder());
      ctx.registerMarshaller(Address.class, new AddressMarshaller());
      ctx.registerMarshaller(Account.class, new AccountMarshaller());
      ctx.registerMarshaller(Transaction.class, new TransactionMarshaller());
   }
}
