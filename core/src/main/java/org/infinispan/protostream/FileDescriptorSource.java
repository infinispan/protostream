package org.infinispan.protostream;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableMap;

/**
 * Aggregator for source protofiles
 *
 * @author gustavonalle
 * @since 2.0
 */
public class FileDescriptorSource {

   public static final String ENCODING = "UTF-8";
   public static final int BUFFER_SIZE = 1024;

   private final Map<String, char[]> descriptors = new ConcurrentHashMap<>();

   public void addProtoFiles(String... classpathResources) throws IOException {
      for (String classpathResource : classpathResources) {
         String absPath = classpathResource.startsWith("/") ? classpathResource : "/" + classpathResource;
         InputStream resourceAsStream = this.getClass().getResourceAsStream(absPath);
         String name = Paths.get(classpathResource).getFileName().toString();
         descriptors.put(name, (toCharArray(resourceAsStream)));
      }
   }

   public void addProtoFile(String name, String contents) {
      descriptors.put(name, contents.toCharArray());
   }

   public void addProtoFile(String name, InputStream contents) throws IOException {
      descriptors.put(name, toCharArray(contents));
   }
   
   public void addProtoFiles(File... protofiles) throws IOException {
      for (File protofile : protofiles) {
         descriptors.put(protofile.getName(), toCharArray(protofile));
      }
   }

   public static FileDescriptorSource fromResources(String... classPathResources) throws IOException {
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFiles(classPathResources);
      return fileDescriptorSource;
   }

   public static FileDescriptorSource fromFiles(File... files) throws IOException {
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFiles(files);
      return fileDescriptorSource;
   }

   public static FileDescriptorSource fromString(String name, String protoSource) {
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFile(name, protoSource);
      return fileDescriptorSource;
   }

   public Map<String, char[]> getFileDescriptors() {
      return unmodifiableMap(descriptors);
   }

   private char[] toCharArray(File file) throws IOException {
      try (FileInputStream is = new FileInputStream(file)) {
         return toCharArray(new InputStreamReader(is, ENCODING));
      }
   }

   private char[] toCharArray(InputStream is) throws IOException {
      return toCharArray(new InputStreamReader(is, ENCODING));
   }

   private char[] toCharArray(Reader reader) throws IOException {
      CharArrayWriter writer = new CharArrayWriter();
      char[] buffer = new char[BUFFER_SIZE];
      int count;
      while ((count = reader.read(buffer)) != -1) {
         writer.write(buffer, 0, count);
      }
      return writer.toCharArray();
   }

}
