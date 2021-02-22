package org.infinispan.protostream.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.ProtoStreamMarshaller;
import org.infinispan.protostream.TagReader;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class EnumMarshallerDelegate<T extends Enum<T>> extends BaseMarshallerDelegate<T> {

   private final EnumMarshaller<T> enumMarshaller;

   private final Set<Integer> definedValues;

   EnumMarshallerDelegate(EnumMarshaller<T> enumMarshaller, EnumDescriptor enumDescriptor) {
      this.enumMarshaller = enumMarshaller;
      definedValues = new HashSet<>();
      for (EnumValueDescriptor evd : enumDescriptor.getValues()) {
         definedValues.add(evd.getNumber());
      }
   }

   @Override
   public EnumMarshaller<T> getMarshaller() {
      return enumMarshaller;
   }

   @Override
   public void marshall(ProtoStreamMarshaller.WriteContext ctx, FieldDescriptor fd, T value) throws IOException {
      encode(fd.getNumber(), value, ctx.getOut());
   }

   public void encode(int fieldNumber, T value, TagWriter out) throws IOException {
      int enumValue = enumMarshaller.encode(value);
      if (!definedValues.contains(enumValue)) {
         throw new IllegalStateException("Undefined enum value " + enumValue + " for " + enumMarshaller.getTypeName());
      }
      out.writeEnum(fieldNumber, enumValue);
   }

   @Override
   public T unmarshall(ProtoStreamMarshaller.ReadContext ctx, FieldDescriptor fd) throws IOException {
      final int expectedTag = fd.getWireTag();
      int enumValue;
      ProtoStreamReaderImpl reader = (ProtoStreamReaderImpl) ctx.getReader();
      UnknownFieldSet unknownFieldSet = reader.getUnknownFieldSet();
      Object value = unknownFieldSet.consumeTag(expectedTag);
      if (value != null) {
         enumValue = ((Long) value).intValue();
      } else {
         TagReader in = ctx.getIn();
         while (true) {
            int tag = in.readTag();
            if (tag == 0) {
               return null;
            }
            if (tag == expectedTag) {
               enumValue = in.readEnum();
               break;
            }
            unknownFieldSet.readSingleField(tag, in);
         }
      }

      return decode(expectedTag, enumValue, unknownFieldSet);
   }

   public T decode(int expectedTag, int enumValue, UnknownFieldSet unknownFieldSet) {
      T decoded = enumMarshaller.decode(enumValue);

      if (decoded == null && unknownFieldSet != null) {
         // the enum value was not recognized by the EnumMarshaller so rather than discarding it we add it to the unknown
         unknownFieldSet.putVarintField(expectedTag, enumValue);
      }

      return decoded;
   }
}
