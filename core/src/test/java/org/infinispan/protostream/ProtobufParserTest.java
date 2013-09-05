package org.infinispan.protostream;

import com.google.protobuf.Descriptors;
import org.infinispan.protostream.domain.Account;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.Transaction;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.domain.marshallers.AccountMarshaller;
import org.infinispan.protostream.domain.marshallers.AddressMarshaller;
import org.infinispan.protostream.domain.marshallers.GenderMarshaller;
import org.infinispan.protostream.domain.marshallers.TransactionMarshaller;
import org.infinispan.protostream.domain.marshallers.UserMarshaller;
import org.infinispan.protostream.impl.Log;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author anistor@redhat.com
 */
public class ProtobufParserTest {

   private static final Log log = Log.LogFactory.getLog(ProtobufParserTest.class);

   @Test
   public void test() throws Exception {
      final SerializationContext ctx = createContext();

      Descriptors.Descriptor wrapperDescriptor = ctx.getMessageDescriptor("org.infinispan.protostream.WrappedMessage");

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(Arrays.asList(1, 3));
      user.setAddresses(Collections.singletonList(new Address("Old Street", "XYZ42")));

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, user);

      TagHandler tagHandler = new TagHandler() {
         private Descriptors.Descriptor nextDescriptor = null;

         @Override
         public void onStart() {
            log.trace("ProtobufParserTest.onStart");
         }

         @Override
         public void onTag(int fieldNumber, String fieldName, Descriptors.FieldDescriptor.Type type, Descriptors.FieldDescriptor.JavaType javaType, Object tagValue) {
            log.tracef("ProtobufParserTest.onTag %s %s", fieldName, tagValue);

            if (fieldName.equals("wrappedDescriptorFullName")) {
               nextDescriptor = ctx.getMessageDescriptor((String) tagValue);
            } else if (fieldName.equals("wrappedMessageBytes")) {
               try {
                  new ProtobufParser().parse(this, nextDescriptor, (byte[]) tagValue);
               } catch (IOException e) {
                  e.printStackTrace();  // TODO: Customise this generated block
               }
            }
         }

         @Override
         public void onStartNested(int fieldNumber, String fieldName, Descriptors.Descriptor messageDescriptor) {
            log.tracef("ProtobufParserTest.onStartNested %s", fieldName);
         }

         @Override
         public void onEndNested(int fieldNumber, String fieldName, Descriptors.Descriptor messageDescriptor) {
            log.tracef("ProtobufParserTest.onEndNested %s", fieldName);
         }

         @Override
         public void onEnd() {
            log.trace("ProtobufParserTest.onEnd");
         }
      };

      ProtobufParser parser = new ProtobufParser();
      parser.parse(tagHandler, wrapperDescriptor, bytes);
   }

   private SerializationContext createContext() throws IOException, Descriptors.DescriptorValidationException {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      ctx.registerProtofile("/message-wrapping.protobin");
      ctx.registerProtofile("/bank.protobin");
      ctx.registerMarshaller(User.class, new UserMarshaller());
      ctx.registerMarshaller(User.Gender.class, new GenderMarshaller());
      ctx.registerMarshaller(Address.class, new AddressMarshaller());
      ctx.registerMarshaller(Account.class, new AccountMarshaller());
      ctx.registerMarshaller(Transaction.class, new TransactionMarshaller());
      return ctx;
   }
}
