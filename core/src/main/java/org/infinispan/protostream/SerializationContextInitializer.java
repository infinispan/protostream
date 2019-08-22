package org.infinispan.protostream;

import java.io.UncheckedIOException;

/**
 * An interface to be used in conjunction with {@link org.infinispan.protostream.annotations.AutoProtoSchemaBuilder}
 * annotation. Just create an abstract class implementing this interface or an interface extending it and annotate it
 * and the compiler (via a custom annotation processor) will generate a concrete implementation of it for you to use.
 * This is a compile-time equivalent of {@link org.infinispan.protostream.annotations.ProtoSchemaBuilder}.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface SerializationContextInitializer {

   /**
    * Returns the name of the proto file (which is allowed to contain slashes, so it could look like a path).
    */
   String getProtoFileName();

   /**
    * Returns the contents of the proto file.
    *
    * @throws UncheckedIOException if the file contents cannot be retrieved
    */
   String getProtoFile() throws UncheckedIOException;

   void registerSchema(SerializationContext serCtx);

   void registerMarshallers(SerializationContext serCtx);
}
