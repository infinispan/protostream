package org.infinispan.protostream.annotations.impl;

import java.lang.reflect.Field;

import org.infinispan.protostream.annotations.ProtoSchemaBuilder;

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

   ProtoEnumValueMetadata(int number, String protoName, Field enumField, Enum enumValue) {
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
      iw.append("\n\n");
      ProtoTypeMetadata.appendDocumentation(iw, documentation);
      iw.append("   ").append(protoName).append(" = ").append(String.valueOf(number));
      if (ProtoSchemaBuilder.generateSchemaDebugComments) {
         iw.append(" /* field = ").append(enumField.getName()).append(" */");
      }
      iw.append(";\n");
   }
}
