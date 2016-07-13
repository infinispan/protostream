package org.infinispan.protostream.descriptors.namespace;

import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;

/**
 * @author anistor@redhat.com
 * @since 3.1
 */
final class ExportedNamespace extends CompositeNamespace {

   private final FileDescriptor fileDescriptor;

   private final Namespace[] namespaces;

   ExportedNamespace(FileDescriptor fileDescriptor, LocalNamespace localNamespace, ImportedNamespace publicImports) {
      this.fileDescriptor = fileDescriptor;
      namespaces = new Namespace[]{localNamespace, publicImports};
   }

   @Override
   public GenericDescriptor get(String name) {
      if (fileDescriptor.isResolved()) {
         return super.get(name);
      }
      throw new IllegalStateException("File " + fileDescriptor.getName() + " is not resolved yet");
   }

   @Override
   protected Namespace[] getNamespaces() {
      return namespaces;
   }
}
