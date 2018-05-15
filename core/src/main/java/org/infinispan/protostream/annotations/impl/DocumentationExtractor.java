package org.infinispan.protostream.annotations.impl;

import java.lang.reflect.AnnotatedElement;

import org.infinispan.protostream.annotations.ProtoDoc;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class DocumentationExtractor {

   /**
    * Collect and concatenate the description text from the {@code @ProtoDoc.value} of the given elements.
    */
   public static String getDocumentation(AnnotatedElement element1, AnnotatedElement element2) {
      String doc1 = getDocumentation(element1);
      String doc2 = getDocumentation(element2);

      if (doc1 == null) {
         return doc2;
      }
      if (doc2 == null) {
         return doc1;
      }
      return doc1 + '\n' + doc2;
   }

   /**
    * Collect and concatenate the description text from the {@code @ProtoDoc.value} of the given element.
    */
   public static String getDocumentation(AnnotatedElement element) {
      StringBuilder sb = null;

      for (ProtoDoc docAnnotation : element.getAnnotationsByType(ProtoDoc.class)) {
         if (!docAnnotation.value().isEmpty()) {
            if (sb == null) {
               sb = new StringBuilder();
            } else {
               sb.append('\n');
            }
            sb.append(docAnnotation.value());
         }
      }

      return sb == null ? null : sb.toString();
   }
}
