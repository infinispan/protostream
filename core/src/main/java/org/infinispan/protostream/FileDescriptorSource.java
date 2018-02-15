package org.infinispan.protostream;

import static java.util.Collections.unmodifiableMap;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregator for source protofiles to be passed to {@link SerializationContext#registerProtoFiles(FileDescriptorSource)}.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class FileDescriptorSource {

   private static final String ENCODING = "UTF-8";
   private static final int BUFFER_SIZE = 1024;

   /**
    * The unparsed files. Using a LinkedHashMap to ensure parsing happens in the order specified by the user.
    */
   private final Map<String, char[]> descriptors = new LinkedHashMap<>();

   private ProgressCallback progressCallback;

   /**
    * A callback interface that receives status notifications during the processing of files defined by a {@link
    * FileDescriptorSource}.
    */
   public interface ProgressCallback {

      default void handleError(String fileName, DescriptorParserException exception) {
      }

      default void handleSuccess(String fileName) {
      }
   }

   /**
    * Set the ProgressCallback. A {@code null} callback indicates that errors are to be reported immediately and the
    * operation should be aborted on first error.
    *
    * @param progressCallback the callback, can be {@code null}
    * @return this object
    */
   public FileDescriptorSource withProgressCallback(ProgressCallback progressCallback) {
      this.progressCallback = progressCallback;
      return this;
   }

   public FileDescriptorSource addProtoFiles(String... classpathResources) throws IOException {
      return addProtoFiles(null, classpathResources);
   }

   public FileDescriptorSource addProtoFiles(ClassLoader userClassLoader, String... classpathResources) throws IOException {
      for (String classpathResource : classpathResources) {
         if (classpathResource == null) {
            throw new IllegalArgumentException("classpathResource cannot be null");
         }
         // enforce absolute resource path
         String absPath = classpathResource.startsWith("/") ? classpathResource : "/" + classpathResource;
         InputStream resourceAsStream = getResourceAsStream(userClassLoader, absPath);
         if (resourceAsStream == null) {
            throw new IOException("Resource not found in class path : " + classpathResource);
         }
         // discard the leading slash
         String path = classpathResource.startsWith("/") ? classpathResource.substring(1) : classpathResource;
         addProtoFile(path, resourceAsStream);
      }
      return this;
   }

   public FileDescriptorSource addProtoFile(String name, String contents) {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null");
      }
      if (contents == null) {
         throw new IllegalArgumentException("contents cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      descriptors.put(path, contents.toCharArray());
      return this;
   }

   public FileDescriptorSource addProtoFile(String name, InputStream contents) throws IOException {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null");
      }
      if (contents == null) {
         throw new IllegalArgumentException("contents cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      descriptors.put(path, toCharArray(contents));
      return this;
   }

   public FileDescriptorSource addProtoFile(String name, Reader contents) throws IOException {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null");
      }
      if (contents == null) {
         throw new IllegalArgumentException("contents cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      descriptors.put(path, toCharArray(contents));
      return this;
   }

   public FileDescriptorSource addProtoFile(String name, File protofile) throws IOException {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null");
      }
      if (protofile == null) {
         throw new IllegalArgumentException("protofile cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      descriptors.put(path, toCharArray(protofile));
      return this;
   }

   public static FileDescriptorSource fromResources(ClassLoader userClassLoader, String... classPathResources) throws IOException {
      return new FileDescriptorSource().addProtoFiles(userClassLoader, classPathResources);
   }

   public static FileDescriptorSource fromResources(String... classPathResources) throws IOException {
      return new FileDescriptorSource().addProtoFiles(classPathResources);
   }

   public static FileDescriptorSource fromString(String name, String protoSource) {
      return new FileDescriptorSource().addProtoFile(name, protoSource);
   }

   public Map<String, char[]> getFileDescriptors() {
      return unmodifiableMap(descriptors);
   }

   public ProgressCallback getProgressCallback() {
      return progressCallback;
   }

   private char[] toCharArray(File file) throws IOException {
      try (FileInputStream is = new FileInputStream(file)) {
         return toCharArray(is);
      }
   }

   private char[] toCharArray(InputStream is) throws IOException {
      try (Reader reader = new InputStreamReader(is, ENCODING)) {
         return toCharArray(reader);
      }
   }

   private char[] toCharArray(Reader reader) throws IOException {
      try {
         CharArrayWriter writer = new CharArrayWriter();
         char[] buffer = new char[BUFFER_SIZE];
         int count;
         while ((count = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, count);
         }
         return writer.toCharArray();
      } finally {
         reader.close();
      }
   }

   private static InputStream getResourceAsStream(ClassLoader userClassLoader, String resourcePath) {
      if (resourcePath.startsWith("/")) {
         resourcePath = resourcePath.substring(1);
      }
      ClassLoader[] classLoaders = {userClassLoader,
            FileDescriptorSource.class.getClassLoader(),
            ClassLoader.getSystemClassLoader(),
            Thread.currentThread().getContextClassLoader()};
      InputStream is = null;
      for (ClassLoader cl : classLoaders) {
         if (cl != null) {
            is = cl.getResourceAsStream(resourcePath);
            if (is != null) {
               break;
            }
         }
      }
      return is;
   }
}
