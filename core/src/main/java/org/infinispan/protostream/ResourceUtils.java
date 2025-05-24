package org.infinispan.protostream;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * @author anistor@redhat.com
 * @since 4.3.4
 */
public final class ResourceUtils {

   private static final int READ_BUFFER_SIZE = 2048;

   private ResourceUtils() {
   }

   public static String getContentsAsString(Reader reader) throws IOException {
      try (reader) {
         CharArrayWriter writer = new CharArrayWriter();
         char[] buffer = new char[READ_BUFFER_SIZE];
         int count;
         while ((count = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, count);
         }
         return writer.toString();
      }
   }

   public static String getContentsAsString(InputStream is) throws IOException {
      try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
         return getContentsAsString(reader);
      }
   }

   public static String getContentsAsString(File file) throws IOException {
      try (FileInputStream is = new FileInputStream(file)) {
         return getContentsAsString(is);
      }
   }

   /**
    * Returns an input stream for reading the specified resource or {@code null} if the resource could not be found.
    */
   public static InputStream getResourceAsStream(ClassLoader userClassLoader, String resourcePath) {
      if (resourcePath.startsWith("/")) {
         resourcePath = resourcePath.substring(1);
      }

      ClassLoader[] classLoaders = {
            userClassLoader,
            ResourceUtils.class.getClassLoader(),
            ClassLoader.getSystemClassLoader(),
            Thread.currentThread().getContextClassLoader()
      };

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

   public static Reader getResourceAsReader(Class<?> c, String resourcePath) throws UncheckedIOException {
      try {
         InputStream is = c.getResourceAsStream(resourcePath);
         if (is == null) {
            throw new IOException("Resource not found in class path : " + resourcePath);
         }
         return new InputStreamReader(is, StandardCharsets.UTF_8);
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   /**
    * Finds a resource with a given path relative to a given {@link Class} and returns it as a {@link String}.
    *
    * @throws UncheckedIOException if the resource is not found or an I/O error occurs
    */
   public static String getResourceAsString(Class<?> c, String resourcePath) throws UncheckedIOException {
      try (Reader reader = getResourceAsReader(c, resourcePath)) {
         return getContentsAsString(reader);
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }
}
