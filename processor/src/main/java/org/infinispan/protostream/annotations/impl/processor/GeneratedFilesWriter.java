package org.infinispan.protostream.annotations.impl.processor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Tracks generated source files. Also keeps track of which files are 'disabled', ie. generated due to dependency
 * processing, but are not part of current module and do not need to be actually emitted and compiled.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
final class GeneratedFilesWriter {

   private final Map<String, GeneratedFile> generatedFiles = new LinkedHashMap<>();

   interface GeneratedFile {

      /**
       * Will it be written on disk (true) or is it just a memory-only file (false)?
       */
      boolean isEnabled();

      String getSource();

      void write(Filer filer) throws IOException;
   }

   private static final class SourceFile implements GeneratedFile {

      private final boolean isEnabled;

      private final String className;

      private final String source;

      private final Element[] originatingElements;

      SourceFile(boolean isEnabled, String className, String source, Element... originatingElements) {
         this.isEnabled = isEnabled;
         this.className = className;
         this.source = source;
         this.originatingElements = originatingElements;
      }

      @Override
      public boolean isEnabled() {
         return isEnabled;
      }

      @Override
      public String getSource() {
         return source;
      }

      @Override
      public void write(Filer filer) throws IOException {
         // check disk contents
         if (source.equals(getSourceFileContents(filer, className))) {
            return;
         }

         JavaFileObject file;
         try {
            file = filer.createSourceFile(className, originatingElements);
         } catch (FilerException fe) {
            // duplicated class name maybe
            throw new AnnotationProcessingException(fe, originatingElements[0], "%s", fe.getMessage());
         }

         try (PrintWriter out = new PrintWriter(file.openWriter())) {
            out.print(source);
         }
      }
   }

   private static final class ResourceFile implements GeneratedFile {

      private final boolean isEnabled;

      private final String pkg;

      private final String relativeName;

      private final String source;

      private final Element[] originatingElements;

      ResourceFile(boolean isEnabled, String pkg, String relativeName, String source, Element... originatingElements) {
         this.isEnabled = isEnabled;
         this.pkg = pkg;
         this.relativeName = relativeName;
         this.source = source;
         this.originatingElements = originatingElements;
      }

      @Override
      public boolean isEnabled() {
         return isEnabled;
      }

      @Override
      public String getSource() {
         return source;
      }

      @Override
      public void write(Filer filer) throws IOException {
         // check disk contents
         if (source.equals(getResourceFileContents(filer, pkg, relativeName))) {
            return;
         }

         FileObject file;
         try {
            file = filer.createResource(StandardLocation.CLASS_OUTPUT, pkg, relativeName, originatingElements);
         } catch (FilerException e) {
            throw new AnnotationProcessingException(e, originatingElements[0], "Package %s already contains a resource file named %s", pkg, relativeName);
         }

         try (PrintWriter out = new PrintWriter(file.openWriter())) {
            out.print(source);
         }
      }
   }

   private final Filer filer;

   private boolean isEnabled = true;

   GeneratedFilesWriter(Filer filer) {
      this.filer = filer;
   }

   public boolean isEnabled() {
      return isEnabled;
   }

   public void setEnabled(boolean isEnabled) {
      this.isEnabled = isEnabled;
   }

   public void addMarshallerSourceFile(String className, String source, Element originatingElement) throws IOException {
      addGeneratedFile(className, new SourceFile(isEnabled, className, source, originatingElement));
   }

   public void addInitializerSourceFile(String className, String source, Element[] originatingElements) throws IOException {
      addGeneratedFile(className, new SourceFile(isEnabled, className, source, originatingElements));
   }

   public void addSchemaResourceFile(String pkg, String relativeName, String source, Element[] originatingElements) throws IOException {
      String fqn = pkg.isEmpty() ? relativeName : pkg + '/' + relativeName;
      addGeneratedFile(fqn, new ResourceFile(isEnabled, pkg, relativeName, source, originatingElements));
   }

   private void addGeneratedFile(String fqn, GeneratedFile file) throws IOException {
      boolean doWrite = true;

      GeneratedFile existingFile = generatedFiles.get(fqn);
      if (existingFile != null) {
         // check in-memory contents
         if (!file.getSource().equals(existingFile.getSource())) {
            throw new IllegalStateException("File " + fqn + " was generated twice with different contents.");
         }
         doWrite = !existingFile.isEnabled() && isEnabled;
      }

      if (doWrite) {
         generatedFiles.put(fqn, file);
         if (isEnabled) {
            file.write(filer);
         }
      }
   }

   /**
    * Reads the contents of a generated resource file if it exists.
    *
    * @param filer        the Filer
    * @param pkg          package relative to which the file should be searched, or the empty string if none
    * @param relativeName final pathname components of the file
    * @return {@code true} if the file exists and contents is as expected, {@code false} otherwise
    * @throws IOException if there is an I/O error during reading of file contents
    */
   private static String getResourceFileContents(Filer filer, String pkg, String relativeName) throws IOException {
      InputStream is;
      try {
         is = filer.getResource(StandardLocation.CLASS_OUTPUT, pkg, relativeName).openInputStream();
      } catch (IOException e) {
         return null;
      }

      try {
         return readUtf8String(is);
      } finally {
         is.close();
      }
   }

   /**
    * Reads the contents of a generated source file if it exists.
    *
    * @param filer     the Filer
    * @param className fully qualified class name
    * @return {@code true} if the file exists and contents is as expected, {@code false} otherwise
    * @throws IOException if there is an I/O error during reading of file contents
    */
   private static String getSourceFileContents(Filer filer, String className) throws IOException {
      InputStream is;
      try {
         is = filer.getResource(StandardLocation.SOURCE_OUTPUT, "", className.replace('.', '/') + ".java").openInputStream();
      } catch (IOException e) {
         return null;
      }

      try {
         return readUtf8String(is);
      } finally {
         is.close();
      }
   }

   //todo [anistor] we assume that our source/resource files are all UTF-8
   private static String readUtf8String(InputStream is) throws IOException {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte[] buf = new byte[1024];
      int len;
      while ((len = is.read(buf)) != -1) {
         bytes.write(buf, 0, len);
      }
      return bytes.toString(StandardCharsets.UTF_8.name());
   }
}
