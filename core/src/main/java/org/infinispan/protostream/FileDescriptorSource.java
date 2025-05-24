package org.infinispan.protostream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregator for source proto files to be passed to {@link SerializationContext#registerProtoFiles(FileDescriptorSource)}.
 * The files are guaranteed to be processed in the order they were added (for better predictability or error reporting).
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class FileDescriptorSource {

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
         if (classpathResource.startsWith("/")) {
            classpathResource = classpathResource.substring(1);
         }
         InputStream resource = ResourceUtils.getResourceAsStream(userClassLoader, classpathResource);
         if (resource == null) {
            throw new IOException("Resource not found in class path : " + classpathResource);
         }
         addProtoFile(classpathResource, resource);
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
      files.put(path, ResourceUtils.getContentsAsString(fileContents));
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
      files.put(path, ResourceUtils.getContentsAsString(fileContents));
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
      files.put(path, ResourceUtils.getContentsAsString(protoFile));
      return this;
   }


   public FileDescriptorSource addProtoFile(File protoFile) throws IOException {
      return addProtoFile(protoFile.getName(), protoFile);
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
}
