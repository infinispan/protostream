package org.infinispan.protostream.annotations.impl.processor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Generate a META-INF/services resource file suitable for {@link java.util.ServiceLoader} mechanism.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
final class ServiceLoaderFileGenerator {

   private static final String META_INF_SERVICES = "META-INF/services/";

   private final String resourceFile;

   private final Set<String> providers = new HashSet<>();

   private final List<Element> originatingElements = new ArrayList<>();

   ServiceLoaderFileGenerator(Class<?> serviceInterface) {
      this.resourceFile = META_INF_SERVICES + serviceInterface.getName();
   }

   void addProvider(String providerClass, Element originatingElement) {
      providers.add(providerClass);
      originatingElements.add(originatingElement);
   }

   void writeServiceFile(Filer filer) throws IOException {
      if (!providers.isEmpty()) {
         Set<String> serviceProviders;
         try {
            FileObject fileObject = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resourceFile);
            serviceProviders = readServiceFile(fileObject.openInputStream());
         } catch (IOException e) {
            // service resource file does not exist yet
            serviceProviders = new LinkedHashSet<>();
         }

         if (serviceProviders.addAll(providers)) {
            FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourceFile, originatingElements.toArray(new Element[0]));
            writeServiceFile(serviceProviders, fileObject.openOutputStream());
         }
      }
   }

   private static Set<String> readServiceFile(InputStream in) throws IOException {
      Set<String> services = new LinkedHashSet<>();
      try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
         String line;
         while ((line = r.readLine()) != null) {
            int commentPos = line.indexOf('#');
            if (commentPos != -1) {
               // drop comments
               line = line.substring(0, commentPos);
            }
            // trim spaces
            line = line.trim();
            if (!line.isEmpty()) {
               // keep only non-empty lines
               services.add(line);
            }
         }
      }
      return services;
   }

   private static void writeServiceFile(Collection<String> services, OutputStream out) throws IOException {
      try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
         for (String service : services) {
            w.write(service);
            w.newLine();
         }
         w.flush();
      }
   }
}
