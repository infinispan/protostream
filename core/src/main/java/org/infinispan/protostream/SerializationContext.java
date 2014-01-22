package org.infinispan.protostream;

import com.google.protobuf.Descriptors;

import java.io.IOException;
import java.io.InputStream;

/**
 * A repository for protobuf definitions and marshallers. All marshalling operations happen in the context of a  {@code
 * SerializationContext}.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface SerializationContext { //todo [anistor] split this into separate immutable/mutable interfaces

   Configuration getConfiguration();

   /**
    * Registers a protobuf file descriptor given in the form of an input stream.
    * <p/>
    * WARNING: This method does not close the input stream when.
    *
    * @param in
    */
   void registerProtofile(InputStream in) throws IOException, Descriptors.DescriptorValidationException;

   void registerProtofile(String classpathResource) throws IOException, Descriptors.DescriptorValidationException;

   void registerProtofile(Descriptors.FileDescriptor fileDescriptor);

   /**
    * Register a type marshaller. The marshaller implementation must be stateless and thread-safe.
    *
    * @param marshaller the marshaller instance
    * @param <T>        the Java type of the object being handled by the marshaller
    */
   <T> void registerMarshaller(BaseMarshaller<T> marshaller);

   /**
    * Register a type marshaller. The marshaller implementation must be stateless and thread-safe.
    *
    * @param clazz      this parameter is currently ignored, see {@link SerializationContext#registerMarshaller(BaseMarshaller<T>)}
    * @param marshaller the marshaller instance
    * @param <T>        the Java type of the object being handled by the marshaller
    * @deprecated This method will be removed in a future version. Use {@link SerializationContext#registerMarshaller(BaseMarshaller<T>)}
    *             instead.
    */
   @Deprecated
   <T> void registerMarshaller(Class<? extends T> clazz, BaseMarshaller<T> marshaller);

   Descriptors.Descriptor getMessageDescriptor(String fullName);

   Descriptors.EnumDescriptor getEnumDescriptor(String fullName);

   /**
    * Checks if the given type (message or enum) can be marshalled. This checks that a marshaller was registered for
    * it.
    *
    * @param clazz the object or enum class to check
    * @return {@code true} if a marshaller exists, {@code false} otherwise
    */
   boolean canMarshall(Class clazz);

   /**
    * Checks if the given type (message or enum) can be marshalled. This checks that the type name was defined and a
    * marshaller was registered for it.
    *
    * @param descriptorFullName the fully qualified name of the protobuf definition to check
    * @return {@code true} if a marshaller exists, {@code false} otherwise
    */
   boolean canMarshall(String descriptorFullName);

   <T> BaseMarshaller<T> getMarshaller(String descriptorFullName);

   <T> BaseMarshaller<T> getMarshaller(Class<T> clazz);
}
