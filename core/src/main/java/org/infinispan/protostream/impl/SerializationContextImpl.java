package org.infinispan.protostream.impl;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.WrappedMessage;

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

   private Map<String, Descriptors.FileDescriptor> fileDescriptors = new ConcurrentHashMap<String, Descriptors.FileDescriptor>();

   private Map<String, Descriptors.Descriptor> messageDescriptors = new ConcurrentHashMap<String, Descriptors.Descriptor>();

   private Map<String, Descriptors.EnumDescriptor> enumDescriptors = new ConcurrentHashMap<String, Descriptors.EnumDescriptor>();

   private Map<Class<?>, BaseMarshaller<?>> marshallersByClass = new ConcurrentHashMap<Class<?>, BaseMarshaller<?>>();

   private Map<String, BaseMarshaller<?>> marshallersByName = new ConcurrentHashMap<String, BaseMarshaller<?>>();

   public SerializationContextImpl() {
      try {
         registerProtofile("/message-wrapping.protobin");
         registerMarshaller(WrappedMessage.class, new WrappedMessageMarshaller());
      } catch (IOException e) {
         e.printStackTrace();  // TODO: Customise this generated block
      } catch (Descriptors.DescriptorValidationException e) {
         e.printStackTrace();  // TODO: Customise this generated block
      }
   }

   private Descriptors.FileDescriptor[] resolveDeps(List<String> dependencyList, Map<String, Descriptors.FileDescriptor> map) {
      List<Descriptors.FileDescriptor> deps = new ArrayList<Descriptors.FileDescriptor>();
      for (String fname : dependencyList) {
         if (map.containsKey(fname)) {
            deps.add(map.get(fname));
         } else if (DescriptorProtos.getDescriptor().getName().equals(fname)) {
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
         messageDescriptors.put(d.getFullName(), d);
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
      Descriptors.Descriptor descriptor = messageDescriptors.get(fullName);
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
   public <T> void registerMarshaller(Class<? extends T> clazz, BaseMarshaller<T> marshaller) {
      // we try to validate first that a message descriptor exists
      if (marshaller instanceof EnumMarshaller) {
         getEnumDescriptor(marshaller.getTypeName());
      } else {
         getMessageDescriptor(marshaller.getTypeName());
      }
      marshallersByClass.put(clazz, marshaller);
      marshallersByName.put(marshaller.getTypeName(), marshaller);
   }

   @Override
   public boolean canMarshall(Class clazz) {
      return marshallersByClass.containsKey(clazz);
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
