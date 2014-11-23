package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
class ProtoTypeMetadata {

   protected final String name;

   protected final Class<?> javaClass;

   protected BaseMarshaller marshaller;

   protected ProtoMessageTypeMetadata outerType;

   public ProtoTypeMetadata(BaseMarshaller marshaller) {
      this(null, marshaller, marshaller.getTypeName(), marshaller.getJavaClass());
   }

   protected ProtoTypeMetadata(ProtoMessageTypeMetadata outerType, BaseMarshaller marshaller, String name, Class<?> javaClass) {
      this.outerType = outerType;
      this.marshaller = marshaller;
      this.name = name;
      this.javaClass = javaClass;
   }

   public String getName() {
      return name;
   }

   public String getFullName() {
      StringBuilder sb = new StringBuilder();
      ProtoMessageTypeMetadata t = outerType;
      while (t != null) {
         sb.append(t.getName()).append('.');
         t = t.getOuterType();
      }
      sb.append(name);
      return sb.toString();
   }

   public Class<?> getJavaClass() {
      return javaClass;
   }

   public BaseMarshaller getMarshaller() {
      return marshaller;
   }

   public void setMarshaller(BaseMarshaller marshaller) {
      this.marshaller = marshaller;
   }

   public boolean isEnum() {
      return marshaller instanceof EnumMarshaller;
   }

   public ProtoMessageTypeMetadata getOuterType() {
      return outerType;
   }

   public void setOuterType(ProtoMessageTypeMetadata outerType) {
      this.outerType = outerType;
   }

   public boolean isTopLevel() {
      return outerType == null;
   }

   public void generateProto(IndentWriter iw) {
   }
}
