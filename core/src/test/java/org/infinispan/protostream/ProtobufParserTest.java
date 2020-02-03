package org.infinispan.protostream;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.impl.Log;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Test;

/**
 * Test the Parser/TagHandler mechanism.
 *
 * @author anistor@redhat.com
 */
public class ProtobufParserTest extends AbstractProtoStreamTest {

   private static final Log log = Log.LogFactory.getLog(ProtobufParserTest.class);

   @Test
   public void testTagHandler() throws Exception {
      ImmutableSerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Arrays.asList(new Address("Old Street", "XYZ42", -12), new Address("Bond Street", "W23", 2)));

      byte[] userBytes = ProtobufUtil.toWrappedByteArray(ctx, user);

      Descriptor wrapperDescriptor = ctx.getMessageDescriptor(WrappedMessage.PROTOBUF_TYPE_NAME);

      TagHandler messageHandler = new TagHandler() {
         @Override
         public void onStart(GenericDescriptor descriptor) {
            log.debugf("\tonStart %s", descriptor);
         }

         @Override
         public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
            log.debugf("\tonTag %d %s %s", fieldNumber, fieldDescriptor != null ? fieldDescriptor.getFullName() : null, tagValue);
         }

         @Override
         public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
            log.debugf("\tonStartNested %d %s", fieldNumber, fieldDescriptor != null ? fieldDescriptor.getFullName() : null);
         }

         @Override
         public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
            log.debugf("\tonEndNested %d %s", fieldNumber, fieldDescriptor != null ? fieldDescriptor.getFullName() : null);
         }

         @Override
         public void onEnd() {
            log.debug("\tonEnd");
         }
      };

      TagHandler wrapperHandler = new TagHandler() {
         private Integer typeId;
         private String typeName;
         private byte[] wrappedMessage;
         private Integer wrappedEnum;

         private GenericDescriptor getDescriptor() {
            return typeId != null ? ctx.getDescriptorByTypeId(typeId) : ctx.getDescriptorByName(typeName);
         }

         @Override
         public void onStart(GenericDescriptor descriptor) {
            log.debugf("onStart %s", descriptor);
         }

         @Override
         public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
            log.debugf("onTag %d %s %s", fieldNumber, fieldDescriptor != null ? fieldDescriptor.getFullName() : null, tagValue);
            if (fieldDescriptor == null) {
               // ignore unknown fields
               return;
            }
            switch (fieldNumber) {
               case WrappedMessage.WRAPPED_TYPE_ID:
                  typeId = (Integer) tagValue;
                  break;
               case WrappedMessage.WRAPPED_TYPE_NAME:
                  typeName = (String) tagValue;
                  break;
               case WrappedMessage.WRAPPED_MESSAGE:
                  wrappedMessage = (byte[]) tagValue;
                  break;
               case WrappedMessage.WRAPPED_ENUM:
                  wrappedEnum = (Integer) tagValue;
                  break;
               case WrappedMessage.WRAPPED_DOUBLE:
               case WrappedMessage.WRAPPED_FLOAT:
               case WrappedMessage.WRAPPED_INT64:
               case WrappedMessage.WRAPPED_UINT64:
               case WrappedMessage.WRAPPED_INT32:
               case WrappedMessage.WRAPPED_FIXED64:
               case WrappedMessage.WRAPPED_FIXED32:
               case WrappedMessage.WRAPPED_BOOL:
               case WrappedMessage.WRAPPED_STRING:
               case WrappedMessage.WRAPPED_BYTES:
               case WrappedMessage.WRAPPED_UINT32:
               case WrappedMessage.WRAPPED_SFIXED32:
               case WrappedMessage.WRAPPED_SFIXED64:
               case WrappedMessage.WRAPPED_SINT32:
               case WrappedMessage.WRAPPED_SINT64:
                  messageHandler.onStart(null);
                  messageHandler.onTag(fieldNumber, fieldDescriptor, tagValue);
                  messageHandler.onEnd();
                  break;
            }
         }

         @Override
         public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
            log.debugf("onStartNested %d %s", fieldNumber, fieldDescriptor != null ? fieldDescriptor.getFullName() : null);
         }

         @Override
         public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
            log.debugf("onEndNested %d %s", fieldNumber, fieldDescriptor != null ? fieldDescriptor.getFullName() : null);
         }

         @Override
         public void onEnd() {
            if (wrappedEnum != null) {
               EnumDescriptor enumDescriptor = (EnumDescriptor) getDescriptor();
               String enumConstantName = enumDescriptor.findValueByNumber(wrappedEnum).getName();
               FieldDescriptor fd = wrapperDescriptor.findFieldByNumber(WrappedMessage.WRAPPED_ENUM);
               messageHandler.onStart(enumDescriptor);
               messageHandler.onTag(WrappedMessage.WRAPPED_ENUM, fd, enumConstantName);
               messageHandler.onEnd();
            } else if (wrappedMessage != null) {
               try {
                  Descriptor messageDescriptor = (Descriptor) getDescriptor();
                  ProtobufParser.INSTANCE.parse(messageHandler, messageDescriptor, wrappedMessage);
               } catch (IOException e) {
                  throw new RuntimeException(e);
               }
            }
            log.debug("onEnd");
         }
      };

      ProtobufParser.INSTANCE.parse(wrapperHandler, wrapperDescriptor, userBytes);
   }
}
