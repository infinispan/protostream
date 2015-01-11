package org.infinispan.protostream.annotations.impl;

import java.lang.reflect.Field;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoEnumValueMetadata {

   private final int number;

   private final String protoName;

   private final Field enumField;

   private final Enum enumValue;

   private final String documentation;

   public ProtoEnumValueMetadata(int number, String protoName, Field enumField, Enum enumValue) {
      this.number = number;
      this.protoName = protoName;
      this.enumField = enumField;
      this.enumValue = enumValue;
      documentation = DocumentationExtractor.getDocumentation(enumField);
   }

   public int getNumber() {
      return number;
   }

   public String getProtoName() {
      return protoName;
   }

   public Field getEnumField() {
      return enumField;
   }

   public Enum getEnumValue() {
      return enumValue;
   }

   public String getDocumentation() {
      return documentation;
   }

   public void generateProto(IndentWriter iw) {
      iw.append('\n');
      if (documentation != null) {
         iw.append("/*\n");
         iw.append(documentation).append('\n');
         iw.append("*/\n");
      }
      iw.append("   ").append(protoName).append(" = ").append(String.valueOf(number)).append(" /* field = ").append(enumField.getName()).append(" */;\n");
   }
}
