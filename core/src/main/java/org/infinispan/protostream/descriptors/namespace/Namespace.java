package org.infinispan.protostream.descriptors.namespace;

import org.infinispan.protostream.descriptors.GenericDescriptor;

/**
 * @author anistor@redhat.com
 * @since 3.1
 */
public interface Namespace {

   /**
    * Looks up the descriptor by name.
    *
    * @param name the fully qualified type name
    * @return the descriptor or {@code null} if not found
    */
   GenericDescriptor get(String name);
}
