package org.infinispan.protostream.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.DescriptorParser;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.descriptors.ResolutionContext;
import org.infinispan.protostream.impl.parser.SquareProtoParser;

import net.jcip.annotations.GuardedBy;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class SerializationContextImpl implements SerializationContext {

   private static final Log log = Log.LogFactory.getLog(SerializationContextImpl.class);

   /**
    * All state is protected by this RW lock.
    */
   private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

   private final Lock readLock = readWriteLock.readLock();

   private final Lock writeLock = readWriteLock.writeLock();

   private final Configuration configuration;

   private final DescriptorParser parser;

   private final Map<String, FileDescriptor> fileDescriptors = new LinkedHashMap<>();

   private final Map<Integer, GenericDescriptor> typeIds = new HashMap<>();

   private final Map<String, GenericDescriptor> genericDescriptors = new HashMap<>();

   private final Map<String, EnumValueDescriptor> enumValueDescriptors = new HashMap<>();

   private final Map<String, BaseMarshallerDelegate<?>> marshallersByName = new ConcurrentHashMap<>();

   private final Map<Class<?>, BaseMarshallerDelegate<?>> marshallersByClass = new ConcurrentHashMap<>();

   public SerializationContextImpl(Configuration configuration) {
      if (configuration == null) {
         throw new IllegalArgumentException("configuration cannot be null");
      }
      this.configuration = configuration;
      parser = new SquareProtoParser(configuration);
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

         // clear errors and put in unresolved state whatever is not resolved already
         for (FileDescriptor fileDescriptor : fileDescriptors.values()) {
            fileDescriptor.clearErrors();
         }

         // resolve imports and types for all files
         ResolutionContext resolutionContext = new ResolutionContext(source.getProgressCallback(), fileDescriptors, genericDescriptors, typeIds, enumValueDescriptors);
         for (FileDescriptor fileDescriptor : fileDescriptors.values()) {
            fileDescriptor.resolveDependencies(resolutionContext);
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
   public void unregisterProtoFile(String fileName) {
      log.debugf("Unregistering proto file : %s", fileName);
      writeLock.lock();
      try {
         FileDescriptor fileDescriptor = fileDescriptors.remove(fileName);
         if (fileDescriptor != null) {
            unregisterFileDescriptorTypes(fileDescriptor);
         } else {
            throw new IllegalArgumentException("File " + fileName + " does not exist");
         }
      } finally {
         writeLock.unlock();
      }
   }

   @GuardedBy("writeLock")
   private void unregisterFileDescriptorTypes(FileDescriptor fileDescriptor) {
      if (fileDescriptor.isResolved()) {
         for (GenericDescriptor d : fileDescriptor.getTypes().values()) {
            Integer typeId = d.getTypeId();
            if (typeId != null) {
               typeIds.remove(typeId);
            }
            if (d instanceof EnumDescriptor) {
               for (EnumValueDescriptor ev : ((EnumDescriptor) d).getValues()) {
                  enumValueDescriptors.remove(ev.getScopedName());
               }
            }
         }
         genericDescriptors.keySet().removeAll(fileDescriptor.getTypes().keySet());
         fileDescriptor.markUnresolved();
      }
      for (FileDescriptor fd : fileDescriptor.getDependants().values()) {
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
         if (!marshaller.getJavaClass().isEnum()) {
            throw new IllegalArgumentException("Invalid enum marshaller (the produced class is not an Enum) : " + marshaller);
         }
         EnumDescriptor enumDescriptor = getEnumDescriptor(marshaller.getTypeName());
         marshallerDelegate = new EnumMarshallerDelegate((EnumMarshaller) marshaller, enumDescriptor);
      } else if (marshaller instanceof RawProtobufMarshaller) {
         marshallerDelegate = new RawProtobufMarshallerDelegate(this, (RawProtobufMarshaller) marshaller);
      } else {
         Descriptor messageDescriptor = getMessageDescriptor(marshaller.getTypeName());
         marshallerDelegate = new MessageMarshallerDelegate(this, (MessageMarshaller) marshaller, messageDescriptor);
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

   public <T> BaseMarshallerDelegate<T> getMarshallerDelegate(String descriptorFullName) {
      BaseMarshallerDelegate<T> marshallerDelegate = (BaseMarshallerDelegate<T>) marshallersByName.get(descriptorFullName);
      if (marshallerDelegate == null) {
         throw new IllegalArgumentException("No marshaller registered for " + descriptorFullName);
      }
      return marshallerDelegate;
   }

   public <T> BaseMarshallerDelegate<T> getMarshallerDelegate(Class<T> clazz) {
      BaseMarshallerDelegate<T> marshallerDelegate = (BaseMarshallerDelegate<T>) marshallersByClass.get(clazz);
      if (marshallerDelegate == null) {
         throw new IllegalArgumentException("No marshaller registered for " + clazz);
      }
      return marshallerDelegate;
   }

   @Override
   public String getTypeNameById(Integer typeId) {
      return getDescriptorByTypeId(typeId).getFullName();
   }

   @Override
   public GenericDescriptor getDescriptorByName(String fullName) {
      GenericDescriptor descriptor;
      readLock.lock();
      try {
         descriptor = genericDescriptors.get(fullName);
      } finally {
         readLock.unlock();
      }
      if (descriptor == null) {
         throw new IllegalArgumentException("Descriptor not found : " + fullName);
      }
      return descriptor;
   }

   @Override
   public GenericDescriptor getDescriptorByTypeId(Integer typeId) {
      readLock.lock();
      try {
         GenericDescriptor descriptorFullName = typeIds.get(typeId);
         if (descriptorFullName == null) {
            throw new IllegalArgumentException("Unknown type id : " + typeId);
         }
         return descriptorFullName;
      } finally {
         readLock.unlock();
      }
   }

   @Override
   public Integer getTypeIdByName(String descriptorFullName) {
      readLock.lock();
      try {
         GenericDescriptor descriptor = genericDescriptors.get(descriptorFullName);
         if (descriptor == null) {
            throw new IllegalArgumentException("Unknown type name : " + descriptorFullName);
         }
         return descriptor.getTypeId();
      } finally {
         readLock.unlock();
      }
   }
}
