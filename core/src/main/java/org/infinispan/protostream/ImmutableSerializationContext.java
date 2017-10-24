package org.infinispan.protostream;

import java.util.Map;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;

/**
 * @author anistor@redhat.com
 * @since 4.0
 */
public interface ImmutableSerializationContext {

   /**
    * Obtain the currently registered file descriptors.
    *
    * @return an immutable copy of the internal map of descriptors
    */
   Map<String, FileDescriptor> getFileDescriptors();

   Descriptor getMessageDescriptor(String fullName);

   EnumDescriptor getEnumDescriptor(String fullName);

   /**
    * Checks if the given type (message or enum) can be marshalled. This checks that a marshaller was registered for
    * it.
    *
    * @param clazz the object or enum class to check
    * @return {@code true} if a marshaller exists, {@code false} otherwise
    */
   boolean canMarshall(Class<?> clazz);

   /**
    * Checks if the given type (message or enum) can be marshalled. This checks that the type name was defined and a
    * marshaller was registered for it.
    *
    * @param fullName the fully qualified name of the protobuf definition to check
    * @return {@code true} if a marshaller exists, {@code false} otherwise
    */
   boolean canMarshall(String fullName);

   <T> BaseMarshaller<T> getMarshaller(String fullName);

   <T> BaseMarshaller<T> getMarshaller(Class<T> clazz);

   /**
    * Obtains the type name associated with a numeric type id.
    *
    * @param typeId the numeric type id
    * @return the fully qualified type name
    * @throws IllegalArgumentException if the given type id is unknown
    */
   String getTypeNameById(Integer typeId);

   /**
    * Obtains the associated numeric type id, if one was defined.
    *
    * @param fullName the fully qualified type name
    * @return the type id or {@code null} if no type id is associated with the type
    * @throws IllegalArgumentException if the given type name is unknown
    */
   Integer getTypeIdByName(String fullName);

   /**
    * Obtains the type name associated with a numeric type id.
    *
    * @param typeId the numeric type id
    * @return the fully qualified type name
    * @throws IllegalArgumentException if the given type id is unknown
    */
   GenericDescriptor getDescriptorByTypeId(Integer typeId);

   GenericDescriptor getDescriptorByName(String fullName);
}
