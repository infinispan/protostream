package org.infinispan.protostream.impl;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.Configuration;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.SerializationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author anistor@redhat.com
 */
public final class SerializationContextImpl implements SerializationContext {

   private final Configuration configuration;

   private Map<String, Descriptors.FileDescriptor> fileDescriptors = new ConcurrentHashMap<String, Descriptors.FileDescriptor>();

   private Map<String, MessageDescriptor> messageDescriptors = new ConcurrentHashMap<String, MessageDescriptor>();

   private Map<String, Descriptors.EnumDescriptor> enumDescriptors = new ConcurrentHashMap<String, Descriptors.EnumDescriptor>();

   private Map<Class<?>, BaseMarshaller<?>> marshallersByClass = new ConcurrentHashMap<Class<?>, BaseMarshaller<?>>();

   private Map<String, BaseMarshaller<?>> marshallersByName = new ConcurrentHashMap<String, BaseMarshaller<?>>();

   public SerializationContextImpl(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public Configuration getConfiguration() {
      return configuration;
   }

   private Descriptors.FileDescriptor[] resolveDeps(List<String> dependencyList, Map<String, Descriptors.FileDescriptor> map) {
      List<Descriptors.FileDescriptor> deps = new ArrayList<Descriptors.FileDescriptor>();
      for (String fileName : dependencyList) {
         if (map.containsKey(fileName)) {
            deps.add(map.get(fileName));
         } else if (DescriptorProtos.getDescriptor().getName().equals(fileName)) {
            deps.add(DescriptorProtos.getDescriptor());
         }
      }
      return deps.toArray(new Descriptors.FileDescriptor[deps.size()]);
   }

   @Override
   public void registerProtofile(InputStream in) throws IOException, Descriptors.DescriptorValidationException {
      DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(in);

      for (DescriptorProtos.FileDescriptorProto fdp : descriptorSet.getFileList()) {
         Descriptors.FileDescriptor[] deps = resolveDeps(fdp.getDependencyList(), fileDescriptors);
         Descriptors.FileDescriptor fd = Descriptors.FileDescriptor.buildFrom(fdp, deps);
         registerProtofile(fd);
      }
   }

   @Override
   public void registerProtofile(String classpathResource) throws IOException, Descriptors.DescriptorValidationException {
      InputStream in = getClass().getResourceAsStream(classpathResource);
      if (in == null) {
         throw new IOException("Resource \"" + classpathResource + "\" does not exist");
      }
      try {
         registerProtofile(in);
      } finally {
         in.close();
      }
   }

   @Override
   public void registerProtofile(Descriptors.FileDescriptor fileDescriptor) {
      fileDescriptors.put(fileDescriptor.getName(), fileDescriptor);
      registerMessageDescriptors(fileDescriptor.getMessageTypes());
      registerEnumDescriptors(fileDescriptor.getEnumTypes());
   }

   private void registerMessageDescriptors(List<Descriptors.Descriptor> messageTypes) {
      for (Descriptors.Descriptor d : messageTypes) {
         messageDescriptors.put(d.getFullName(), new MessageDescriptor(d));
         registerMessageDescriptors(d.getNestedTypes());
         registerEnumDescriptors(d.getEnumTypes());
      }
   }

   private void registerEnumDescriptors(List<Descriptors.EnumDescriptor> enumTypes) {
      for (Descriptors.EnumDescriptor e : enumTypes) {
         enumDescriptors.put(e.getFullName(), e);
      }
   }

   @Override
   public Descriptors.Descriptor getMessageDescriptor(String fullName) {
      MessageDescriptor descriptor = messageDescriptors.get(fullName);
      if (descriptor == null) {
         throw new IllegalArgumentException("Message descriptor not found : " + fullName);
      }
      return descriptor.getMessageDescriptor();
   }

   public MessageDescriptor getInternalMessageDescriptor(String fullName) {
      MessageDescriptor descriptor =  messageDescriptors.get(fullName);
      if (descriptor == null) {
         throw new IllegalArgumentException("Message descriptor not found : " + fullName);
      }
      return descriptor;
   }

   @Override
   public Descriptors.EnumDescriptor getEnumDescriptor(String fullName) {
      Descriptors.EnumDescriptor descriptor = enumDescriptors.get(fullName);
      if (descriptor == null) {
         throw new IllegalArgumentException("Enum descriptor not found : " + fullName);
      }
      return descriptor;
   }

   @Override
   public <T> void registerMarshaller(BaseMarshaller<T> marshaller) {
      //TODO [anistor] here we should check if marshaller.getJavaType() is a supported type. some might not be allowed by our framework

      // we try to validate first that a message descriptor exists
      if (marshaller instanceof EnumMarshaller) {
         getEnumDescriptor(marshaller.getTypeName());
      } else {
         getMessageDescriptor(marshaller.getTypeName());
      }
      marshallersByClass.put(marshaller.getJavaClass(), marshaller);
      marshallersByName.put(marshaller.getTypeName(), marshaller);
   }

   @Override
   public <T> void registerMarshaller(Class<? extends T> clazz, BaseMarshaller<T> marshaller) {
      if (!marshaller.getJavaClass().equals(clazz)) {
         throw new IllegalArgumentException("The given class must match the class reported by the marshaller.");
      }
      registerMarshaller(marshaller);
   }

   @Override
   public boolean canMarshall(Class clazz) {
      return marshallersByClass.containsKey(clazz);
   }

   @Override
   public boolean canMarshall(String descriptorFullName) {
      return messageDescriptors.containsKey(descriptorFullName) || enumDescriptors.containsKey(descriptorFullName);
      //TODO the correct implementation should be: return marshallersByName.containsKey(descriptorFullName);
   }

   @Override
   public <T> BaseMarshaller<T> getMarshaller(String descriptorFullName) {
      BaseMarshaller<T> marshaller = (BaseMarshaller<T>) marshallersByName.get(descriptorFullName);
      if (marshaller == null) {
         throw new IllegalArgumentException("No marshaller registered for " + descriptorFullName);
      }
      return marshaller;
   }

   @Override
   public <T> BaseMarshaller<T> getMarshaller(Class<T> clazz) {
      BaseMarshaller<T> marshaller = (BaseMarshaller<T>) marshallersByClass.get(clazz);
      if (marshaller == null) {
         throw new IllegalArgumentException("No marshaller registered for " + clazz);
      }
      return marshaller;
   }
}
