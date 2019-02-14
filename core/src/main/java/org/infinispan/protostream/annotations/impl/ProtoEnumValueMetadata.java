package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.annotations.ProtoSchemaBuilder;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoEnumValueMetadata implements HasProtoSchema {

   private final int number;

   private final String protoName;

   private final int javaEnumOrdinal;

   private final String javaEnumName;

   private final String documentation;

   ProtoEnumValueMetadata(int number, String protoName, Enum enumValue, String documentation) {
      this.number = number;
      this.protoName = protoName;
      this.javaEnumOrdinal = enumValue.ordinal();
      this.javaEnumName = enumValue.getDeclaringClass().getName() + '.' + enumValue.name();
      this.documentation = documentation;
   }

   /**
    * Returns the Protobuf number associated to this enum value.
    */
   public int getNumber() {
      return number;
   }

   /**
    * Returns the Protobuf name of this enum value.
    */
   public String getProtoName() {
      return protoName;
   }

   /**
    * Returns the ordinal of the Java enum constant.
    */
   public int getJavaEnumOrdinal() {
      return javaEnumOrdinal;
   }

   /**
    * Returns the FQN of the Java enum constant.
    */
   public String getJavaEnumName() {
      return javaEnumName;
   }

   /**
    * Returns the documentation attached to this enum.
    */
   public String getDocumentation() {
      return documentation;
   }

   @Override
   public void generateProto(IndentWriter iw) {
      iw.append("\n");
      ProtoTypeMetadata.appendDocumentation(iw, documentation);
      iw.append(protoName).append(" = ").append(String.valueOf(number));
      if (ProtoSchemaBuilder.generateSchemaDebugComments) {
         iw.append(" /* ").append(javaEnumName).append(" */");
      }
      iw.append(";\n");
   }
}
