package org.infinispan.protostream.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
    * All mutable internal state is protected by this RW lock.
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

   private final Map<MarshallerProvider, MarshallerProvider> marshallerProviders = new ConcurrentHashMap<>();

   public SerializationContextImpl(Configuration configuration) {
      if (configuration == null) {
         throw new IllegalArgumentException("configuration argument cannot be null");
      }
      this.configuration = configuration;
      parser = new SquareProtoParser(configuration);
   }

   @Override
   public Configuration getConfiguration() {
      return configuration;
   }

   @Override
   public Map<String, FileDescriptor> getFileDescriptors() {
      readLock.lock();
      try {
         return Collections.unmodifiableMap(new HashMap<>(fileDescriptors));
      } finally {
         readLock.unlock();
      }
   }

   @Override
   public Map<String, GenericDescriptor> getGenericDescriptors() {
      readLock.lock();
      try {
         return Collections.unmodifiableMap(new HashMap<>(genericDescriptors));
      } finally {
         readLock.unlock();
      }
   }

   @Override
   public void registerProtoFiles(FileDescriptorSource source) throws DescriptorParserException {
      if (log.isDebugEnabled()) {
         log.debugf("Registering proto files : %s", source.getFiles().keySet());
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

         // resolve imports and types for all files
         ResolutionContext resolutionContext = new ResolutionContext(source.getProgressCallback(), fileDescriptors, genericDescriptors, typeIds, enumValueDescriptors);
         resolutionContext.resolve();
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

   @Override
   public void unregisterProtoFiles(Set<String> fileNames) {
      log.debugf("Unregistering proto files : %s", fileNames);
      writeLock.lock();
      try {
         for (String fileName : fileNames) {
            FileDescriptor fileDescriptor = fileDescriptors.remove(fileName);
            if (fileDescriptor != null) {
               unregisterFileDescriptorTypes(fileDescriptor);
            } else {
               throw new IllegalArgumentException("File " + fileName + " does not exist");
            }
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
   public Descriptor getMessageDescriptor(String fullTypeName) {
      GenericDescriptor descriptor = getDescriptorByName(fullTypeName);
      if (!(descriptor instanceof Descriptor)) {
         throw new IllegalArgumentException(fullTypeName + " is not a message type");
      }
      return (Descriptor) descriptor;
   }

   @Override
   public EnumDescriptor getEnumDescriptor(String fullTypeName) {
      GenericDescriptor descriptor = getDescriptorByName(fullTypeName);
      if (!(descriptor instanceof EnumDescriptor)) {
         throw new IllegalArgumentException(fullTypeName + " is not an enum type");
      }
      return (EnumDescriptor) descriptor;
   }

   @Override
   public void registerMarshaller(BaseMarshaller<?> marshaller) {
      if (marshaller == null) {
         throw new IllegalArgumentException("marshaller argument cannot be null");
      }
      BaseMarshallerDelegate<?> marshallerDelegate = makeMarshallerDelegate(marshaller);
      BaseMarshallerDelegate<?> existingDelegate = marshallersByName.put(marshaller.getTypeName(), marshallerDelegate);
      if (existingDelegate != null) {
         marshallersByClass.remove(existingDelegate.getMarshaller().getJavaClass());
      }
      existingDelegate = marshallersByClass.put(marshaller.getJavaClass(), marshallerDelegate);
      if (existingDelegate != null) {
         marshallersByName.remove(existingDelegate.getMarshaller().getTypeName());
      }
   }

   private BaseMarshallerDelegate<?> makeMarshallerDelegate(BaseMarshaller<?> marshaller) {
      if (marshaller.getJavaClass().isEnum() && !(marshaller instanceof EnumMarshaller)) {
         throw new IllegalArgumentException("Invalid marshaller (the produced class is a Java Enum but the marshaller is not an EnumMarshaller) : " + marshaller.getClass().getName());
      }
      // we try to validate first that a message descriptor exists
      if (marshaller instanceof EnumMarshaller) {
         if (!marshaller.getJavaClass().isEnum()) {
            throw new IllegalArgumentException("Invalid enum marshaller (the produced class is not a Java Enum) : " + marshaller.getClass().getName());
         }
         EnumDescriptor enumDescriptor = getEnumDescriptor(marshaller.getTypeName());
         return new EnumMarshallerDelegate<>((EnumMarshaller<?>) marshaller, enumDescriptor);
      } else if (marshaller instanceof RawProtobufMarshaller) {
         return new RawProtobufMarshallerDelegate<>(this, (RawProtobufMarshaller<?>) marshaller);
      }
      Descriptor messageDescriptor = getMessageDescriptor(marshaller.getTypeName());
      return new MessageMarshallerDelegate<>(this, (MessageMarshaller<?>) marshaller, messageDescriptor);
   }

   @Override
   public void unregisterMarshaller(BaseMarshaller<?> marshaller) {
      if (marshaller == null) {
         throw new IllegalArgumentException("marshaller argument cannot be null");
      }
      BaseMarshallerDelegate<?> marshallerDelegate = marshallersByName.get(marshaller.getTypeName());
      if (marshallerDelegate == null || marshallerDelegate.getMarshaller() != marshaller) {
         throw new IllegalArgumentException("The given marshaller was not previously registered with this SerializationContext");
      }
      marshallersByName.remove(marshaller.getTypeName());
      marshallersByClass.remove(marshaller.getJavaClass());
   }

   @Override
   public void registerMarshallerProvider(MarshallerProvider marshallerProvider) {
      if (marshallerProvider == null) {
         throw new IllegalArgumentException("marshallerProvider argument cannot be null");
      }
      marshallerProviders.put(marshallerProvider, marshallerProvider);
   }

   @Override
   public void unregisterMarshallerProvider(MarshallerProvider marshallerProvider) {
      if (marshallerProvider == null) {
         throw new IllegalArgumentException("marshallerProvider argument cannot be null");
      }
      marshallerProviders.remove(marshallerProvider);
   }

   @Override
   public boolean canMarshall(Class<?> javaClass) {
      return marshallersByClass.containsKey(javaClass) || getMarshallerFromProvider(javaClass) != null;
   }

   @Override
   public boolean canMarshall(String fullTypeName) {
      return marshallersByName.containsKey(fullTypeName) || getMarshallerFromProvider(fullTypeName) != null;
   }

   @Override
   public <T> BaseMarshaller<T> getMarshaller(String fullTypeName) {
      return this.<T>getMarshallerDelegate(fullTypeName).getMarshaller();
   }

   @Override
   public <T> BaseMarshaller<T> getMarshaller(Class<T> clazz) {
      return getMarshallerDelegate(clazz).getMarshaller();
   }

   public <T> BaseMarshallerDelegate<T> getMarshallerDelegate(String descriptorFullName) {
      BaseMarshallerDelegate<T> marshallerDelegate = (BaseMarshallerDelegate<T>) marshallersByName.get(descriptorFullName);
      if (marshallerDelegate == null) {
         BaseMarshaller<?> marshaller = getMarshallerFromProvider(descriptorFullName);
         if (marshaller == null) {
            throw new IllegalArgumentException("No marshaller registered for Protobuf type " + descriptorFullName);
         }
         marshallerDelegate = (BaseMarshallerDelegate<T>) makeMarshallerDelegate(marshaller);
      }
      return marshallerDelegate;
   }

   public <T> BaseMarshallerDelegate<T> getMarshallerDelegate(Class<T> javaClass) {
      BaseMarshallerDelegate<T> marshallerDelegate = (BaseMarshallerDelegate<T>) marshallersByClass.get(javaClass);
      if (marshallerDelegate == null) {
         BaseMarshaller<?> marshaller = getMarshallerFromProvider(javaClass);
         if (marshaller == null) {
            throw new IllegalArgumentException("No marshaller registered for Java type " + javaClass.getName());
         }
         marshallerDelegate = (BaseMarshallerDelegate<T>) makeMarshallerDelegate(marshaller);
      }
      return marshallerDelegate;
   }

   private BaseMarshaller<?> getMarshallerFromProvider(Class<?> javaClass) {
      if (!marshallerProviders.isEmpty()) {
         for (MarshallerProvider mp : marshallerProviders.keySet()) {
            BaseMarshaller<?> marshaller = mp.getMarshaller(javaClass);
            if (marshaller != null) {
               return marshaller;
            }
         }
      }
      return null;
   }

   private BaseMarshaller<?> getMarshallerFromProvider(String fullTypeName) {
      if (!marshallerProviders.isEmpty()) {
         for (MarshallerProvider mp : marshallerProviders.keySet()) {
            BaseMarshaller<?> marshaller = mp.getMarshaller(fullTypeName);
            if (marshaller != null) {
               return marshaller;
            }
         }
      }
      return null;
   }

   @Deprecated
   @Override
   public String getTypeNameById(Integer typeId) {
      return getDescriptorByTypeId(typeId).getFullName();
   }

   @Deprecated
   @Override
   public Integer getTypeIdByName(String fullTypeName) {
      return getDescriptorByName(fullTypeName).getTypeId();
   }

   @Override
   public GenericDescriptor getDescriptorByName(String fullTypeName) {
      if (fullTypeName == null) {
         throw new IllegalArgumentException("Type name argument cannot be null");
      }
      readLock.lock();
      try {
         GenericDescriptor descriptor = genericDescriptors.get(fullTypeName);
         if (descriptor == null) {
            throw new IllegalArgumentException("Unknown type name : " + fullTypeName);
         }
         return descriptor;
      } finally {
         readLock.unlock();
      }
   }

   @Override
   public GenericDescriptor getDescriptorByTypeId(Integer typeId) {
      if (typeId == null) {
         throw new IllegalArgumentException("Type id argument cannot be null");
      }
      readLock.lock();
      try {
         GenericDescriptor descriptor = typeIds.get(typeId);
         if (descriptor == null) {
            throw new IllegalArgumentException("Unknown type id : " + typeId);
         }
         return descriptor;
      } finally {
         readLock.unlock();
      }
   }
}
