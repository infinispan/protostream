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
import org.infinispan.protostream.containers.ElementContainer;
import org.infinispan.protostream.containers.ElementContainerAdapter;
import org.infinispan.protostream.impl.Log;

/**
 * A {@link ProtoTypeMetadata} for an enum type created based on annotations.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class ProtoEnumTypeMetadata extends ProtoTypeMetadata {

   private static final Log log = Log.LogFactory.getLog(ProtoEnumTypeMetadata.class);

   private final XClass annotatedEnumClass;

   private final boolean isAdapter;

   private SortedMap<Integer, ProtoEnumValueMetadata> membersByNumber;

   private Map<String, ProtoEnumValueMetadata> membersByName;

   public ProtoEnumTypeMetadata(XClass annotatedEnumClass, XClass enumClass) {
      super(getProtoName(annotatedEnumClass, enumClass), enumClass);
      this.annotatedEnumClass = annotatedEnumClass;
      this.isAdapter = annotatedEnumClass != enumClass;

      // Enums cannot be element containers!
      if (isAdapter) {
         if (annotatedEnumClass.isAssignableTo(ElementContainerAdapter.class)) {
            throw new ProtoSchemaBuilderException("ElementContainerAdapter interface should not be implemented by annotated adapters for enums: " + annotatedEnumClass.getName());
         }
      } else {
         if (annotatedEnumClass.isAssignableTo(ElementContainer.class)) {
            throw new ProtoSchemaBuilderException("ElementContainer interface should not be implemented by annotated enums: " + annotatedEnumClass.getName());
         }
      }

      validateName();
   }

   private static String getProtoName(XClass annotatedEnumClass, XClass enumClass) {
      ProtoName annotation = annotatedEnumClass.getAnnotation(ProtoName.class);
      ProtoEnum protoEnumAnnotation = annotatedEnumClass.getAnnotation(ProtoEnum.class);
      if (annotation != null) {
         if (protoEnumAnnotation != null) {
            throw new ProtoSchemaBuilderException("@ProtoEnum annotation cannot be used together with @ProtoName: " + annotatedEnumClass.getName());
         }
         return annotation.value().isEmpty() ? enumClass.getSimpleName() : annotation.value();
      }
      return protoEnumAnnotation == null || protoEnumAnnotation.name().isEmpty() ? enumClass.getSimpleName() : protoEnumAnnotation.name();
   }

   @Override
   public XClass getAnnotatedClass() {
      return annotatedEnumClass;
   }

   @Override
   public boolean isAdapter() {
      return isAdapter;
   }

   @Override
   public void scanMemberAnnotations() {
      if (membersByNumber == null) {
         membersByNumber = new TreeMap<>();
         membersByName = new HashMap<>();
         for (XEnumConstant ec : annotatedEnumClass.getEnumConstants()) {
            ProtoEnumValue annotation = ec.getAnnotation(ProtoEnumValue.class);
            if (annotation == null) {
               throw new ProtoSchemaBuilderException("Enum constants must have the @ProtoEnumValue annotation: " + getAnnotatedClassName() + '.' + ec.getName());
            }
            int number = getNumber(annotation, ec);
            if (membersByNumber.containsKey(number)) {
               throw new ProtoSchemaBuilderException("Found duplicate definition of Protobuf enum tag " + number + " on enum constant: " + getAnnotatedClassName() + '.' + ec.getName()
                     + " clashes with " + membersByNumber.get(number).getJavaEnumName());
            }
            String name = annotation.name();
            if (name.isEmpty()) {
               name = ec.getName();
            }
            if (membersByName.containsKey(name)) {
               throw new ProtoSchemaBuilderException("Found duplicate definition of Protobuf enum constant " + name + " on enum constant: " + getAnnotatedClassName() + '.' + ec.getName()
                     + " clashes with " + membersByName.get(name).getJavaEnumName());
            }
            ProtoEnumValueMetadata pevm = new ProtoEnumValueMetadata(number, name,
                  ec.getOrdinal(), getJavaClassName() + '.' + ec.getName(), ec.getDocumentation());
            membersByNumber.put(number, pevm);
            membersByName.put(pevm.getProtoName(), pevm);

            if (isAdapter()) {
               XEnumConstant enumConstant = javaClass.getEnumConstant(ec.getName());
               if (enumConstant == null) {
                  throw new ProtoSchemaBuilderException(getAnnotatedClassName() + '.' + ec.getName() + " does not have a corresponding enum value in " + getJavaClassName());
               }
            }
         }
         if (isAdapter()) {
            for (XEnumConstant ec : javaClass.getEnumConstants()) {
               XEnumConstant enumConstant = annotatedEnumClass.getEnumConstant(ec.getName());
               if (enumConstant == null) {
                  throw new ProtoSchemaBuilderException(getAnnotatedClassName() + " does not have a corresponding enum value for " + getJavaClassName() + '.' + ec.getName());
               }
            }
         }
         if (membersByNumber.isEmpty()) {
            throw new ProtoSchemaBuilderException("Enums must contain at least one value: " + getAnnotatedClassName());
         }
      }
   }

   private int getNumber(ProtoEnumValue annotation, XEnumConstant ec) {
      int number = annotation.number();
      if (number == 0) {
         number = annotation.value();
      } else if (annotation.value() != 0) {
         throw new ProtoSchemaBuilderException("@ProtoEnumValue.number() and value() are mutually exclusive: " + getAnnotatedClassName() + '.' + ec.getName());
      }
      return number;
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
      reserved.scan(annotatedEnumClass);

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
            ", annotatedEnumClass=" + annotatedEnumClass +
            ", isAdapter=" + isAdapter +
            ", membersByNumber=" + membersByNumber +
            '}';
   }
}
