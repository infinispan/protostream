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
         // check disk contents before writing the generated file again to avoid causing needless rebuilds
         if (checkSourceFileUpToDate(filer, className, source)) {
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

      // file name relative to root package
      private final String fileName;

      private final String source;

      private final Element[] originatingElements;

      ResourceFile(boolean isEnabled, String fileName, String source, Element... originatingElements) {
         this.isEnabled = isEnabled;
         this.fileName = fileName.startsWith("/") ? fileName.substring(1) : fileName;
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
         // check disk contents before writing the generated file again to avoid causing needless rebuilds
         if (checkResourceFileUpToDate(filer, fileName, source)) {
            return;
         }

         FileObject file;
         try {
            file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", fileName, originatingElements);
         } catch (FilerException e) {
            throw new AnnotationProcessingException(e, originatingElements[0], "Failed to create resource file \"%s\". Name could be invalid or the file already exists?", fileName);
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
      SourceFile sourceFile = originatingElement != null ? new SourceFile(isEnabled, className, source, originatingElement): new SourceFile(isEnabled, className, source);
      addGeneratedFile(className, sourceFile);
   }

   public void addInitializerSourceFile(String className, String source, Element[] originatingElements) throws IOException {
      addGeneratedFile(className, new SourceFile(isEnabled, className, source, originatingElements));
   }

   public void addSchemaResourceFile(String fileName, String source, Element[] originatingElements) throws IOException {
      addGeneratedFile(fileName, new ResourceFile(isEnabled, fileName, source, originatingElements));
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
    * Checks that a generated resource file exists on disk and the contents matches the expected string.
    *
    * @param filer    the Filer
    * @param fileName the file name
    * @return {@code true} if the file exists and contents is as expected, {@code false} otherwise
    */
   private static boolean checkResourceFileUpToDate(Filer filer, String fileName, String contents) {
      try {
         FileObject resourceFile = filer.getResource(StandardLocation.CLASS_OUTPUT, "", fileName);
         try (InputStream is = resourceFile.openInputStream()) {
            String existing = readUtf8String(is);
            return contents.equals(existing);
         }
      } catch (IOException e) {
         return false;
      }
   }

   /**
    * Checks that a generated source file is up to date: it exists on disk, the contents matches the expected string and
    * the corresponding class also exists and its timestamp is newer.
    *
    * @param filer     the Filer
    * @param className fully qualified class name
    * @param contents  the expected contents of the source file
    * @return {@code true} if the file exists and contents is as expected, {@code false} otherwise
    */
   private static boolean checkSourceFileUpToDate(Filer filer, String className, String contents) {
      String fileName = className.replace('.', '/');

      long sourceTimestamp;
      try {
         FileObject javaFile = filer.getResource(StandardLocation.SOURCE_OUTPUT, "", fileName + ".java");
         try (InputStream is = javaFile.openInputStream()) {
            String existing = readUtf8String(is);
            if (!contents.equals(existing)) {
               return false;
            }
         }
         sourceTimestamp = javaFile.getLastModified();
      } catch (IOException e) {
         return false;
      }

      long classTimestamp;
      try {
         FileObject classFile = filer.getResource(StandardLocation.CLASS_OUTPUT, "", fileName + ".class");
         classTimestamp = classFile.getLastModified();
      } catch (IOException e) {
         return false;
      }

      return sourceTimestamp <= classTimestamp;
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
