package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XField;
import org.infinispan.protostream.annotations.impl.types.XMethod;
import org.infinispan.protostream.descriptors.Type;

public class ProtoMapMetadata extends ProtoFieldMetadata {
   private final ProtoFieldMetadata key;
   private final ProtoFieldMetadata value;

   ProtoMapMetadata(int number, String name, XClass keyJavaType, XClass valueJavaType, XClass mapImplementation, Type keyType, Type valueType, ProtoTypeMetadata protoTypeMetadata, XField field) {
      super(number, name, null, null, mapImplementation, Type.MAP, null, false, true, false, null, field);
      key = new ProtoFieldMetadata(1, "key", null, keyJavaType, null, keyType, protoTypeMetadata, false, false, false, null, field);
      value = new ProtoFieldMetadata(2, "value", null, valueJavaType, null, valueType, protoTypeMetadata, false, false, false, null, field);
   }

   ProtoMapMetadata(int number, String name, XClass keyJavaType, XClass keyValueType, XClass mapImplementation, Type keyType, Type valueType, ProtoTypeMetadata protoTypeMetadata, String propertyName, XMethod definingMethod, XMethod getter, XMethod setter) {
      super(number, name, null, null, mapImplementation, Type.MAP, null, false, true, false, null, propertyName, definingMethod, getter, setter);
      key = new ProtoFieldMetadata(1, "key", null, keyJavaType, null, keyType, protoTypeMetadata, false, false, false, null, null, definingMethod, null, null);
      value = new ProtoFieldMetadata(2, "value", null, keyValueType, null, valueType, protoTypeMetadata, false, false, false, null, null, definingMethod, null, null);
   }

   @Override
   public boolean isMap() {
      return true;
   }

   public ProtoFieldMetadata getKey() {
      return key;
   }

   public ProtoFieldMetadata getValue() {
      return value;
   }

   @Override
   public void generateProto(IndentWriter iw, ProtoSyntax syntax) {
      iw.println();
      ProtoTypeMetadata.appendDocumentation(iw, getDocumentation());
      iw.printf("map<%s, %s> %s = %d;\n", key.getTypeName(), value.getTypeName(), getName(), getNumber());
   }
}
