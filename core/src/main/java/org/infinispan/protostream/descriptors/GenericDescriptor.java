package org.infinispan.protostream.descriptors;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public interface GenericDescriptor {

   String getName();

   String getFullName();

   FileDescriptor getFileDescriptor();
}
