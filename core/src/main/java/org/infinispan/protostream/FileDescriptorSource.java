package org.infinispan.protostream;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregator for source proto files to be passed to {@link SerializationContext#registerProtoFiles(FileDescriptorSource)}.
 * The files are guaranted to be processed in the order specified.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class FileDescriptorSource {

   private static final int READ_BUFFER_SIZE = 1024;

   /**
    * The unparsed files. Using a LinkedHashMap to ensure parsing happens in the order specified by the user so the
    * eventual errors will appear in a predictable order.
    */
   private final Map<String, String> files = new LinkedHashMap<>();

   /**
    * Optional progress callback. If non-null it will be invoked based on the status of each parsed file.
    */
   private ProgressCallback progressCallback;

   /**
    * A callback interface that receives status notifications during the processing of files defined by a {@link
    * FileDescriptorSource}.
    */
   public interface ProgressCallback {

      /**
       * This is invoked when an error is encountered, possibly more thant once per file.
       *
       * @param fileName  the name of the file that failed
       * @param exception the error
       */
      default void handleError(String fileName, DescriptorParserException exception) {
      }

      /**
       * This is invoked at most once per file, at the end of the parsing of a file, if it completed successfully.
       *
       * @param fileName the name of the file that was parsed successfully
       */
      default void handleSuccess(String fileName) {
      }
   }

   /**
    * Set the ProgressCallback. A {@code null} callback indicates that errors are to be reported immediately and the
    * parsing operation should be aborted on first error.
    *
    * @param progressCallback the callback, can be {@code null}
    * @return this object, for easy method chaining
    */
   public FileDescriptorSource withProgressCallback(ProgressCallback progressCallback) {
      this.progressCallback = progressCallback;
      return this;
   }

   /**
    * Add proto files from class path. The resource names are expected to be absolute.
    */
   public FileDescriptorSource addProtoFiles(String... classpathResources) throws IOException {
      return addProtoFiles(null, classpathResources);
   }

   /**
    * Add proto files from class path. The resource names are expected to be absolute.
    */
   public FileDescriptorSource addProtoFiles(ClassLoader userClassLoader, String... classpathResources) throws IOException {
      for (String classpathResource : classpathResources) {
         if (classpathResource == null) {
            throw new IllegalArgumentException("classpathResource argument cannot be null");
         }
         // enforce absolute resource path
         String absResPath = classpathResource.startsWith("/") ? classpathResource : "/" + classpathResource;
         InputStream resource = getResourceAsStream(userClassLoader, absResPath);
         if (resource == null) {
            throw new IOException("Resource not found in class path : " + absResPath);
         }
         // discard the leading slash
         String path = classpathResource.startsWith("/") ? classpathResource.substring(1) : classpathResource;
         addProtoFile(path, resource);
      }
      return this;
   }

   /**
    * Add a proto file, given a name and the file contents as a {@link String}.
    */
   public FileDescriptorSource addProtoFile(String name, String fileContents) {
      if (name == null) {
         throw new IllegalArgumentException("name argument cannot be null");
      }
      if (fileContents == null) {
         throw new IllegalArgumentException("fileContents argument cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      files.put(path, fileContents);
      return this;
   }

   /**
    * Add a proto file, given a name and the file contents as an {@link InputStream}.
    */
   public FileDescriptorSource addProtoFile(String name, InputStream fileContents) throws IOException {
      if (name == null) {
         throw new IllegalArgumentException("name argument cannot be null");
      }
      if (fileContents == null) {
         throw new IllegalArgumentException("fileContents argument cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      files.put(path, getContentsAsString(fileContents));
      return this;
   }

   /**
    * Add a proto file, given a name and the file contents as a {@link Reader}.
    */
   public FileDescriptorSource addProtoFile(String name, Reader fileContents) throws IOException {
      if (name == null) {
         throw new IllegalArgumentException("name argument cannot be null");
      }
      if (fileContents == null) {
         throw new IllegalArgumentException("contents argument cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      files.put(path, getContentsAsString(fileContents));
      return this;
   }

   /**
    * Add a proto file, given a name and the file contents as a {@link File}.
    */
   public FileDescriptorSource addProtoFile(String name, File protoFile) throws IOException {
      if (name == null) {
         throw new IllegalArgumentException("name argument cannot be null");
      }
      if (protoFile == null) {
         throw new IllegalArgumentException("protoFile argument cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      files.put(path, getContentsAsString(protoFile));
      return this;
   }

   public static FileDescriptorSource fromResources(ClassLoader userClassLoader, String... classPathResources) throws IOException {
      return new FileDescriptorSource().addProtoFiles(userClassLoader, classPathResources);
   }

   public static FileDescriptorSource fromResources(String... classPathResources) throws IOException {
      return new FileDescriptorSource().addProtoFiles(classPathResources);
   }

   public static FileDescriptorSource fromString(String name, String fileContents) {
      return new FileDescriptorSource().addProtoFile(name, fileContents);
   }

   /**
    * @deprecated This method was added for internal use and is deprecated since 4.3.4 to be removed in 5.
    * Replaced by {@link #getFiles()}
    */
   @Deprecated
   public Map<String, char[]> getFileDescriptors() {
      LinkedHashMap<String, char[]> map = new LinkedHashMap<>();
      for (Map.Entry<String, String> e : files.entrySet()) {
         map.put(e.getKey(), e.getValue().toCharArray());
      }
      return map;
   }

   public Map<String, String> getFiles() {
      return Collections.unmodifiableMap(files);
   }

   public ProgressCallback getProgressCallback() {
      return progressCallback;
   }

   private static String getContentsAsString(File file) throws IOException {
      try (FileInputStream is = new FileInputStream(file)) {
         return getContentsAsString(is);
      }
   }

   private static String getContentsAsString(InputStream is) throws IOException {
      try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
         return getContentsAsString(reader);
      }
   }

   private static String getContentsAsString(Reader reader) throws IOException {
      try {
         CharArrayWriter writer = new CharArrayWriter();
         char[] buffer = new char[READ_BUFFER_SIZE];
         int count;
         while ((count = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, count);
         }
         return writer.toString();
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

   /**
    * Finds a resource with a given name, relative to a given Class and returns it as a String.
    *
    * @throws UncheckedIOException if the resource is not found or an I/O error occurs
    * @deprecated This method is internal and has been deprecated in 4.3.4 to prevent use from external projects as it
    * is subject for removal in 5.
    */
   @Deprecated
   public static String getResourceAsString(Class<?> c, String name) throws UncheckedIOException {
      try (InputStream is = c.getResourceAsStream(name)) {
         if (is == null) {
            throw new IOException("Resource not found in class path : " + name);
         }
         try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            StringWriter writer = new StringWriter();
            char[] buffer = new char[READ_BUFFER_SIZE];
            int count;
            while ((count = reader.read(buffer)) != -1) {
               writer.write(buffer, 0, count);
            }
            return writer.toString();
         }
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }
}
