package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.config.Configuration;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public abstract class ProtoTypeMetadata implements HasProtoSchema {

   protected final String name;

   /**
    * The marshalled Java class.
    */
   protected final XClass javaClass;

   protected ProtoMessageTypeMetadata outerType;

   protected ProtoTypeMetadata(String name, XClass javaClass) {
      this.name = name;
      this.javaClass = javaClass;
   }

   protected void validateName() {
      if (name == null || name.isEmpty()) {
         throw new IllegalArgumentException("Illegal protobuf name for class " + javaClass.getCanonicalName() + ". Name cannot be null or empty.");
      }
      for (int i = 0; i < name.length(); i++) {
         char ch = name.charAt(i);
         if (ch != '.' && !Character.isJavaIdentifierPart(ch)) { //todo [anistor] validate against protobuf identifier rules, not java identifier rules (but they are close enough)
            throw new IllegalArgumentException("Illegal protobuf name for class " + javaClass.getCanonicalName() + ". Invalid identifier : " + name);
         }
      }
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
      String protoDocs = getProtoDocs();

      // Add @TypeId(..) if any
      Integer protoTypeId = getProtoTypeId();

      if (protoTypeId != null) {
         String typeIdAnnotation = '@' + Configuration.TYPE_ID_ANNOTATION + '(' + protoTypeId + ")\n";
         if (protoDocs == null) {
            protoDocs = typeIdAnnotation;
         } else {
            protoDocs += "\n" + typeIdAnnotation;
         }
      }

      return protoDocs;
   }

   public String getProtoDocs() {
      return getAnnotatedClass().getDocumentation();
   }

   public Integer getProtoTypeId() {
      //todo [anistor] what if @Protodoc("@TypeId(xxx)") is used instead ?
      ProtoTypeId protoTypeId = getAnnotatedClass().getAnnotation(ProtoTypeId.class);
      return protoTypeId != null ? protoTypeId.value() : null;
   }

   public XClass getJavaClass() {
      return javaClass;
   }

   public String getJavaClassName() {
      String canonicalName = javaClass.getCanonicalName();
      return canonicalName != null ? canonicalName : javaClass.getName();
   }

   /**
    * At this level we pretend the Java class and the annotated class are one and the same, but subclasses
    * may decide otherwise.
    */
   public XClass getAnnotatedClass() {
      return getJavaClass();
   }

   public String getAnnotatedClassName() {
      String canonicalName = getAnnotatedClass().getCanonicalName();
      return canonicalName != null ? canonicalName : getAnnotatedClass().getName();
   }

   public boolean isAdapter() {
      return false;
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

   /**
    * This is only for enums.
    */
   public abstract ProtoEnumValueMetadata getEnumMemberByName(String name);
   public abstract ProtoEnumValueMetadata getEnumMemberByNumber(int number);

   public final ProtoMessageTypeMetadata getOuterType() {
      return outerType;
   }

   protected final void setOuterType(ProtoMessageTypeMetadata outerType) {
      this.outerType = outerType;
   }

   @Override
   public void generateProto(IndentWriter iw, ProtoSyntax syntax) {
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
