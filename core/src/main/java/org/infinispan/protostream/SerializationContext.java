package org.infinispan.protostream;

import java.util.Set;

/**
 * A repository for Protobuf type definitions and their marshallers. All ProtoStream marshalling operations happen in
 * the context of a {@code SerializationContext}.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface SerializationContext extends ImmutableSerializationContext {

   /**
    * Register some proto schema definition files from a {@link FileDescriptorSource}.
    *
    * @param source
    * @throws DescriptorParserException
    */
   void registerProtoFiles(FileDescriptorSource source) throws DescriptorParserException;

   /**
    * Unregisters a file. All types defined in it are removed and also the types from all dependant files. The status of
    * dependant files is set to 'unresolved'.
    */
   void unregisterProtoFile(String fileName);

   /**
    * Unregisters a set of files. All types defined in them are removed and also the types from all dependant files. The
    * status of dependant files is set to 'unresolved'.
    */
   void unregisterProtoFiles(Set<String> fileNames);

   /**
    * Register a type marshaller.
    *
    * @param marshaller the marshaller instance
    */
   void registerMarshaller(BaseMarshaller<?> marshaller);

   void unregisterMarshaller(BaseMarshaller<?> marshaller);

   void registerMarshallerProvider(MarshallerProvider marshallerProvider);

   void unregisterMarshallerProvider(MarshallerProvider marshallerProvider);

   /**
    * Interface to be implemented for dynamic lookup of marshallers. The marshaller instances returned by the provider
    * are never cached internally by Protostream and a new invocation is performed each time the marshaller for a type
    * is needed. The provider is responsible for caching the marshaller instance if this is suitable and worthwhile.
    */
   interface MarshallerProvider {

      /**
       * Get a marshaller instance for the given type name.
       *
       * @return the marshaller instance or {@code null} if the types cannot be marshalled by this provider
       */
      BaseMarshaller<?> getMarshaller(String typeName);

      /**
       * Get a marshaller instance for the given Java class.
       *
       * @return the marshaller instance or {@code null} if the java class cannot be marshalled by this provider
       */
      BaseMarshaller<?> getMarshaller(Class<?> javaClass);
   }
}
