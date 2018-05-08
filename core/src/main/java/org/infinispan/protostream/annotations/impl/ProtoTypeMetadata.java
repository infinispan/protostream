package org.infinispan.protostream.annotations.impl;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
abstract class ProtoTypeMetadata {

   protected final String name;

   protected final Class<?> javaClass;

   protected final String documentation;

   protected ProtoMessageTypeMetadata outerType;

   protected ProtoTypeMetadata(String name, Class<?> javaClass, String documentation) {
      this.name = name;
      this.javaClass = javaClass;
      this.documentation = documentation;
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

   public abstract boolean isEnum();

   public abstract ProtoEnumValueMetadata getEnumMemberByName(String name);

   public final ProtoMessageTypeMetadata getOuterType() {
      return outerType;
   }

   protected final void setOuterType(ProtoMessageTypeMetadata outerType) {
      this.outerType = outerType;
   }

   public final boolean isTopLevel() {
      return outerType == null;
   }

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
