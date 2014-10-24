package org.infinispan.protostream.impl;

import net.jcip.annotations.GuardedBy;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.Configuration;
import org.infinispan.protostream.DescriptorParser;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.impl.parser.SquareProtoParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author anistor@redhat.com
 */
public final class SerializationContextImpl implements SerializationContext {

   private static final Log log = Log.LogFactory.getLog(SerializationContextImpl.class);

   private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

   private final Lock readLock = readWriteLock.readLock();

   private final Lock writeLock = readWriteLock.writeLock();

   private final Configuration configuration;

   private final DescriptorParser parser = new SquareProtoParser();

   private final Map<String, FileDescriptor> fileDescriptors = new HashMap<>();

   private final Map<String, GenericDescriptor> genericDescriptors = new HashMap<>();

   private final Map<String, BaseMarshallerDelegate<?>> marshallersByName = new ConcurrentHashMap<>();

   private final Map<Class<?>, BaseMarshallerDelegate<?>> marshallersByClass = new ConcurrentHashMap<>();

   public SerializationContextImpl(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public Configuration getConfiguration() {
      return configuration;
   }

   public Map<String, FileDescriptor> getFileDescriptors() {
      readLock.lock();
      try {
         return new HashMap<>(fileDescriptors);
      } finally {
         readLock.unlock();
      }
   }

   @Override
   public void registerProtoFiles(FileDescriptorSource source) throws IOException, DescriptorParserException {
      if (log.isDebugEnabled()) {
         log.debugf("Registering proto files : %s", source.getFileDescriptors().keySet());
      }
      Map<String, FileDescriptor> fileDescriptorMap = parser.parse(source);
      writeLock.lock();
      try {
         // unregister all types from the files that are being overwritten
         for (String fileName : fileDescriptorMap.keySet()) {
            FileDescriptor oldFileDescriptor = fileDescriptors.get(fileName);
            if (oldFileDescriptor != null) {
               unregisterFileDescriptorTypes(oldFileDescriptor);
            }
         }
         fileDescriptors.putAll(fileDescriptorMap);
         // resolve imports and types
         for (FileDescriptor fileDescriptor : fileDescriptors.values()) {
            if (fileDescriptor.resolveDependencies(source.getProgressCallback(), fileDescriptors, genericDescriptors)) {
               registerFileDescriptor(fileDescriptor);
            }
         }
         // clear errors and leave in unresolved state whatever could not be resolved
         for (FileDescriptor fileDescriptor : fileDescriptors.values()) {
            fileDescriptor.clearErrors();
         }
      } finally {
         writeLock.unlock();
      }
   }

   @Override
   public void registerProtoFiles(String... classpathResource) throws IOException, DescriptorParserException {
      FileDescriptorSource fileDescriptorSource = new FileDescriptorSource();
      fileDescriptorSource.addProtoFiles(classpathResource);
      registerProtoFiles(fileDescriptorSource);
   }

   @Override
   public void unregisterProtoFile(String fileName) {
      log.debugf("Unregistering proto file : %s", fileName);
      writeLock.lock();
      try {
         FileDescriptor fileDescriptor = fileDescriptors.get(fileName);
         if (fileDescriptor != null) {
            fileDescriptors.remove(fileDescriptor.getName());
            unregisterFileDescriptorTypes(fileDescriptor);
         }
      } finally {
         writeLock.unlock();
      }
   }

   @GuardedBy("writeLock")
   private void registerFileDescriptor(FileDescriptor fileDescriptor) {
      if (log.isDebugEnabled()) {
         log.debugf("Registering file descriptor : fileName=%s types=%s", fileDescriptor.getName(), fileDescriptor.getTypes().keySet());
      }
      fileDescriptors.put(fileDescriptor.getName(), fileDescriptor);
      genericDescriptors.putAll(fileDescriptor.getTypes());
   }

   @GuardedBy("writeLock")
   private void unregisterFileDescriptorTypes(FileDescriptor fileDescriptor) {
      genericDescriptors.keySet().removeAll(fileDescriptor.getTypes().keySet());
      for (FileDescriptor fd : fileDescriptor.getDependants().values()) {
         fd.markUnresolved();
         unregisterFileDescriptorTypes(fd);
      }
   }

   @Override
   public Descriptor getMessageDescriptor(String fullName) {
      readLock.lock();
      try {
         GenericDescriptor descriptor = genericDescriptors.get(fullName);
         if (descriptor == null) {
            throw new IllegalArgumentException("Message descriptor not found : " + fullName);
         }
         if (!(descriptor instanceof Descriptor)) {
            throw new IllegalArgumentException(fullName + " is not a message type");
         }
         return (Descriptor) descriptor;
      } finally {
         readLock.unlock();
      }
   }

   @Override
   public EnumDescriptor getEnumDescriptor(String fullName) {
      readLock.lock();
      try {
         GenericDescriptor descriptor = genericDescriptors.get(fullName);
         if (descriptor == null) {
            throw new IllegalArgumentException("Enum descriptor not found : " + fullName);
         }
         if (!(descriptor instanceof EnumDescriptor)) {
            throw new IllegalArgumentException(fullName + " is not an enum type");
         }
         return (EnumDescriptor) descriptor;
      } finally {
         readLock.unlock();
      }
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
      readLock.lock();
      try {
         return genericDescriptors.containsKey(descriptorFullName);
         //TODO the correct implementation should be: return marshallersByName.containsKey(descriptorFullName);
      } finally {
         readLock.unlock();
      }
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
