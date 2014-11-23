package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.annotations.ProtoEnum;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.impl.Log;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoEnumMetadataScanner {

   private static final Log log = Log.LogFactory.getLog(ProtoEnumMetadataScanner.class);

   private final Class<? extends Enum> cls;

   private final ProtoEnumTypeMetadata protoEnumMetadata;

   public ProtoEnumMetadataScanner(Class<? extends Enum> cls) {
      this.cls = cls;

      ProtoEnum annotation = cls.getAnnotation(ProtoEnum.class);
      String name = annotation == null || annotation.name().isEmpty() ? cls.getSimpleName() : annotation.name();

      Map<Integer, ProtoEnumValueMetadata> members = discoverMembers();
      if (members.isEmpty()) {
         throw new ProtoSchemaBuilderException("Enum " + cls.getCanonicalName() + " does not have any members");
      }
      protoEnumMetadata = new ProtoEnumTypeMetadata(null, cls, name, members);
   }

   private Map<Integer, ProtoEnumValueMetadata> discoverMembers() {
      Map<Integer, ProtoEnumValueMetadata> members = new HashMap<Integer, ProtoEnumValueMetadata>();
      for (Field f : cls.getDeclaredFields()) {
         if (f.isEnumConstant()) {
            ProtoEnumValue annotation = f.getAnnotation(ProtoEnumValue.class);
            if (annotation == null) {
               throw new ProtoSchemaBuilderException("Enum member should have the @ProtoEnumValue annotation: " + cls.getName() + "." + f.getName());
            }
            if (members.containsKey(annotation.number())) {
               throw new ProtoSchemaBuilderException("Duplicate definition of Protobuf enum tag " + annotation.number() + " on annotation member: " + cls.getName() + "." + f.getName());
            }
            String name = annotation.name();
            if (name.isEmpty()) {
               name = f.getName();
            }
            Enum e = null;
            try {
               e = (Enum) f.get(cls);
            } catch (IllegalAccessException e1) {
               // not really possible
            }
            members.put(annotation.number(), new ProtoEnumValueMetadata(annotation.number(), name, e));
         }
      }
      return members;
   }

   public ProtoEnumTypeMetadata getProtoEnumMetadata() {
      return protoEnumMetadata;
   }
}


