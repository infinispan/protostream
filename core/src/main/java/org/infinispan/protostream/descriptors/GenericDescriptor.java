package org.infinispan.protostream.descriptors;

/**
 * Base interface for type (message and enum) descriptors.
 *
 * @author anistor@redhat.com
 * @since 2.0
 */
public interface GenericDescriptor {

   String getName();

   String getFullName();

   FileDescriptor getFileDescriptor();

   GenericDescriptor getContainingType();
}
