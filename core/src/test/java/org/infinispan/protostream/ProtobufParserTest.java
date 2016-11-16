package org.infinispan.protostream;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Type;
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
      final SerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Arrays.asList(new Address("Old Street", "XYZ42", -12), new Address("Bond Street", "W23", 2)));

      byte[] userBytes = ProtobufUtil.toWrappedByteArray(ctx, user);

      TagHandler tagHandler = new TagHandler() {
         private Descriptor messageDescriptor = null;

         @Override
         public void onStart() {
            log.debug("onStart");
         }

         @Override
         public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
            log.debugf("onTag %s %s", fieldDescriptor.getName(), tagValue);

            switch (fieldNumber) {
               case WrappedMessage.WRAPPED_DESCRIPTOR_ID:
                  String typeName = ctx.getTypeNameById((Integer) tagValue);
                  messageDescriptor = ctx.getMessageDescriptor(typeName);
                  break;
               case WrappedMessage.WRAPPED_DESCRIPTOR_FULL_NAME:
                  messageDescriptor = ctx.getMessageDescriptor((String) tagValue);
                  break;
               case WrappedMessage.WRAPPED_MESSAGE_BYTES:
                  try {
                     // todo here we expect WRAPPED_DESCRIPTOR_FULL_NAME or WRAPPED_DESCRIPTOR_ID was already read, which might not be the case
                     ProtobufParser.INSTANCE.parse(this, messageDescriptor, (byte[]) tagValue);
                  } catch (IOException e) {
                     throw new RuntimeException(e);
                  }
            }
         }

         @Override
         public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
            log.debugf("onStartNested %s", fieldDescriptor.getName());
         }

         @Override
         public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
            log.debugf("onEndNested %s", fieldDescriptor.getName());
         }

         @Override
         public void onEnd() {
            log.debug("onEnd");
         }
      };

      Descriptor wrapperDescriptor = ctx.getMessageDescriptor(WrappedMessage.PROTOBUF_TYPE_NAME);

      ProtobufParser.INSTANCE.parse(tagHandler, wrapperDescriptor, userBytes);
   }

   @Test
   public void testPseudoJSON() throws Exception {
      SerializationContext ctx = createContext();

      User user = new User();
      user.setId(1);
      user.setName("John");
      user.setSurname("Batman");
      user.setGender(User.Gender.MALE);
      user.setAccountIds(new HashSet<>(Arrays.asList(1, 3)));
      user.setAddresses(Arrays.asList(new Address("Old Street", "XYZ42", -12), new Address("Bond Street", "W23", 2)));

      byte[] userBytes = ProtobufUtil.toWrappedByteArray(ctx, user);

      System.out.println("Canonical JSON out:\n" + toCanonicalJSON(ctx, userBytes));

      byte[] piBytes = ProtobufUtil.toWrappedByteArray(ctx, 3.14d);
      System.out.println("Canonical JSON out:\n" + toCanonicalJSON(ctx, piBytes));
   }

   private String toCanonicalJSON(SerializationContext ctx, byte[] bytes) throws IOException {
      final StringBuilder jsonOut = new StringBuilder();

      TagHandler messageHandler = new TagHandler() {

         final ArrayDeque<Boolean> nestingLevels = new ArrayDeque<>();

         private void indent() {
            for (int i = 0; i < nestingLevels.size(); i++) {
               jsonOut.append("  ");
            }
         }

         @Override
         public void onStart() {
            indent();
            jsonOut.append('{');
            nestingLevels.add(true);
         }

         @Override
         public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
            if (fieldDescriptor == null) {
               // unknown field, ignore
               return;
            }

            if (nestingLevels.peekLast()) {
               nestingLevels.removeLast();
               nestingLevels.add(false);
               jsonOut.append("\n");
            } else {
               jsonOut.append(",\n");
            }
            indent();

            //TODO repeated fields should be rendered as json arrays. to do that we need to emit a [
            // first time we see a FieldDescriptor that has Label.REPEATED and start tracking following onTag invocations
            // until we encounter a different field id and then emit a ] just before handling it

            jsonOut.append('"').append(fieldDescriptor.getName()).append("\": ");
            if (fieldDescriptor.getType() == Type.STRING) {
               jsonOut.append("\"").append(tagValue).append("\"");
            } else if (fieldDescriptor.getType() == Type.ENUM) {
               String enumValName = fieldDescriptor.getEnumType().findValueByNumber((Integer) tagValue).getName();
               jsonOut.append("\"").append(enumValName).append("\"");
            } else {
               jsonOut.append(tagValue);
            }
         }

         @Override
         public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
            if (nestingLevels.peekLast()) {
               nestingLevels.removeLast();
               nestingLevels.add(false);
               jsonOut.append("\n");
            } else {
               jsonOut.append(",\n");
            }
            indent();
            jsonOut.append('"').append(fieldDescriptor.getName()).append("\": {");
            nestingLevels.add(true);
         }

         @Override
         public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
            jsonOut.append("\n");
            nestingLevels.removeLast();
            indent();
            jsonOut.append('}');
         }

         @Override
         public void onEnd() {
            jsonOut.append("\n}\n");
            nestingLevels.clear();
         }
      };

      TagHandler wrapperHandler = new TagHandler() {

         private Descriptor descriptor;

         @Override
         public void onStart() {
            descriptor = null;
         }

         @Override
         public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
            if (fieldDescriptor == null) {
               // ignore unknown fields
               return;
            }
            switch (fieldNumber) {
               case WrappedMessage.WRAPPED_DESCRIPTOR_ID:
                  String typeName = ctx.getTypeNameById((Integer) tagValue);
                  descriptor = ctx.getMessageDescriptor(typeName);
                  break;
               case WrappedMessage.WRAPPED_DESCRIPTOR_FULL_NAME:
                  descriptor = ctx.getMessageDescriptor((String) tagValue);
                  break;
               case WrappedMessage.WRAPPED_MESSAGE_BYTES:
                  try {
                     // todo here we expect WRAPPED_DESCRIPTOR_FULL_NAME or WRAPPED_DESCRIPTOR_ID was already read, which might not be the case
                     ProtobufParser.INSTANCE.parse(messageHandler, descriptor, (byte[]) tagValue);
                  } catch (IOException e) {
                     throw new RuntimeException(e);
                  }
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
               case WrappedMessage.WRAPPED_ENUM:
                  messageHandler.onStart();
                  messageHandler.onTag(fieldNumber, fieldDescriptor, tagValue);
                  messageHandler.onEnd();
            }
         }
      };

      Descriptor wrapperDescriptor = ctx.getMessageDescriptor(WrappedMessage.PROTOBUF_TYPE_NAME);
      ProtobufParser.INSTANCE.parse(wrapperHandler, wrapperDescriptor, bytes);
      return jsonOut.toString();
   }
}
