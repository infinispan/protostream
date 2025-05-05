package org.infinispan.protostream;

/**
 * An interface to be used in conjunction with the {@link org.infinispan.protostream.annotations.ProtoSchema}
 * annotation. By creating an abstract class implementing this interface or an interface extending it and annotating it
 * with {@link org.infinispan.protostream.annotations.ProtoSchema} the compiler (via a custom annotation
 * processor) will generate a concrete implementation for you to use. The implementation will provide a single schema
 * file that is generated based on the annotations and also marshallers for all the contained types.
 * <p>
 * You can also use the sub-interface {@link GeneratedSchema} instead, which provides methods to access the generated
 * schema file.
 * </p>
 * <p>
 * Manually written implementations of these interfaces are allowed but are of no special use with regard to the
 * ProtoStream library.
 * </p>
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface SerializationContextInitializer {

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
