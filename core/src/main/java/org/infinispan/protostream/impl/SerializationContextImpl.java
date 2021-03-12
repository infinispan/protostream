package org.infinispan.protostream.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.ProtobufTagMarshaller;
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
    * All descriptor related mutable internal state is protected by this RW lock.
    */
   private final ReentrantReadWriteLock dReadWriteLock = new ReentrantReadWriteLock();

   private final Lock dReadLock = dReadWriteLock.readLock();

   private final Lock dWriteLock = dReadWriteLock.writeLock();

   private final Configuration configuration;

   private final SquareProtoParser parser;

   private final Map<String, FileDescriptor> fileDescriptors = new LinkedHashMap<>();

   private final Map<Integer, GenericDescriptor> typeIds = new HashMap<>();

   private final Map<String, GenericDescriptor> genericDescriptors = new HashMap<>();

   private final Map<String, EnumValueDescriptor> enumValueDescriptors = new HashMap<>();

   /**
    * All marshaller related mutable internal state is protected by this RW lock.
    */
   private final ReentrantReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

   private final Lock mReadLock = mReadWriteLock.readLock();

   private final Lock mWriteLock = mReadWriteLock.writeLock();

   private final Map<String, Registration> marshallersByName = new HashMap<>();

   private final Map<Class<?>, Registration> marshallersByClass = new HashMap<>();

   private final List<MarshallerProvider> legacyMarshallerProviders = new ArrayList<>();

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
      dReadLock.lock();
      try {
         return Collections.unmodifiableMap(new HashMap<>(fileDescriptors));
      } finally {
         dReadLock.unlock();
      }
   }

   @Override
   public Map<String, GenericDescriptor> getGenericDescriptors() {
      dReadLock.lock();
      try {
         return Collections.unmodifiableMap(new HashMap<>(genericDescriptors));
      } finally {
         dReadLock.unlock();
      }
   }

   @Override
   public void registerProtoFiles(FileDescriptorSource source) throws DescriptorParserException {
      if (log.isDebugEnabled()) {
         log.debugf("Registering proto files : %s", source.getFiles().keySet());
      }
      Map<String, FileDescriptor> fileDescriptorMap = parser.parse(source);
      dWriteLock.lock();
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
         dWriteLock.unlock();
      }
   }

   @Override
   public void unregisterProtoFile(String fileName) {
      log.debugf("Unregistering proto file : %s", fileName);
      dWriteLock.lock();
      try {
         FileDescriptor fileDescriptor = fileDescriptors.remove(fileName);
         if (fileDescriptor != null) {
            unregisterFileDescriptorTypes(fileDescriptor);
         } else {
            throw new IllegalArgumentException("File " + fileName + " does not exist");
         }
      } finally {
         dWriteLock.unlock();
      }
   }

   @Override
   public void unregisterProtoFiles(Set<String> fileNames) {
      log.debugf("Unregistering proto files : %s", fileNames);
      dWriteLock.lock();
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
         dWriteLock.unlock();
      }
   }

   @GuardedBy("dWriteLock")
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

   private static final class Registration {

      final BaseMarshallerDelegate<?> marshallerDelegate;
      final InstanceMarshallerProvider<?> marshallerProvider;

      // Used on both java and protobuf side when a single marshaller is given and no marshallerProvider
      Registration(BaseMarshallerDelegate<?> marshallerDelegate) {
         this.marshallerDelegate = marshallerDelegate;
         this.marshallerProvider = null;
      }

      // Used on protobuf side when a marshallerProvider is given
      Registration(BaseMarshallerDelegate<?> marshallerDelegate, InstanceMarshallerProvider<?> marshallerProvider) {
         this.marshallerDelegate = marshallerDelegate;
         this.marshallerProvider = marshallerProvider;
      }

      // Used on java side when a marshallerProvider is given
      Registration(InstanceMarshallerProvider<?> marshallerProvider) {
         this.marshallerDelegate = null;
         this.marshallerProvider = marshallerProvider;
      }
   }

   @Override
   public void registerMarshaller(BaseMarshaller<?> marshaller) {
      if (marshaller == null) {
         throw new IllegalArgumentException("marshaller argument cannot be null");
      }

      mWriteLock.lock();
      try {
         Registration existingByName = marshallersByName.get(marshaller.getTypeName());
         Registration existingByClass = marshallersByClass.get(marshaller.getJavaClass());
         if (existingByName != null && existingByName.marshallerProvider != null ||
               existingByClass != null && existingByClass.marshallerProvider != null) {
            throw new IllegalArgumentException("The given marshaller attempts to override an existing marshaller registered indirectly via an InstanceMarshallerProvider. Please unregister it first.");
         }

         if (existingByName != null) {
            Registration anotherByClass = marshallersByClass.get(existingByName.marshallerDelegate.getMarshaller().getJavaClass());
            if (anotherByClass == null) {
               throw new IllegalStateException("Inconsistent marshaller definitions!");
            }
            if (anotherByClass.marshallerProvider != null) {
               throw new IllegalArgumentException("The given marshaller attempts to override an existing marshaller registered indirectly via an InstanceMarshallerProvider. Please unregister that first.");
            } else {
               if (!anotherByClass.marshallerDelegate.getMarshaller().getTypeName().equals(marshaller.getTypeName())) {
                  throw new IllegalStateException("Inconsistent marshaller definitions!");
               }
            }
            marshallersByClass.remove(existingByName.marshallerDelegate.getMarshaller().getJavaClass());
         }
         if (existingByClass != null) {
            marshallersByName.remove(existingByClass.marshallerDelegate.getMarshaller().getTypeName());
         }
         Registration registration = new Registration(makeMarshallerDelegate(marshaller));
         marshallersByClass.put(marshaller.getJavaClass(), registration);
         marshallersByName.put(marshaller.getTypeName(), registration);
      } finally {
         mWriteLock.unlock();
      }
   }

   private <T> BaseMarshallerDelegate<T> makeMarshallerDelegate(BaseMarshaller<T> marshaller) {
      if (marshaller.getJavaClass().isEnum() && !(marshaller instanceof EnumMarshaller)) {
         throw new IllegalArgumentException("Invalid marshaller (the produced class is a Java Enum, but the marshaller is not an EnumMarshaller) : " + marshaller.getClass().getName());
      }
      // we try to validate first that a descriptor exists
      if (marshaller instanceof EnumMarshaller) {
         if (!marshaller.getJavaClass().isEnum()) {
            throw new IllegalArgumentException("Invalid enum marshaller (the produced class is not a Java Enum) : " + marshaller.getClass().getName());
         }
         EnumDescriptor enumDescriptor = getEnumDescriptor(marshaller.getTypeName());
         return new EnumMarshallerDelegate<>((EnumMarshaller) marshaller, enumDescriptor);
      } else if (marshaller instanceof ProtobufTagMarshaller) {
         return new RawProtoStreamMarshallerDelegate<>((ProtobufTagMarshaller<T>) marshaller);
      }
      Descriptor messageDescriptor = getMessageDescriptor(marshaller.getTypeName());
      return new MessageMarshallerDelegate<>((MessageMarshaller<T>) marshaller, messageDescriptor);
   }

   @Override
   public void unregisterMarshaller(BaseMarshaller<?> marshaller) {
      if (marshaller == null) {
         throw new IllegalArgumentException("marshaller argument cannot be null");
      }

      mWriteLock.lock();
      try {
         Registration existingByName = marshallersByName.get(marshaller.getTypeName());
         if (existingByName == null || existingByName.marshallerDelegate.getMarshaller() != marshaller) {
            throw new IllegalArgumentException("The given marshaller was not previously registered with this SerializationContext");
         }
         if (existingByName.marshallerProvider != null) {
            throw new IllegalArgumentException("Attempting to unregister a marshaller that was registered indirectly via an InstanceMarshallerProvider. Please use the same mechanism to unregister it.");
         }
         marshallersByName.remove(marshaller.getTypeName());
         marshallersByClass.remove(marshaller.getJavaClass());
      } finally {
         mWriteLock.unlock();
      }
   }

   @Deprecated
   @Override
   public void registerMarshallerProvider(MarshallerProvider marshallerProvider) {
      if (marshallerProvider == null) {
         throw new IllegalArgumentException("marshallerProvider argument cannot be null");
      }

      mWriteLock.lock();
      try {
         legacyMarshallerProviders.add(marshallerProvider);
      } finally {
         mWriteLock.unlock();
      }
   }

   @Deprecated
   @Override
   public void unregisterMarshallerProvider(MarshallerProvider marshallerProvider) {
      if (marshallerProvider == null) {
         throw new IllegalArgumentException("marshallerProvider argument cannot be null");
      }

      mWriteLock.lock();
      try {
         legacyMarshallerProviders.remove(marshallerProvider);
      } finally {
         mWriteLock.unlock();
      }
   }

   @Override
   public void registerMarshallerProvider(InstanceMarshallerProvider<?> marshallerProvider) {
      if (marshallerProvider == null) {
         throw new IllegalArgumentException("marshallerProvider argument cannot be null");
      }

      mWriteLock.lock();
      try {
         Registration byClass = marshallersByClass.get(marshallerProvider.getJavaClass());
         if (byClass != null) {
            if (byClass.marshallerProvider == null) {
               throw new IllegalArgumentException("A marshaller was already registered for the same class. Please unregister it first.");
            } else {
               if (!byClass.marshallerProvider.getTypeNames().equals(marshallerProvider.getTypeNames())) {
                  throw new IllegalArgumentException("An InstanceMarshallerProvider was already registered for the same class but maps to a different set of protobuf types. Please unregister it first.");
               }
            }
         }
         marshallersByClass.put(marshallerProvider.getJavaClass(), new Registration(marshallerProvider));
         for (String typeName : marshallerProvider.getTypeNames()) {
            BaseMarshaller<?> marshaller = marshallerProvider.getMarshaller(typeName);
            marshallersByName.put(typeName, new Registration(makeMarshallerDelegate(marshaller), marshallerProvider));
         }
      } finally {
         mWriteLock.unlock();
      }
   }

   @Override
   public void unregisterMarshallerProvider(InstanceMarshallerProvider<?> marshallerProvider) {
      if (marshallerProvider == null) {
         throw new IllegalArgumentException("marshallerProvider argument cannot be null");
      }

      mWriteLock.lock();
      try {
         Registration byClass = marshallersByClass.get(marshallerProvider.getJavaClass());
         if (byClass == null || byClass.marshallerProvider != marshallerProvider) {
            throw new IllegalArgumentException("The given InstanceMarshallerProvider was not previously registered with this SerializationContext");
         }
         marshallersByClass.remove(marshallerProvider.getJavaClass());
         marshallersByName.keySet().removeAll(marshallerProvider.getTypeNames());
      } finally {
         mWriteLock.unlock();
      }
   }

   @Override
   public boolean canMarshall(Class<?> javaClass) {
      mReadLock.lock();
      try {
         return marshallersByClass.containsKey(javaClass) || getMarshallerFromLegacyProvider(javaClass) != null;
      } finally {
         mReadLock.unlock();
      }
   }

   @Override
   public boolean canMarshall(String fullTypeName) {
      mReadLock.lock();
      try {
         return marshallersByName.containsKey(fullTypeName) || getMarshallerFromLegacyProvider(fullTypeName) != null;
      } finally {
         mReadLock.unlock();
      }
   }

   @Override
   public boolean canMarshall(Object object) {
      Class<?> javaClass = object.getClass();
      mReadLock.lock();
      try {
         Registration registration = marshallersByClass.get(javaClass);
         if (registration != null) {
            if (registration.marshallerProvider != null) {
               String typeName = ((InstanceMarshallerProvider<Object>)registration.marshallerProvider).getTypeName(object);
               if (typeName == null) {
                  throw new IllegalArgumentException("No marshaller registered for object of Java type " + javaClass.getName() + " : " + object);
               }
               registration = marshallersByName.get(typeName);
            }
            if (registration != null) {
               return true;
            }
         }

         BaseMarshaller<?> marshaller = getMarshallerFromLegacyProvider(javaClass);
         return marshaller != null;
      } finally {
         mReadLock.unlock();
      }
   }

   @Override
   public <T> BaseMarshaller<T> getMarshaller(T object) {
      return getMarshallerDelegate(object).getMarshaller();
   }

   @Override
   public <T> BaseMarshaller<T> getMarshaller(String fullTypeName) {
      return this.<T>getMarshallerDelegate(fullTypeName).getMarshaller();
   }

   @Override
   public <T> BaseMarshaller<T> getMarshaller(Class<T> clazz) {
      return getMarshallerDelegate(clazz).getMarshaller();
   }

   public <T> BaseMarshallerDelegate<T> getMarshallerDelegate(String typeName) {
      mReadLock.lock();
      try {
         Registration registration = marshallersByName.get(typeName);
         if (registration != null) {
            return (BaseMarshallerDelegate<T>) registration.marshallerDelegate;
         }

         BaseMarshaller<T> marshaller = getMarshallerFromLegacyProvider(typeName);
         if (marshaller == null) {
            throw new IllegalArgumentException("No marshaller registered for Protobuf type " + typeName);
         }
         //todo [anistor] A marshaller delegate is created per call and cannot be cached! This is just legacy.
         return makeMarshallerDelegate(marshaller);
      } finally {
         mReadLock.unlock();
      }
   }

   public <T> BaseMarshallerDelegate<T> getMarshallerDelegate(Class<T> javaClass) {
      mReadLock.lock();
      try {
         Registration registration = marshallersByClass.get(javaClass);
         if (registration != null) {
            if (registration.marshallerProvider != null) {
               throw new IllegalArgumentException("Java type " + javaClass.getName()
                     + " is mapped to multiple protobuf types : " + registration.marshallerProvider.getTypeNames()
                     + ". Object instance needed for disambiguation.");
            }
            return (BaseMarshallerDelegate<T>) registration.marshallerDelegate;
         }

         BaseMarshaller<T> marshaller = getMarshallerFromLegacyProvider(javaClass);
         if (marshaller == null) {
            throw new IllegalArgumentException("No marshaller registered for Java type " + javaClass.getName());
         }
         //todo [anistor] A marshaller delegate is created per call and cannot be cached! This is just legacy.
         return makeMarshallerDelegate(marshaller);
      } finally {
         mReadLock.unlock();
      }
   }

   public <T> BaseMarshallerDelegate<T> getMarshallerDelegate(T object) {
      Class<T> javaClass = (Class<T>) object.getClass();
      mReadLock.lock();
      try {
         Registration registration = marshallersByClass.get(javaClass);
         if (registration != null) {
            if (registration.marshallerProvider != null) {
               String typeName = ((InstanceMarshallerProvider<T>) registration.marshallerProvider).getTypeName(object);
               if (typeName == null) {
                  throw new IllegalArgumentException("No marshaller registered for object of Java type " + javaClass.getName() + " : " + object);
               }
               registration = marshallersByName.get(typeName);
            }
            if (registration != null) {
               return (BaseMarshallerDelegate<T>) registration.marshallerDelegate;
            }
         }

         BaseMarshaller<T> marshaller = getMarshallerFromLegacyProvider(javaClass);
         if (marshaller == null) {
            throw new IllegalArgumentException("No marshaller registered for object of Java type " + javaClass.getName() + " : " + object);
         }
         //todo [anistor] A marshaller delegate is created per call and cannot be cached! This is just legacy.
         return makeMarshallerDelegate(marshaller);
      } finally {
         mReadLock.unlock();
      }
   }

   @GuardedBy("mReadLock")
   private <T> BaseMarshaller<T> getMarshallerFromLegacyProvider(Class<T> javaClass) {
      if (!legacyMarshallerProviders.isEmpty()) {
         for (MarshallerProvider mp : legacyMarshallerProviders) {
            BaseMarshaller<T> marshaller = (BaseMarshaller<T>) mp.getMarshaller(javaClass);
            if (marshaller != null) {
               return marshaller;
            }
         }
      }
      return null;
   }

   @GuardedBy("mReadLock")
   private <T> BaseMarshaller<T> getMarshallerFromLegacyProvider(String fullTypeName) {
      if (!legacyMarshallerProviders.isEmpty()) {
         for (MarshallerProvider mp : legacyMarshallerProviders) {
            BaseMarshaller<T> marshaller = (BaseMarshaller<T>) mp.getMarshaller(fullTypeName);
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

      dReadLock.lock();
      try {
         GenericDescriptor descriptor = genericDescriptors.get(fullTypeName);
         if (descriptor == null) {
            throw new IllegalArgumentException("Unknown type name : " + fullTypeName);
         }
         return descriptor;
      } finally {
         dReadLock.unlock();
      }
   }

   @Override
   public GenericDescriptor getDescriptorByTypeId(Integer typeId) {
      if (typeId == null) {
         throw new IllegalArgumentException("Type id argument cannot be null");
      }

      dReadLock.lock();
      try {
         GenericDescriptor descriptor = typeIds.get(typeId);
         if (descriptor == null) {
            throw new IllegalArgumentException("Unknown type id : " + typeId);
         }
         return descriptor;
      } finally {
         dReadLock.unlock();
      }
   }
}
