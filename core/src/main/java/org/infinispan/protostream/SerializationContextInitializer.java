package org.infinispan.protostream;

import java.io.UncheckedIOException;

/**
 * An interface to be usually used in conjunction with the {@link org.infinispan.protostream.annotations.AutoProtoSchemaBuilder}
 * annotation. By creating an abstract class implementing this interface or an interface extending it and annotating it with
 * {@link org.infinispan.protostream.annotations.AutoProtoSchemaBuilder} the compiler (via a custom annotation processor)
 * will generate a concrete implementation for you to use. This mechanism is a compile-time alternative to
 * {@link org.infinispan.protostream.annotations.ProtoSchemaBuilder} which works using runtime bytecode generation.
 * <p>
 * Manually written implementations of this interface are also allowed, but are of no special use with regard to
 * ProtoStream library.
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
    * Returns the contents of the proto file as a {@link String}. The returned value must be the same (equals) on each
    * invocation. Implementations can return a constant or a value stored in memory but they are generally free to
    * retrieve it from somewhere else, including the classpath, the disk, or even a mechanism that can potentially fail
    * with an {@link UncheckedIOException}.
    *
    * @throws UncheckedIOException if the file contents cannot be retrieved
    */
   String getProtoFile() throws UncheckedIOException;

   void registerSchema(SerializationContext serCtx);

   void registerMarshallers(SerializationContext serCtx);
}
