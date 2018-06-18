package org.infinispan.protostream;

import java.io.IOException;

import org.infinispan.protostream.config.Configuration;

/**
 * A repository for protobuf definitions and marshallers. All marshalling operations happen in the context of a {@code
 * SerializationContext}.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface SerializationContext extends ImmutableSerializationContext {

   /**
    * Get the configuration.
    */
   Configuration getConfiguration();

   /**
    * Register some proto schema definition files from a {@link FileDescriptorSource}.
    *
    * @param source
    * @throws IOException
    * @throws DescriptorParserException
    */
   void registerProtoFiles(FileDescriptorSource source) throws IOException, DescriptorParserException;

   /**
    * Unregisters a file. Types from dependant files are removed too.
    */
   void unregisterProtoFile(String fileName);

   /**
    * Register a type marshaller.
    *
    * @param marshaller the marshaller instance
    */
   void registerMarshaller(BaseMarshaller<?> marshaller);

   void unregisterMarshaller(BaseMarshaller<?> marshaller);

   void registerMarshallerProvider(MarshallerProvider marshallerProvider);

   void unregisterMarshallerProvider(MarshallerProvider marshallerProvider);

   void registerDynamicMarshallerProvider(DynamicMarshallerProvider marshallerProvider);

   void unregisterDynamicMarshallerProvider(DynamicMarshallerProvider marshallerProvider);

   /**
    * Interface to be implemented for dynamic lookup of marshallers.
    */
   interface MarshallerProvider {

      /**
       * Get marshaller given a type name.
       *
       * @return the marshaller instance or {@code null} if the types cannot be marshalled by this provider
       */
      BaseMarshaller<?> getMarshaller(String typeName);

      /**
       * Get marshaller given a Java class.
       *
       * @return the marshaller instance or {@code null} if the java class cannot be marshalled by this provider
       */
      BaseMarshaller<?> getMarshaller(Class<?> javaClass);
   }

   /**
    * Interface to be implemented for dynamic lookup of marshallers where the type is part of the entity being
    * marshalled.
    */
   interface DynamicMarshallerProvider {

      /**
       * Get marshaller given a instance to be marshalled or {@code null} if the instance cannot be marshalled by this provider.
       */
      BaseMarshaller<?> getMarshaller(Object instance);

      /**
       * Get a marshaller to unmarshall the supplied type name or @code null} if the type cannot be unmarshalled by this provider.
       */
      BaseMarshaller<?> getMarshaller(String typeName);
   }
}
