package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.annotations.ProtoDoc;

import java.lang.reflect.AnnotatedElement;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
final class DocumentationExtractor {

   public static String getDocumentation(AnnotatedElement... elements) {
      StringBuilder sb = null;

      for (AnnotatedElement element : elements) {
         ProtoDoc docAnnotation = element.getAnnotation(ProtoDoc.class);
         if (docAnnotation != null && !docAnnotation.value().isEmpty()) {
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
