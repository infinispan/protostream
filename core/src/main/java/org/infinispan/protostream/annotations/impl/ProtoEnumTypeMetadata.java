package org.infinispan.protostream.annotations.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.infinispan.protostream.annotations.ProtoEnum;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.impl.Log;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoEnumTypeMetadata extends ProtoTypeMetadata {

   private static final Log log = Log.LogFactory.getLog(ProtoEnumTypeMetadata.class);

   private Map<Integer, ProtoEnumValueMetadata> membersByNumber;

   private Map<String, ProtoEnumValueMetadata> membersByName;

   ProtoEnumTypeMetadata(Class<? extends Enum> enumClass) {
      super(getProtoName(enumClass), enumClass);
   }

   private static String getProtoName(Class<? extends Enum> enumClass) {
      ProtoEnum annotation = enumClass.getAnnotation(ProtoEnum.class);
      return annotation == null || annotation.name().isEmpty() ? enumClass.getSimpleName() : annotation.name();
   }

   @Override
   public void scanMemberAnnotations() {
      if (membersByNumber == null) {
         membersByNumber = new TreeMap<>();
         for (Field f : javaClass.getDeclaredFields()) {
            if (f.isEnumConstant()) {
               ProtoEnumValue annotation = f.getAnnotation(ProtoEnumValue.class);
               if (annotation == null) {
                  throw new ProtoSchemaBuilderException("Enum members must have the @ProtoEnumValue annotation: " + javaClass.getName() + "." + f.getName());
               }
               if (membersByNumber.containsKey(annotation.number())) {
                  throw new ProtoSchemaBuilderException("Found duplicate definition of Protobuf enum tag " + annotation.number() + " on annotation member: " + javaClass.getName() + "." + f.getName());
               }
               String name = annotation.name();
               if (name.isEmpty()) {
                  name = f.getName();
               }
               Enum e = null;
               try {
                  e = (Enum) f.get(javaClass);
               } catch (IllegalAccessException iae) {
                  // not really possible
               }
               membersByNumber.put(annotation.number(), new ProtoEnumValueMetadata(annotation.number(), name, f, e));
            }
         }
         if (membersByNumber.isEmpty()) {
            throw new ProtoSchemaBuilderException("Members of enum " + javaClass.getCanonicalName() + " must be @ProtoEnum annotated");
         }
         membersByName = new HashMap<>(membersByNumber.size());
         for (ProtoEnumValueMetadata enumVal : membersByNumber.values()) {
            membersByName.put(enumVal.getProtoName(), enumVal);
         }
      }
   }

   public Map<Integer, ProtoEnumValueMetadata> getMembers() {
      scanMemberAnnotations();
      return membersByNumber;
   }

   public ProtoEnumValueMetadata getMemberByName(String name) {
      scanMemberAnnotations();
      return membersByName.get(name);
   }

   @Override
   public void generateProto(IndentWriter iw) {
      scanMemberAnnotations();

      iw.append("\n\n");
      appendDocumentation(iw, documentation);
      iw.append("enum ").append(name);
      if (ProtoSchemaBuilder.generateSchemaDebugComments) {
         iw.append(" /* ").append(javaClass.getCanonicalName()).append(" */");
      }
      iw.append(" {\n");
      for (ProtoEnumValueMetadata m : membersByNumber.values()) {
         m.generateProto(iw);
      }
      iw.append("}\n");
   }

   @Override
   public boolean isEnum() {
      return true;
   }
}
