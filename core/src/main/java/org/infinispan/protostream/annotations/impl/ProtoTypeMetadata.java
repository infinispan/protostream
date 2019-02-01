package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
class ProtoTypeMetadata implements HasProtoSchema {

   protected final String name;

   protected final Class<?> javaClass;

   protected final String documentation;

   protected GenericDescriptor descriptor;

   protected BaseMarshaller marshaller;

   protected ProtoMessageTypeMetadata outerType;

   /**
    * Constructor for a type that is already marshallable by SerializationContext. No schema or code is generated for it.
    */
   ProtoTypeMetadata(GenericDescriptor descriptor, BaseMarshaller<?> marshaller) {
      this.descriptor = descriptor;
      this.marshaller = marshaller;
      this.name = marshaller.getTypeName();
      this.javaClass = marshaller.getJavaClass();
      this.documentation = null;
   }

   /**
    * Constructor to be used by derived classes, not for direct instantiation.
    */
   protected ProtoTypeMetadata(String name, Class<?> javaClass) {
      this.name = name;
      this.javaClass = javaClass;
      this.documentation = DocumentationExtractor.getDocumentation(javaClass);
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

   public String getDocumentation() {
      return documentation;
   }

   public Class<?> getJavaClass() {
      return javaClass;
   }

   public String getJavaClassName() {
      String canonicalName = javaClass.getCanonicalName();
      return canonicalName != null ? canonicalName : javaClass.getName();
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

   public ProtoEnumValueMetadata getEnumMemberByName(String name) {
      if (!isEnum()) {
         throw new IllegalStateException(getFullName() + " is not an enum");
      }
      EnumValueDescriptor evd = ((EnumDescriptor) descriptor).findValueByName(name);
      if (evd == null) {
         return null;
      }
      Enum ev = ((EnumMarshaller) marshaller).decode(evd.getNumber());
      return new ProtoEnumValueMetadata(evd.getNumber(), name, ev, null);
   }

   public ProtoMessageTypeMetadata getOuterType() {
      return outerType;
   }

   protected void setOuterType(ProtoMessageTypeMetadata outerType) {
      this.outerType = outerType;
   }

   public boolean isTopLevel() {
      return outerType == null;
   }

   @Override
   public void generateProto(IndentWriter iw) {
      // subclasses must override this
   }

   public void scanMemberAnnotations() {
      // subclasses must override this
   }

   protected static void appendDocumentation(IndentWriter iw, String documentation) {
      if (documentation != null) {
         iw.append("/**\n");
         for (String s : documentation.split("\\r\\n|\\n|\\r")) {
            iw.append(" * ").append(s).append('\n');
         }
         iw.append(" */\n");
      }
   }
}
