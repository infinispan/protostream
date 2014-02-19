package org.infinispan.protostream;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;

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
   void registerProtofile(InputStream in) throws IOException, DescriptorValidationException;

   void registerProtofile(String classpathResource) throws IOException, DescriptorValidationException;

   void registerProtofile(FileDescriptor fileDescriptor);

   /**
    * Register a type marshaller. The marshaller implementation must be stateless and thread-safe.
    *
    * @param marshaller the marshaller instance
    * @param <T>        the Java type of the object being handled by the marshaller
    */
   <T> void registerMarshaller(BaseMarshaller<T> marshaller);

   Descriptor getMessageDescriptor(String fullName);

   EnumDescriptor getEnumDescriptor(String fullName);

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
