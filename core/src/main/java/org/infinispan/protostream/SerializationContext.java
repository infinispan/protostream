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
}
