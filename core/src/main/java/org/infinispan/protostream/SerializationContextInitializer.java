package org.infinispan.protostream;

import java.io.UncheckedIOException;

/**
 * An interface to be used in conjunction with the {@link org.infinispan.protostream.annotations.AutoProtoSchemaBuilder}
 * annotation. By creating an abstract class implementing this interface or an interface extending it and annotating it
 * with {@link org.infinispan.protostream.annotations.AutoProtoSchemaBuilder} the compiler (via a custom annotation
 * processor) will generate a concrete implementation for you to use. The implementation will provide a single schema
 * file that is generated based on the annotations and also marshallers for all the contained types.
 * <p>
 * You can also use the sub-interface {@link GeneratedSchema} instead, which provides methods to access the generated
 * schema file.
 * <p>
 * Manually written implementations of these interfaces are allowed, but are of no special use with regard to
 * ProtoStream library.
 * <p>
 * This mechanism is very similar to {@link org.infinispan.protostream.annotations.ProtoSchemaBuilder} which works using
 * bytecode generation at runtime instead of compile time.
 * <p>
 * <em>NOTE:</em> Methods {@link #getProtoFileName()} and {@link #getProtoFile()} will be removed from this interface in
 * ver. 5 but will continue to exist in {@link GeneratedSchema}, which extends this interface.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface SerializationContextInitializer {

   /**
    * Returns the name of the proto file (which is allowed to contain slashes, so it could look like a path).
    *
    * @deprecated in 4.3.4, to be removed in 5. The method was moved and will continue to exist starting with ver. 5
    * as {@link GeneratedSchema#getProtoFileName()} (see IPROTO-154).
    */
   @Deprecated
   String getProtoFileName();

   /**
    * Returns the contents of the proto file as a {@link String}. The returned value must be guaranteed to be the same
    * (equals) on each invocation. Implementations can return a constant or a value stored in memory but they are
    * generally free to also retrieve it from somewhere else, including the classpath, the disk, or even a mechanism
    * that can potentially fail with an {@link UncheckedIOException}.
    *
    * @throws UncheckedIOException if the file contents cannot be retrieved
    * @deprecated in 4.3.4, to be removed in 5. The method was moved and will continue to exist starting with ver. 5
    * as {@link GeneratedSchema#getProtoFile()} (see IPROTO-154).
    */
   @Deprecated
   String getProtoFile() throws UncheckedIOException;

   /**
    * Registers schema files to the given {@link SerializationContext}. This is always invoked before
    * {@link #registerMarshallers}.
    */
   void registerSchema(SerializationContext serCtx);

   /**
    * Registers marshallers to the given {@link SerializationContext}.This is always invoked after
    * {@link #registerSchema}.
    */
   void registerMarshallers(SerializationContext serCtx);
}
