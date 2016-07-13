package org.infinispan.protostream.descriptors.namespace;

import org.infinispan.protostream.descriptors.GenericDescriptor;

/**
 * @author anistor@redhat.com
 * @since 3.1
 */
abstract class CompositeNamespace implements Namespace {

   @Override
   public GenericDescriptor get(String name) {
      Namespace[] namespaces = getNamespaces();
      if (namespaces != null) {
         for (Namespace n : namespaces) {
            GenericDescriptor d = n.get(name);
            if (d != null) {
               return d;
            }
         }
      }
      return null;
   }

   protected abstract Namespace[] getNamespaces();
}
