package org.infinispan.protostream.impl;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.infinispan.protostream.EnumEncoder;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.SerializationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author anistor@redhat.com
 */
public final class SerializationContextImpl implements SerializationContext {

   private Map<String, Descriptors.FileDescriptor> fileDescriptors = new HashMap<String, Descriptors.FileDescriptor>();

   private Map<String, Descriptors.Descriptor> messageDescriptors = new HashMap<String, Descriptors.Descriptor>();

   private Map<String, Descriptors.EnumDescriptor> enumDescriptors = new HashMap<String, Descriptors.EnumDescriptor>();

   private Map<Class<?>, MessageMarshaller<?>> marshallersByClass = new HashMap<Class<?>, MessageMarshaller<?>>();
   private Map<String, MessageMarshaller<?>> marshallersByName = new HashMap<String, MessageMarshaller<?>>();

   private Map<Class<?>, EnumEncoder<?>> enumEncodersByClass = new HashMap<Class<?>, EnumEncoder<?>>();
   private Map<String, EnumEncoder<?>> enumEncodersByName = new HashMap<String, EnumEncoder<?>>();

   public SerializationContextImpl() {
      try {
         registerProtofile("/message-wrapping.protobin");
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
   public <T> void registerMarshaller(Class<? extends T> clazz, MessageMarshaller<T> marshaller) {
      getMessageDescriptor(marshaller.getFullName()); // we try to validate that a message descriptor exists
      marshallersByClass.put(clazz, marshaller);
      marshallersByName.put(marshaller.getFullName(), marshaller);
   }

   @Override
   public boolean canMarshall(Class clazz) {
      return Enum.class.isAssignableFrom(clazz) ? enumEncodersByClass.containsKey(clazz) : marshallersByClass.containsKey(clazz);
   }

   @Override
   public <T> MessageMarshaller<T> getMarshaller(String descriptorFullName) {
      MessageMarshaller<T> marshaller = (MessageMarshaller<T>) marshallersByName.get(descriptorFullName);
      if (marshaller == null) {
         throw new IllegalArgumentException("No marshaller registered for " + descriptorFullName);
      }
      return marshaller;
   }

   @Override
   public <T> MessageMarshaller<T> getMarshaller(Class<T> clazz) {
      MessageMarshaller<T> marshaller = (MessageMarshaller<T>) marshallersByClass.get(clazz);
      if (marshaller == null) {
         throw new IllegalArgumentException("No marshaller registered for " + clazz);
      }
      return marshaller;
   }

   @Override
   public <T extends Enum<T>> void registerEnumEncoder(Class<T> clazz, EnumEncoder<T> enumEncoder) {
      getEnumDescriptor(enumEncoder.getFullName());   // we try to validate that an enum descriptor exists
      enumEncodersByClass.put(clazz, enumEncoder);
      enumEncodersByName.put(enumEncoder.getFullName(), enumEncoder);
   }

   @Override
   public <T extends Enum<T>> EnumEncoder<T> getEnumEncoder(String descriptorFullName) {
      EnumEncoder<T> marshaller = (EnumEncoder<T>) enumEncodersByName.get(descriptorFullName);
      if (marshaller == null) {
         throw new IllegalArgumentException("No enum encoder registered for " + descriptorFullName);
      }
      return marshaller;
   }

   @Override
   public <T extends Enum<T>> EnumEncoder<T> getEnumEncoder(Class<T> clazz) {
      EnumEncoder<T> marshaller = (EnumEncoder<T>) enumEncodersByClass.get(clazz);
      if (marshaller == null) {
         throw new IllegalArgumentException("No enum encoder registered for " + clazz);
      }
      return marshaller;
   }
}
