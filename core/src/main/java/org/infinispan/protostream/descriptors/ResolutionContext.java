package org.infinispan.protostream.descriptors;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.impl.Log;

/**
 * @author anistor@redhat.com
 * @since 3.1
 */
public class ResolutionContext {

   private static final Log log = Log.LogFactory.getLog(ResolutionContext.class);

   private final FileDescriptorSource.ProgressCallback progressCallback;

   private final Map<String, FileDescriptor> fileDescriptorMap;

   private final Map<Integer, GenericDescriptor> allTypeIds;

   private final Map<Integer, GenericDescriptor> typeIds = new HashMap<>();

   private Map<String, GenericDescriptor> allGlobalTypes;

   private Map<String, GenericDescriptor> globalTypes = new HashMap<>();

   private Map<String, EnumValueDescriptor> allEnumValueDescriptors;

   private Map<String, EnumValueDescriptor> enumValueDescriptors = new HashMap<>();

   public ResolutionContext(FileDescriptorSource.ProgressCallback progressCallback,
                            Map<String, FileDescriptor> fileDescriptorMap,
                            Map<String, GenericDescriptor> allGlobalTypes,
                            Map<Integer, GenericDescriptor> allTypeIds,
                            Map<String, EnumValueDescriptor> allEnumValueDescriptors) {
      this.progressCallback = progressCallback;
      this.fileDescriptorMap = fileDescriptorMap;
      this.allGlobalTypes = allGlobalTypes;
      this.allTypeIds = allTypeIds;
      this.allEnumValueDescriptors = allEnumValueDescriptors;
   }

   public void resolve() {
      // clear errors and put in unresolved state whatever is not already resolved
      for (FileDescriptor fileDescriptor : fileDescriptorMap.values()) {
         fileDescriptor.clearErrors();
      }

      // resolve imports and types for all files
      for (FileDescriptor fileDescriptor : fileDescriptorMap.values()) {
         fileDescriptor.resolveDependencies(this);
      }

      // clear errors and leave in unresolved state whatever could not be resolved
      for (FileDescriptor fileDescriptor : fileDescriptorMap.values()) {
         fileDescriptor.clearErrors();
      }
   }

   void handleError(FileDescriptor fileDescriptor, DescriptorParserException dpe) {
      if (log.isDebugEnabled()) {
         log.debugf(dpe, "File has errors : %s", fileDescriptor.getName());
      }

      // name resolving errors are not fatal
      fileDescriptor.markError();
      if (progressCallback == null) {
         throw dpe;
      }
      progressCallback.handleError(fileDescriptor.getName(), dpe);
   }

   void handleSuccess(FileDescriptor fileDescriptor) {
      if (log.isDebugEnabled()) {
         log.debugf("File resolved successfully : %s", fileDescriptor.getName());
      }

      if (progressCallback != null) {
         progressCallback.handleSuccess(fileDescriptor.getName());
      }
   }

   Map<String, FileDescriptor> getFileDescriptorMap() {
      return fileDescriptorMap;
   }

   void addGenericDescriptor(GenericDescriptor genericDescriptor) {
      checkUniqueName(genericDescriptor);

      if (genericDescriptor.getTypeId() != null) {
         checkUniqueTypeId(genericDescriptor);
         typeIds.put(genericDescriptor.getTypeId(), genericDescriptor);
      }

      globalTypes.put(genericDescriptor.getFullName(), genericDescriptor);

      if (genericDescriptor instanceof EnumDescriptor) {
         EnumDescriptor enumDescriptor = (EnumDescriptor) genericDescriptor;
         for (EnumValueDescriptor ev : enumDescriptor.getValues()) {
            enumValueDescriptors.put(ev.getScopedName(), ev);
         }
      }
   }

   private void checkUniqueName(GenericDescriptor genericDescriptor) {
      GenericDescriptor existingGenericDescriptor = lookup(globalTypes, allGlobalTypes, genericDescriptor.getFullName());
      if (existingGenericDescriptor != null) {
         List<String> locations = Arrays.asList(genericDescriptor.getFileDescriptor().getName(), existingGenericDescriptor.getFileDescriptor().getName());
         if (locations.get(0).equals(locations.get(1))) {
            throw new DescriptorParserException("Duplicate definition of '" + genericDescriptor.getFullName() + "' in " + locations.get(0));
         }
         Collections.sort(locations); // sort names for more predictable error messages in unit tests
         throw new DescriptorParserException("Duplicate definition of " + genericDescriptor.getFullName() + " in " + locations.get(0) + " and " + locations.get(1));
      }

      EnumValueDescriptor existingEnumValueDescriptor = lookup(enumValueDescriptors, allEnumValueDescriptors, genericDescriptor.getFullName());
      if (existingEnumValueDescriptor != null) {
         List<String> locations = Arrays.asList(genericDescriptor.getFileDescriptor().getName(), existingEnumValueDescriptor.getFileDescriptor().getName());
         Collections.sort(locations);
         throw new DescriptorParserException((genericDescriptor instanceof EnumDescriptor ? "Enum" : "Message") + " definition " + genericDescriptor.getFullName()
               + " clashes with enum value " + existingEnumValueDescriptor.getFullName());
      }

      if (genericDescriptor instanceof EnumDescriptor) {
         EnumDescriptor enumDescriptor = (EnumDescriptor) genericDescriptor;
         for (EnumValueDescriptor ev : enumDescriptor.getValues()) {
            // check if this enum value constant conflicts with another enum value constant
            existingEnumValueDescriptor = lookup(enumValueDescriptors, allEnumValueDescriptors, ev.getScopedName());
            if (existingEnumValueDescriptor != null) {
               throw new DescriptorParserException("Enum value " + ev.getFullName() + " clashes with enum value " + existingEnumValueDescriptor.getFullName());
            }

            existingGenericDescriptor = lookup(globalTypes, allGlobalTypes, ev.getScopedName());
            if (existingGenericDescriptor != null) {
               throw new DescriptorParserException("Enum value " + ev.getFullName() + " clashes with " + (existingGenericDescriptor instanceof EnumDescriptor ? "enum" : "message")
                     + " definition " + existingGenericDescriptor.getFullName());
            }
         }
      }
   }

   private void checkUniqueTypeId(GenericDescriptor descriptor) {
      GenericDescriptor existing = lookup(typeIds, allTypeIds, descriptor.getTypeId());
      if (existing != null) {
         throw new DescriptorParserException("Duplicate type id " + descriptor.getTypeId() + " for type " + descriptor.getFullName() + ". Already used by " + existing.getFullName());
      }
   }

   private <K, V> V lookup(Map<K, V> first, Map<K, V> second, K k) {
      V v = first.get(k);
      return v != null ? v : second.get(k);
   }

   void flush() {
      allGlobalTypes.putAll(globalTypes);
      allTypeIds.putAll(typeIds);
      allEnumValueDescriptors.putAll(enumValueDescriptors);
      clear();
   }

   void clear() {
      globalTypes.clear();
      typeIds.clear();
      enumValueDescriptors.clear();
   }
}
