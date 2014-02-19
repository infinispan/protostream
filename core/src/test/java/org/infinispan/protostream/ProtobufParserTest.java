package org.infinispan.protostream;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.impl.Log;
import org.infinispan.protostream.impl.WrappedMessageMarshaller;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author anistor@redhat.com
 */
public class ProtobufParserTest extends AbstractProtoStreamTest {

   private static final Log log = Log.LogFactory.getLog(ProtobufParserTest.class);

   @Test
   public void test() throws Exception {
      final SerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(Arrays.asList(1, 3));
      user.setAddresses(Collections.singletonList(new Address("Old Street", "XYZ42")));

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, user);

      TagHandler tagHandler = new TagHandler() {
         private Descriptor nextDescriptor = null;

         @Override
         public void onStart() {
            log.trace("onStart");
         }

         @Override
         public void onTag(int fieldNumber, String fieldName, FieldDescriptor.Type type, FieldDescriptor.JavaType javaType, Object tagValue) {
            log.tracef("onTag %s %s", fieldName, tagValue);

            switch (fieldNumber) {
               case WrappedMessageMarshaller.WRAPPED_DESCRIPTOR_FULL_NAME:
                  nextDescriptor = ctx.getMessageDescriptor((String) tagValue);
                  break;
               case WrappedMessageMarshaller.WRAPPED_MESSAGE_BYTES:
                  try {
                     // todo here we expect WRAPPED_DESCRIPTOR_FULL_NAME was already read, which might not be the case
                     ProtobufParser.INSTANCE.parse(this, nextDescriptor, (byte[]) tagValue);
                  } catch (IOException e) {
                     throw new RuntimeException(e);
                  }
            }
         }

         @Override
         public void onStartNested(int fieldNumber, String fieldName, Descriptor messageDescriptor) {
            log.tracef("onStartNested %s", fieldName);
         }

         @Override
         public void onEndNested(int fieldNumber, String fieldName, Descriptor messageDescriptor) {
            log.tracef("onEndNested %s", fieldName);
         }

         @Override
         public void onEnd() {
            log.trace("onEnd");
         }
      };

      Descriptor wrapperDescriptor = ctx.getMessageDescriptor(WrappedMessage.PROTOBUF_TYPE_NAME);

      ProtobufParser.INSTANCE.parse(tagHandler, wrapperDescriptor, bytes);
   }
}
