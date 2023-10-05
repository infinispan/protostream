package org.infinispan.protostream.annotations.impl.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import org.infinispan.protostream.annotations.ProtoComment;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class DocumentationExtractor {

   public static final String PROTOSTREAM_ANNOTATIONS_PREFIX = "@org.infinispan.protostream.annotations";

   /**
    * Collect and concatenate the description text from the {@code @ProtoDoc.value} of the given ProtoDoc annotations
    * (that were previously obtained either from an AnnotatedElement or an AnnotatedConstruct). Each annotation value is
    * put on a separate line. The beginning and trailing empty lines are trimmed off. If the resulting documentation
    * text does not have any line then {@code null} is returned.
    */
   private static StringBuilder getDocumentation(ProtoComment[] annotations) {
      int start = 0;
      while (start < annotations.length && annotations[start].value().isEmpty()) {
         start++;
      }

      int end = annotations.length;
      while (end > start && annotations[end - 1].value().isEmpty()) {
         end--;
      }

      StringBuilder sb = null;
      for (int i = start; i < end; i++) {
         if (sb == null) {
            sb = new StringBuilder();
         } else {
            sb.append('\n');
         }
         sb.append(annotations[i].value());
      }

      return sb;
   }

   /**
    * Collect and concatenate the description text from an element (type, field, method). This unwraps
    * {@code @ProtoDoc.value}s and copies other annotations as-is. The beginning and trailing empty lines are trimmed
    * off. If the resulting documentation text does not have any line then {@code null} is returned.
    */
   public static String getDocumentation(Element element, boolean fullyQualified) {
      StringBuilder docs = getDocumentation(element.getAnnotationsByType(ProtoComment.class));
      // Copy other annotations as-is
      for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
         String s = mirror.toString();
         docs = annotationToString(docs, s, fullyQualified);
      }
      return docs != null ? docs.toString() : null;
   }

   public static String getDocumentation(Field f, boolean fullyQualified) {
      StringBuilder docs = getDocumentation(f.getAnnotationsByType(ProtoComment.class));
      return getDocumentation(docs, f.getAnnotations(), fullyQualified);
   }

   public static String getDocumentation(Class<?> clazz, boolean fullyQualified) {
      StringBuilder docs = getDocumentation(clazz.getAnnotationsByType(ProtoComment.class));
      return getDocumentation(docs, clazz.getAnnotations(), fullyQualified);
   }

   public static String getDocumentation(Method method, boolean fullyQualified) {
      StringBuilder docs = getDocumentation(method.getAnnotationsByType(ProtoComment.class));
      return getDocumentation(docs, method.getAnnotations(), fullyQualified);
   }

   private static String getDocumentation(StringBuilder docs, Annotation[] annotations, boolean fullyQualified) {
      for (Annotation annotation : annotations) {
         String s = annotation.toString();
         docs = annotationToString(docs, s, fullyQualified);
      }
      return docs != null ? docs.toString() : null;
   }

   private static StringBuilder annotationToString(StringBuilder docs, String s, boolean fullyQualified) {
      if (!s.startsWith(PROTOSTREAM_ANNOTATIONS_PREFIX)) {
         if (docs == null) {
            docs = new StringBuilder();
         } else {
            docs.append('\n');
         }
         if (fullyQualified) {
            docs.append(s);
         } else {
            // Remove the package name
            docs.append('@');
            int i = s.indexOf('(');
            i = i > 0 ? s.lastIndexOf('.', i) : s.lastIndexOf('.');
            docs.append(i > 0 ? s.substring(i + 1) : s.substring(1));
         }
      }
      return docs;
   }
}
