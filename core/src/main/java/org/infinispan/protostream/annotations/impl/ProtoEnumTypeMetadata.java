package org.infinispan.protostream.annotations.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.infinispan.protostream.annotations.ProtoEnum;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XEnumConstant;
import org.infinispan.protostream.impl.Log;

/**
 * A {@link ProtoTypeMetadata} for an enum type created based on annotations during the current execution of {@link
 * org.infinispan.protostream.annotations.ProtoSchemaBuilder}.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class ProtoEnumTypeMetadata extends ProtoTypeMetadata {

   private static final Log log = Log.LogFactory.getLog(ProtoEnumTypeMetadata.class);

   private SortedMap<Integer, ProtoEnumValueMetadata> membersByNumber;

   private Map<String, ProtoEnumValueMetadata> membersByName;

   ProtoEnumTypeMetadata(XClass enumClass) {
      super(getProtoName(enumClass), enumClass);
   }

   private static String getProtoName(XClass enumClass) {
      ProtoName annotation = enumClass.getAnnotation(ProtoName.class);
      ProtoEnum protoEnumAnnotation = enumClass.getAnnotation(ProtoEnum.class);
      if (annotation != null) {
         if (protoEnumAnnotation != null) {
            throw new ProtoSchemaBuilderException("@ProtoEnum annotation cannot be used together with @ProtoName: " + enumClass.getName());
         }
         return annotation.value().isEmpty() ? enumClass.getSimpleName() : annotation.value();
      }
      return protoEnumAnnotation == null || protoEnumAnnotation.name().isEmpty() ? enumClass.getSimpleName() : protoEnumAnnotation.name();
   }

   @Override
   public void scanMemberAnnotations() {
      if (membersByNumber == null) {
         membersByNumber = new TreeMap<>();
         membersByName = new HashMap<>();
         for (XEnumConstant ec : javaClass.getEnumConstants()) {
            ProtoEnumValue annotation = ec.getAnnotation(ProtoEnumValue.class);
            if (annotation == null) {
               throw new ProtoSchemaBuilderException("Enum constants must have the @ProtoEnumValue annotation: " + getJavaClassName() + '.' + ec.getName());
            }
            if (membersByNumber.containsKey(annotation.number())) {
               throw new ProtoSchemaBuilderException("Found duplicate definition of Protobuf enum tag " + annotation.number() + " on enum constant: " + getJavaClassName() + '.' + ec.getName());
            }
            String name = annotation.name();
            if (name.isEmpty()) {
               name = ec.getName();
            }
            if (membersByName.containsKey(name)) {
               throw new ProtoSchemaBuilderException("Found duplicate definition of Protobuf enum constant " + name + " on enum constant: " + getJavaClassName() + '.' + ec.getName());
            }
            ProtoEnumValueMetadata pevm = new ProtoEnumValueMetadata(annotation.number(), name,
                  ec.getOrdinal(), ec.getDeclaringClass().getName() + '.' + ec.getName(), ec.getProtoDocs());
            membersByNumber.put(annotation.number(), pevm);
            membersByName.put(pevm.getProtoName(), pevm);
         }
         if (membersByNumber.isEmpty()) {
            throw new ProtoSchemaBuilderException("Enums without members are not allowed: " + getJavaClassName());
         }
      }
   }

   public SortedMap<Integer, ProtoEnumValueMetadata> getMembers() {
      scanMemberAnnotations();
      return membersByNumber;
   }

   @Override
   public boolean isEnum() {
      return true;
   }

   @Override
   public ProtoEnumValueMetadata getEnumMemberByName(String name) {
      scanMemberAnnotations();
      return membersByName.get(name);
   }

   @Override
   public void generateProto(IndentWriter iw) {
      scanMemberAnnotations();

      iw.append("\n\n");
      appendDocumentation(iw, getDocumentation());
      iw.append("enum ").append(name);
      if (BaseProtoSchemaGenerator.generateSchemaDebugComments) {
         iw.append(" /* ").append(getJavaClassName()).append(" */");
      }
      iw.append(" {\n");
      iw.inc();

      ReservedProcessor reserved = new ReservedProcessor();
      reserved.scan(javaClass);

      for (String memberName : membersByName.keySet()) {
         XClass where = reserved.checkReserved(name);
         if (where != null) {
            throw new ProtoSchemaBuilderException("Protobuf enum value " + memberName + " of enum constant " +
                  membersByName.get(memberName).getJavaEnumName() + " conflicts with 'reserved' statement in " + where.getCanonicalName());
         }
      }

      for (int memberNumber : membersByNumber.keySet()) {
         XClass where = reserved.checkReserved(memberNumber);
         if (where != null) {
            throw new ProtoSchemaBuilderException("Protobuf enum number " + memberNumber + " of enum constant " +
                  membersByNumber.get(memberNumber).getJavaEnumName() + " conflicts with 'reserved' statement in " + where.getCanonicalName());
         }
      }

      reserved.generate(iw);

      for (ProtoEnumValueMetadata m : membersByNumber.values()) {
         m.generateProto(iw);
      }

      iw.dec();
      iw.append("}\n");
   }

   @Override
   public String toString() {
      return "ProtoEnumTypeMetadata{" +
            "name='" + name + '\'' +
            ", javaClass=" + javaClass +
            ", membersByNumber=" + membersByNumber +
            '}';
   }
}
