package org.infinispan.protostream;

import com.google.protobuf.Descriptors;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author anistor@redhat.com
 */
public interface SerializationContext {

   /**
    * The input stream is not closed when finished.
    *
    * @param in
    */
   void registerProtofile(InputStream in) throws IOException, Descriptors.DescriptorValidationException;

   void registerProtofile(String classpathResource) throws IOException, Descriptors.DescriptorValidationException;

   void registerProtofile(Descriptors.FileDescriptor fileDescriptor);

   Descriptors.Descriptor getMessageDescriptor(String fullName);

   Descriptors.EnumDescriptor getEnumDescriptor(String fullName);

   <T> void registerMarshaller(Class<? extends T> clazz, BaseMarshaller<T> marshaller);

   /**
    * Checks if the message or enum type can be marshalled (a marshaller is defined for it).
    *
    * @param clazz
    * @return
    */
   boolean canMarshall(Class clazz);

   <T> BaseMarshaller<T> getMarshaller(String descriptorFullName);

   <T> BaseMarshaller<T> getMarshaller(Class<T> clazz);
}
