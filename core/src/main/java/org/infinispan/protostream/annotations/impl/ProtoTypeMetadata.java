package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.annotations.impl.types.XClass;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public abstract class ProtoTypeMetadata implements HasProtoSchema {

   protected final String name;

   protected final XClass javaClass;

   protected ProtoMessageTypeMetadata outerType;

   protected ProtoTypeMetadata(String name, XClass javaClass) {
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
         t = t.outerType;
      }
      sb.append(name);
      return sb.toString();
   }

   public String getDocumentation() {
      return javaClass.getDocumentation();
   }

   public XClass getJavaClass() {
      return javaClass;
   }

   public String getJavaClassName() {
      String canonicalName = javaClass.getCanonicalName();
      return canonicalName != null ? canonicalName : javaClass.getName();
   }

   /**
    * Indicates if this type comes from the currently processed/generated schema of from an external schema.
    */
   public boolean isImported() {
      return false;
   }

   /**
    * The schema file where this type comes from. Must be non-null for all imported types, can be null for others.
    */
   public String getFileName() {
      return null;
   }

   public abstract boolean isEnum();

   public abstract ProtoEnumValueMetadata getEnumMemberByName(String name);

   public final ProtoMessageTypeMetadata getOuterType() {
      return outerType;
   }

   protected final void setOuterType(ProtoMessageTypeMetadata outerType) {
      this.outerType = outerType;
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
