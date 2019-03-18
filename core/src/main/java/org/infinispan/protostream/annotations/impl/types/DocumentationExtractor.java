package org.infinispan.protostream.annotations.impl.types;

import org.infinispan.protostream.annotations.ProtoDoc;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class DocumentationExtractor {

   /**
    * Collect and concatenate the description text from the {@code @ProtoDoc.value} of the given ProtoDoc annotations
    * (that were previously obtained either from an AnnotatedElement or an AnnotatedConstruct). Each annotation value is
    * put on a separate line. The beginning and trailing empty lines are trimmed off. If the resulting documentation
    * text does not have any line then {@code null} is returned.
    */
   public static String getDocumentation(ProtoDoc[] annotations) {
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

      return sb == null ? null : sb.toString();
   }
}
