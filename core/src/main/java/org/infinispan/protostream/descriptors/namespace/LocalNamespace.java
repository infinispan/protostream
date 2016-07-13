package org.infinispan.protostream.descriptors.namespace;

import java.util.Collections;
import java.util.Map;

import org.infinispan.protostream.descriptors.GenericDescriptor;

/**
 * @author anistor@redhat.com
 * @since 3.1
 */
public final class LocalNamespace implements Namespace {

   private final Map<String, GenericDescriptor> localDefinitions;

   LocalNamespace(Map<String, GenericDescriptor> localDefinitions) {
      this.localDefinitions = Collections.unmodifiableMap(localDefinitions);
   }

   @Override
   public GenericDescriptor get(String name) {
      return localDefinitions.get(name);
   }

   public Map<String, GenericDescriptor> getTypes() {
      return localDefinitions;
   }
}
