package org.infinispan.protostream.annotations.impl;

import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class ProtoEnumTypeMetadata extends ProtoTypeMetadata {

   private final Map<Integer, ProtoEnumValueMetadata> members;

   public ProtoEnumTypeMetadata(ProtoMessageTypeMetadata outerType, Class<? extends Enum> enumClass, String name, Map<Integer, ProtoEnumValueMetadata> members) {
      super(outerType, null, name, enumClass);
      this.members = members;
   }

   public Map<Integer, ProtoEnumValueMetadata> getMembers() {
      return members;
   }

   @Override
   public void generateProto(IndentWriter iw) {
      iw.append("\nenum ").append(name).append(" {\n");
      for (ProtoEnumValueMetadata m : members.values()) {
         iw.append("   ").append(m.getProtoName()).append(" = ").append(String.valueOf(m.getNumber())).append(";\n");
      }
      iw.append("}\n");
   }

   @Override
   public boolean isEnum() {
      return true;
   }
}
