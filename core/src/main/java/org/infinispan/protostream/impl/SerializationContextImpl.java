package org.infinispan.protostream.impl;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.Configuration;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.DescriptorParser;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.impl.parser.SquareProtoParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author anistor@redhat.com
 */
public final class SerializationContextImpl implements SerializationContext {

   private final Configuration configuration;

   private final DescriptorParser parser = new SquareProtoParser();

   private Map<String, FileDescriptor> fileDescriptors = new ConcurrentHashMap<>();

   private Map<String, Descriptor> messageDescriptors = new ConcurrentHashMap<>();

   private Map<String, EnumDescriptor> enumDescriptors = new ConcurrentHashMap<>();

   private Map<String, BaseMarshallerDelegate<?>> marshallersByName = new ConcurrentHashMap<>();

   private Map<Class<?>, BaseMarshallerDelegate<?>> marshallersByClass = new ConcurrentHashMap<>();

   public SerializationContextImpl(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public Configuration getConfiguration() {
      return configuration;
   }

   @Override
   public void registerProtoFiles(FileDescriptorSource source) throws DescriptorParserException, IOException {
      Map<String, FileDescriptor> parse = parser.parse(source);
      for (FileDescriptor d : parse.values()) {
         registerProtofile(d);
      }
   }

   @Override
   public void registerProtofiles(String... classpathResource) throws IOException, DescriptorParserException {
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFiles(classpathResource);
      this.registerProtoFiles(fileDescriptorSource);
   }

   public void registerProtofile(FileDescriptor fileDescriptor) {
      fileDescriptors.put(fileDescriptor.getName(), fileDescriptor);
      registerMessageDescriptors(fileDescriptor.getMessageTypes());
      registerEnumDescriptors(fileDescriptor.getEnumTypes());
   }

   private void registerMessageDescriptors(List<Descriptor> messageTypes) {
      for (Descriptor d : messageTypes) {
         messageDescriptors.put(d.getFullName(), d);
         registerMessageDescriptors(d.getNestedTypes());
         registerEnumDescriptors(d.getEnumTypes());
      }
   }

   private void registerEnumDescriptors(List<EnumDescriptor> enumTypes) {
      for (EnumDescriptor e : enumTypes) {
         enumDescriptors.put(e.getFullName(), e);
      }
   }

   @Override
   public Descriptor getMessageDescriptor(String fullName) {
      Descriptor descriptor = messageDescriptors.get(fullName);
      if (descriptor == null) {
         throw new IllegalArgumentException("Message descriptor not found : " + fullName);
      }
      return descriptor;
   }

   @Override
   public EnumDescriptor getEnumDescriptor(String fullName) {
      EnumDescriptor descriptor = enumDescriptors.get(fullName);
      if (descriptor == null) {
         throw new IllegalArgumentException("Enum descriptor not found : " + fullName);
      }
      return descriptor;
   }

   @Override
   public <T> void registerMarshaller(BaseMarshaller<T> marshaller) {
      // we try to validate first that a message descriptor exists
      BaseMarshallerDelegate marshallerDelegate;
      if (marshaller instanceof EnumMarshaller) {
         if (!Enum.class.isAssignableFrom(marshaller.getJavaClass())) {
            throw new IllegalArgumentException("Invalid enum marshaller (the produced class is not an Enum) : " + marshaller);
         }
         EnumDescriptor enumDescriptor = getEnumDescriptor(marshaller.getTypeName());
         marshallerDelegate = new EnumMarshallerDelegate((EnumMarshaller) marshaller, enumDescriptor);
      } else if (marshaller instanceof RawProtobufMarshaller) {
         marshallerDelegate = new RawProtobufMarshallerDelegate((RawProtobufMarshaller) marshaller, this);
      } else {
         Descriptor messageDescriptor = getMessageDescriptor(marshaller.getTypeName());
         marshallerDelegate = new MessageMarshallerDelegate((MessageMarshaller) marshaller, messageDescriptor);
      }
      marshallersByName.put(marshaller.getTypeName(), marshallerDelegate);
      marshallersByClass.put(marshaller.getJavaClass(), marshallerDelegate);
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
      return this.<T>getMarshallerDelegate(descriptorFullName).getMarshaller();
   }

   @Override
   public <T> BaseMarshaller<T> getMarshaller(Class<T> clazz) {
      return getMarshallerDelegate(clazz).getMarshaller();
   }

   <T> BaseMarshallerDelegate<T> getMarshallerDelegate(String descriptorFullName) {
      BaseMarshallerDelegate<T> marshallerDelegate = (BaseMarshallerDelegate<T>) marshallersByName.get(descriptorFullName);
      if (marshallerDelegate == null) {
         throw new IllegalArgumentException("No marshaller registered for " + descriptorFullName);
      }
      return marshallerDelegate;
   }

   <T> BaseMarshallerDelegate<T> getMarshallerDelegate(Class<T> clazz) {
      BaseMarshallerDelegate<T> marshallerDelegate = (BaseMarshallerDelegate<T>) marshallersByClass.get(clazz);
      if (marshallerDelegate == null) {
         throw new IllegalArgumentException("No marshaller registered for " + clazz);
      }
      return marshallerDelegate;
   }
}
