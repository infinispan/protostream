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

   /**
    * @deprecated Since 4.4. Replaced by the more flexible {@link InstanceMarshallerProvider}. To be removed in 5.
    */
   @Deprecated
   void registerMarshallerProvider(MarshallerProvider marshallerProvider);

   /**
    * @deprecated Since 4.4. Replaced by the more flexible {@link InstanceMarshallerProvider}. To be removed in 5.
    */
   @Deprecated
   void unregisterMarshallerProvider(MarshallerProvider marshallerProvider);

   <T> BaseMarshallerDelegate<T> getMarshallerDelegate(Class<T> typeName);

   /**
    * Interface to be implemented for dynamic lookup of marshallers. The marshaller instances returned by the provider
    * are never cached internally by ProtoStream and a new invocation is performed each time the marshaller for a type
    * is needed. The provider implementation is responsible for caching the marshaller instance if this is considered
    * suitable and worthwhile.
    * <p>
    * This interface is invoked last during marsahller lookup and only if lookup of statically registered marshallers
    * and also {@link InstanceMarshallerProvider} does not succeed in determining a marshaller.
    *
    * @deprecated Since 4.4. Replaced by the more flexible {@link InstanceMarshallerProvider}. To be removed in 5.
    */
   @Deprecated
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

   /**
    * Interface to be implemented for dynamic lookup of marshallers where the type is part of the entity being
    * marshalled.
    */
   interface InstanceMarshallerProvider<T> {

      /**
       * Returns the Java type handled by this marshaller. This must not change over multiple invocations.
       *
       * @return the Java type.
       */
      Class<T> getJavaClass();

      /**
       * Returns the protobuf types handled by this marshaller. This must not change over multiple invocations.
       *
       * @return the protobuf types.
       */
      Set<String> getTypeNames();

      String getTypeName(T instance);

      /**
       * Get marshaller given a instance to be marshalled or {@code null} if the instance cannot be marshalled by this provider.
       */
      BaseMarshaller<T> getMarshaller(T instance);

      /**
       * Get a marshaller to unmarshall the supplied type name or @code null} if the type cannot be unmarshalled by this provider.
       */
      BaseMarshaller<T> getMarshaller(String typeName);
   }

   void registerMarshallerProvider(InstanceMarshallerProvider<?> marshallerProvider);

   void unregisterMarshallerProvider(InstanceMarshallerProvider<?> marshallerProvider);
}
