package org.infinispan.protostream.impl;

import static org.infinispan.protostream.WrappedMessage.WRAPPED_CONTAINER_TYPE_NAME;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_ENUM;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_MESSAGE;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_TYPE_ID;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_TYPE_NAME;
import static org.infinispan.protostream.impl.JsonUtils.JSON_VALUE_FIELD;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufParser;
import org.infinispan.protostream.TagHandler;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;

final class JsonWrappedTagHandler implements TagHandler {

   private Integer typeId;
   private String typeName;
   private byte[] wrappedMessage;
   private Integer wrappedEnum;
   private String wrappedContainerType;

   private final JsonTagHandler jsonDelegate;
   private final ImmutableSerializationContext ctx;
   private final Descriptor wrapperDescriptor;
   private final FieldDescriptor fieldDescriptor;
   private final int nested;

   public JsonWrappedTagHandler(JsonTagHandler handler, ImmutableSerializationContext ctx) {
      this(handler, ctx, 0, null);
   }

   private JsonWrappedTagHandler(JsonTagHandler handler, ImmutableSerializationContext ctx, int nested, FieldDescriptor fieldDescriptor) {
      this.jsonDelegate = handler;
      this.ctx = ctx;
      this.nested = nested;
      this.wrapperDescriptor = ctx.getMessageDescriptor(WrappedMessage.PROTOBUF_TYPE_NAME);
      this.fieldDescriptor = fieldDescriptor;
   }

   private GenericDescriptor getDescriptor() {
      return typeId != null ? ctx.getDescriptorByTypeId(typeId) : ctx.getDescriptorByName(typeName);
   }

   @Override
   public void onStart(GenericDescriptor descriptor) {
      // Fresh start, we don't write anything yet.
      if (nested == 0)
         return;

      if (nested > 1 && fieldDescriptor == null) {
         jsonDelegate.writeOutput(",");
         jsonDelegate.addNextLevel(JSON_VALUE_FIELD);
      }

      jsonDelegate.onStart(descriptor);
   }

   @Override
   public void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
      jsonDelegate.onStartNested(fieldNumber, fieldDescriptor);
   }

   @Override
   public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
      if (fieldDescriptor == null) {
         // ignore unknown fields
         return;
      }
      Class<?> clazz = null;
      switch (fieldNumber) {
         case WRAPPED_TYPE_ID:
            typeId = (Integer) tagValue;
            break;
         case WRAPPED_TYPE_NAME:
            typeName = (String) tagValue;
            break;
         case WRAPPED_MESSAGE:
            wrappedMessage = (byte[]) tagValue;
            break;
         case WRAPPED_ENUM:
            wrappedEnum = (Integer) tagValue;
            break;
         case WRAPPED_CONTAINER_TYPE_NAME:
            wrappedContainerType = (String) tagValue;
            GenericDescriptor descriptorByName = ctx.getDescriptorByName(wrappedContainerType);
            jsonDelegate.onStart(descriptorByName);
            break;
         case WrappedMessage.WRAPPED_INSTANT_SECONDS:
         case WrappedMessage.WRAPPED_INSTANT_NANOS:
            searchProtoStreamAdapter(Instant.class);
            jsonDelegate.onTag(fieldNumber, fieldDescriptor, tagValue);
            break;
         case WrappedMessage.WRAPPED_DATE_MILLIS:
            searchProtoStreamAdapter(Date.class);
            jsonDelegate.onTag(fieldNumber, fieldDescriptor, tagValue);
            break;
         case WrappedMessage.WRAPPED_BYTE:
         case WrappedMessage.WRAPPED_SHORT:
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
            if (wrappedContainerType != null) {
               jsonDelegate.onTag(fieldNumber, new RepeatedFieldDescriptor(fieldDescriptor), tagValue);
            } else {
               boolean startEnd = !jsonDelegate.isAlreadyStarted();
               if (startEnd) jsonDelegate.onStart(null);
               jsonDelegate.onTag(fieldNumber, fieldDescriptor, tagValue);
               if (startEnd) jsonDelegate.onEnd();
            }
            break;
      }
   }

   private void searchProtoStreamAdapter(Class<?> clazz) {
      if (wrappedContainerType == null) {
         BaseMarshaller<?> marshaller = ctx.getMarshaller(clazz);
         if (marshaller == null)
            throw new IllegalStateException("Unable to convert instant to JSON");

         GenericDescriptor descriptor = ctx.getDescriptorByName(marshaller.getTypeName());
         wrappedContainerType = descriptor.getFullName();
         jsonDelegate.onStart(descriptor);
      }
   }

   @Override
   public void onEnd() {
      onEndInternal(false, null);
   }

   @Override
   public void onEndNested(int fieldNumber, FieldDescriptor fieldDescriptor) {
      onEndInternal(true, fieldDescriptor);
      // Complex objects don't need to call the end here. The method above has already called it.
      wrappedMessage = null;
      jsonDelegate.onEndNested(fieldNumber, fieldDescriptor);
   }

   private void onEndInternal(boolean nestedEnd, FieldDescriptor fieldDescriptor) {
      if (wrappedContainerType != null) {
         jsonDelegate.onEnd();
         return;
      }

      if (wrappedEnum != null) {
         EnumDescriptor enumDescriptor = (EnumDescriptor) getDescriptor();
         String enumConstantName = enumDescriptor.findValueByNumber(wrappedEnum).getName();
         FieldDescriptor fd = wrapperDescriptor.findFieldByNumber(WRAPPED_ENUM);
         jsonDelegate.onStart(enumDescriptor);
         jsonDelegate.onTag(WRAPPED_ENUM, fd, enumConstantName);
         jsonDelegate.onEnd();
         return;
      }

      if (wrappedMessage != null) {
         // We have a nested WrappedMessage.
         // We start parsing recursively with a fresh context and a deeper nesting level.
         try {
            Descriptor messageDescriptor = (Descriptor) getDescriptor();
            ProtobufParser.INSTANCE.parse(new JsonWrappedTagHandler(jsonDelegate.next(), ctx, nested + 1, fieldDescriptor), messageDescriptor, wrappedMessage);
            if (nested > 0 && !nestedEnd) jsonDelegate.onEnd();
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

      // Reached the end without an additional nested message.
      if (!nestedEnd)
         jsonDelegate.onEnd();
   }
}
