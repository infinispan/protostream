package org.infinispan.protostream.annotations.impl.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
class SourceFileWriter {

   private final Map<String, GeneratedSourceFile> generatedClasses = new LinkedHashMap<>();

   private static class GeneratedSourceFile {

      private final String className;

      private final String classSource;

      private final Element originatingElement;

      GeneratedSourceFile(String className, String classSource, Element originatingElement) {
         this.className = className;
         this.classSource = classSource;
         this.originatingElement = originatingElement;
      }
   }

   SourceFileWriter() {
   }

   public void writeSourceFile(String className, String classSource, Element originatingElement) {
      generatedClasses.put(className, new GeneratedSourceFile(className, classSource, originatingElement));
   }

   private void writeFile(Filer filer, String className, String classSource, Element originatingElement) throws IOException {
      try {
         JavaFileObject file = filer.createSourceFile(className, originatingElement);
         try (PrintWriter out = new PrintWriter(file.openWriter())) {
            out.print(classSource);
         }
      } catch (FilerException e) {
         // ignore if already generated
      }
   }

   public void writeFiles(Filer filer) throws IOException {
      for (GeneratedSourceFile f : generatedClasses.values()) {
         writeFile(filer, f.className, f.classSource, f.originatingElement);
      }
   }

   public Set<String> getGeneratedMarshallerClasses() {
      return generatedClasses.keySet();
   }
}
