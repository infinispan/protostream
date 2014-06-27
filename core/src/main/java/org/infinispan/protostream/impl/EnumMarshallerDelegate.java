package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.UnknownFieldSet;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class EnumMarshallerDelegate<T extends Enum<T>> implements BaseMarshallerDelegate<T> {

   private final EnumMarshaller<T> enumMarshaller;

   private final EnumDescriptor enumDescriptor;

   private final Set<Integer> definedValues;

   public EnumMarshallerDelegate(EnumMarshaller<T> enumMarshaller, EnumDescriptor enumDescriptor) {
      this.enumMarshaller = enumMarshaller;
      this.enumDescriptor = enumDescriptor;
      List<EnumValueDescriptor> enumValues = enumDescriptor.getValues();
      definedValues = new HashSet<Integer>(enumValues.size());
      for (EnumValueDescriptor evd : enumValues) {
         definedValues.add(evd.getNumber());
      }
   }

   @Override
   public EnumMarshaller<T> getMarshaller() {
      return enumMarshaller;
   }

   public EnumDescriptor getEnumDescriptor() {
      return enumDescriptor;
   }

   public Set<Integer> getDefinedValues() {
      return definedValues;
   }

   @Override
   public void marshall(String fieldName, FieldDescriptor fd, T value, ProtoStreamWriterImpl writer, CodedOutputStream out) throws IOException {
      int enumValue = enumMarshaller.encode(value);

      if (!definedValues.contains(enumValue)) {
         throw new IllegalArgumentException("Undefined enum value : " + enumValue);
      }

      out.writeEnum(fd.getNumber(), enumValue);
   }

   @Override
   public T unmarshall(String fieldName, FieldDescriptor fieldDescriptor, ProtoStreamReaderImpl reader, CodedInputStream in) throws IOException {
      final int expectedTag = WireFormat.makeTag(fieldDescriptor.getNumber(), WireFormat.WIRETYPE_VARINT);
      int enumValue;
      UnknownFieldSet unknownFieldSet = reader.getUnknownFieldSet();
      Object o = unknownFieldSet.consumeTag(expectedTag);
      if (o != null) {
         enumValue = ((Long) o).intValue();
      } else {
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

      T decoded = enumMarshaller.decode(enumValue);

      if (decoded == null) {
         // the enum value was not recognized by the decoder so rather than discarding it we add it to the unknown
         unknownFieldSet.putVarintField(expectedTag, enumValue);
      }

      return decoded;
   }
}

