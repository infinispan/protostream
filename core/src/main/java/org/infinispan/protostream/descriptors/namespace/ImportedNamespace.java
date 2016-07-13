package org.infinispan.protostream.descriptors.namespace;

import java.util.Collection;

import org.infinispan.protostream.descriptors.FileDescriptor;

/**
 * @author anistor@redhat.com
 * @since 3.1
 */
final class ImportedNamespace extends CompositeNamespace {

   private final Namespace[] namespaces;

   ImportedNamespace(Collection<FileDescriptor> importedFiles) {
      if (importedFiles == null || importedFiles.isEmpty()) {
         namespaces = null;
      } else {
         namespaces = new Namespace[importedFiles.size()];
         int i = 0;
         for (FileDescriptor file : importedFiles) {
            namespaces[i++] = file.getExportedNamespace();
         }
      }
   }

   @Override
   protected Namespace[] getNamespaces() {
      return namespaces;
   }
}
