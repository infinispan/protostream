package org.infinispan.protostream.annotations.impl;

import java.lang.reflect.Field;

import org.infinispan.protostream.annotations.ProtoSchemaBuilder;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoEnumValueMetadata implements HasProtoSchema {

   private final int number;

   private final String protoName;

   private final Enum enumValue;

   private final String documentation;

   ProtoEnumValueMetadata(int number, String protoName, Field enumField, Enum enumValue) {
      this.number = number;
      this.protoName = protoName;
      this.enumValue = enumValue;
      documentation = DocumentationExtractor.getDocumentation(enumField);
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

   public String getDocumentation() {
      return documentation;
   }

   @Override
   public void generateProto(IndentWriter iw) {
      iw.append("\n");
      ProtoTypeMetadata.appendDocumentation(iw, documentation);
      iw.append(protoName).append(" = ").append(String.valueOf(number));
      if (ProtoSchemaBuilder.generateSchemaDebugComments) {
         iw.append(" /* ").append(enumValue.getClass().getCanonicalName()).append('.').append(enumValue.name()).append(" */");
      }
      iw.append(";\n");
   }
}
