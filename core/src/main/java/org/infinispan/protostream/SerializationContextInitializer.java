package org.infinispan.protostream;

import java.io.IOException;

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

   String getProtoFileName();

   String getProtoFile(); //todo IOException

   void registerSchema(ClassLoader classLoader, SerializationContext serCtx) throws IOException;

   void registerSchema(SerializationContext serCtx) throws IOException;

   void registerMarshallers(SerializationContext serCtx);
}
