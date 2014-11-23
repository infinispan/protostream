package org.infinispan.protostream.annotations.impl;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoEnumValueMetadata {

   private final int number;

   private final String protoName;

   private final Enum enumValue;

   public ProtoEnumValueMetadata(int number, String protoName, Enum enumValue) {
      this.number = number;
      this.protoName = protoName;
      this.enumValue = enumValue;
   }

   public int getNumber() {
      return number;
   }

   public String getProtoName() {
      return protoName;
   }

   public Enum getEnumValue() {
      return enumValue;
   }
}
