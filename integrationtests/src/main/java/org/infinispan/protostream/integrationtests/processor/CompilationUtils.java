package org.infinispan.protostream.integrationtests.processor;

import static com.google.testing.compile.Compiler.javac;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;

import org.infinispan.protostream.annotations.impl.processor.AutoProtoSchemaBuilderAnnotationProcessor;

import com.google.common.io.Resources;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;

/**
 * Some utility functions for working with Compilation objects.
 *
 * @author anistor@redhat.com
 * @since 4.3.5
 */
public final class CompilationUtils {

   private static final String JAVA_SUFFIX = ".java";

   public static boolean checkFileContainsString(FileObject file, String string) {
      if (file == null) {
         throw new IllegalArgumentException("The file argument must not be null");
      }
      if (string == null || string.isEmpty()) {
         throw new IllegalArgumentException("The string argument must not be null or empty");
      }

      String src;
      try {
         src = file.getCharContent(true).toString();
      } catch (IOException ioe) {
         throw new UncheckedIOException(ioe);
      }

      return src.contains(string);
   }

   /**
    * Compiles several Java source files that are represented by resource files found in the classpath.
    *
    * @return a Compilation object that can be used to obtain information about the compilation outcome.
    */
   public static Compilation compile(String... resourceNames) {
      if (resourceNames == null || resourceNames.length == 0) {
         throw new IllegalArgumentException("resourceNames argument must not be null or empty");
      }

      List<JavaFileObject> files = new ArrayList<>(resourceNames.length);
      for (String resourceName : resourceNames) {
         files.add(createJavaFileObject(resourceName));
      }

      return javac()
            .withProcessors(new AutoProtoSchemaBuilderAnnotationProcessor())
            .compile(files);
   }

   /**
    * Loads a resource file from a classpath that is expected to be a Java source file and creates a JavaFileObject.
    *
    * @param resourceName the name of the resource file. The file name must end with a '.java' suffix.
    * @return a JavaFileObject representing the resource file.
    */
   private static JavaFileObject createJavaFileObject(String resourceName) {
      if (!resourceName.endsWith(JAVA_SUFFIX)) {
         throw new IllegalArgumentException("The resource must be a Java source file and the file name must end with '.java' suffix");
      }

      String fullyQualifiedName = resourceName.substring(0, resourceName.length() - JAVA_SUFFIX.length()).replace('/', '.');

      String source;
      try {
         source = Resources.asByteSource(Resources.getResource(resourceName))
               .asCharSource(StandardCharsets.UTF_8)
               .read();
      } catch (IOException ioe) {
         throw new UncheckedIOException(ioe);
      }
      return JavaFileObjects.forSourceString(fullyQualifiedName, source);
   }
}
