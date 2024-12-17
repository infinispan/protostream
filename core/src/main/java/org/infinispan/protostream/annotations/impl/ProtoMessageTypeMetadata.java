
package org.infinispan.protostream.annotations.impl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoComment;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.annotations.ProtoUnknownFieldSet;
import org.infinispan.protostream.annotations.impl.types.XClass;
import org.infinispan.protostream.annotations.impl.types.XConstructor;
import org.infinispan.protostream.annotations.impl.types.XExecutable;
import org.infinispan.protostream.annotations.impl.types.XField;
import org.infinispan.protostream.annotations.impl.types.XMember;
import org.infinispan.protostream.annotations.impl.types.XMethod;
import org.infinispan.protostream.annotations.impl.types.XTypeFactory;
import org.infinispan.protostream.containers.IndexedElementContainer;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;
import org.infinispan.protostream.containers.IterableElementContainer;
import org.infinispan.protostream.containers.IterableElementContainerAdapter;
import org.infinispan.protostream.descriptors.JavaType;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.impl.Log;

/**
 * A {@link ProtoTypeMetadata} for a message type created based on annotations.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public class ProtoMessageTypeMetadata extends ProtoTypeMetadata {
   private static final byte[] EMPTY_BYTES = {};

   private static final Log log = Log.LogFactory.getLog(ProtoMessageTypeMetadata.class);

   private final BaseProtoSchemaGenerator protoSchemaGenerator;

   protected final XTypeFactory typeFactory;

   private SortedMap<Integer, ProtoFieldMetadata> fieldsByNumber = null;

   private final XClass annotatedClass;

   private final boolean isAdapter;

   private final boolean isIndexedContainer;

   private final boolean isIterableContainer;

   private XExecutable factory;

   private XField unknownFieldSetField;

   private XMethod unknownFieldSetGetter;

   private XMethod unknownFieldSetSetter;

   private final Map<XClass, ProtoTypeMetadata> innerTypes = new HashMap<>();

   protected ProtoMessageTypeMetadata(BaseProtoSchemaGenerator protoSchemaGenerator, XClass annotatedClass, XClass javaClass) {
      super(getProtoName(annotatedClass, javaClass), javaClass);
      this.protoSchemaGenerator = protoSchemaGenerator;
      this.annotatedClass = annotatedClass;
      this.typeFactory = annotatedClass.getFactory();
      this.isAdapter = javaClass != annotatedClass;
      this.isIndexedContainer = annotatedClass.isAssignableTo(isAdapter ? IndexedElementContainerAdapter.class : IndexedElementContainer.class);
      this.isIterableContainer = annotatedClass.isAssignableTo(isAdapter ? IterableElementContainerAdapter.class : IterableElementContainer.class);

      checkInstantiability();

      validateName();
   }

   private static String getProtoName(XClass annotatedClass, XClass javaClass) {
      var clazz = javaClass.isInterface() ? annotatedClass : javaClass;
      ProtoName annotation = annotatedClass.getAnnotation(ProtoName.class);
      if (annotation != null) {
         return annotation.value().isEmpty() ? clazz.getSimpleName() : annotation.value();
      } else {
         return clazz.getSimpleName();
      }
   }

   @Override
   public XClass getAnnotatedClass() {
      return annotatedClass;
   }

   @Override
   public boolean isAdapter() {
      return isAdapter;
   }

   public boolean isIndexedContainer() {
      return isIndexedContainer;
   }

   public boolean isIterableContainer() {
      return isIterableContainer;
   }

   public boolean isContainer() {
      return isIterableContainer || isIndexedContainer;
   }

   public XExecutable getFactory() {
      scanMemberAnnotations();
      return factory;
   }

   public XField getUnknownFieldSetField() {
      scanMemberAnnotations();
      return unknownFieldSetField;
   }

   public XMethod getUnknownFieldSetGetter() {
      scanMemberAnnotations();
      return unknownFieldSetGetter;
   }

   public XMethod getUnknownFieldSetSetter() {
      scanMemberAnnotations();
      return unknownFieldSetSetter;
   }

   public SortedMap<Integer, ProtoFieldMetadata> getFields() {
      scanMemberAnnotations();
      return fieldsByNumber;
   }

   protected void addInnerType(ProtoTypeMetadata typeMetadata) {
      innerTypes.put(typeMetadata.getJavaClass(), typeMetadata);
   }

   @Override
   public void generateProto(IndentWriter iw, ProtoSyntax syntax) {
      scanMemberAnnotations();  //todo [anistor] need to have a better place for this call

      iw.append("\n\n");
      appendDocumentation(iw, getDocumentation());
      iw.append("message ").append(name);
      if (BaseProtoSchemaGenerator.generateSchemaDebugComments) {
         iw.append(" /* ").append(getJavaClassName()).append(" */");
      }
      iw.append(" {\n");
      iw.inc();

      ReservedProcessor reserved = new ReservedProcessor();
      reserved.scan(annotatedClass);

      for (int memberNumber : fieldsByNumber.keySet()) {
         ProtoFieldMetadata field = fieldsByNumber.get(memberNumber);
         XClass where = reserved.checkReserved(memberNumber);
         if (where != null) {
            throw Log.LOG.reservedNumber(memberNumber, field.getName(), where.getCanonicalName());
         }
         where = reserved.checkReserved(field.getName());
         if (where != null) {
            throw Log.LOG.reservedName(field.getName(), where.getCanonicalName());
         }
      }

      reserved.generate(iw);

      for (ProtoTypeMetadata t : innerTypes.values()) {
         t.generateProto(iw, syntax);
      }

      LinkedList<ProtoFieldMetadata> unprocessedFields = new LinkedList<>(fieldsByNumber.values());
      while (!unprocessedFields.isEmpty()) {
         ProtoFieldMetadata f = unprocessedFields.remove();
         if (f.getOneof() == null) {
            f.generateProto(iw, syntax);
         } else {
            iw.append("\noneof ").append(f.getOneof()).append(" {\n");
            iw.inc();
            f.generateProto(iw, syntax);
            Iterator<ProtoFieldMetadata> it = unprocessedFields.iterator();
            while (it.hasNext()) {
               ProtoFieldMetadata f2 = it.next();
               if (f.getOneof().equals(f2.getOneof())) {
                  f2.generateProto(iw, syntax);
                  it.remove();
               }
            }
            iw.dec();
            iw.append("}\n");
         }
      }

      iw.dec();
      iw.append("}\n");
   }

   @Override
   public final boolean isEnum() {
      return false;
   }

   @Override
   public final ProtoEnumValueMetadata getEnumMemberByName(String name) {
      throw new IllegalStateException(getJavaClassName() + " is not an enum");
   }

   @Override
   public ProtoEnumValueMetadata getEnumMemberByNumber(int number) {
      throw new IllegalStateException(getJavaClassName() + " is not an enum");
   }

   @Override
   public void scanMemberAnnotations() {
      if (fieldsByNumber == null) {
         // All the fields discovered in this class hierarchy, the key is their number.
         // We use a TreeMap to ensure ascending order by field number.
         fieldsByNumber = new TreeMap<>();

         // all the fields discovered in this class hierarchy, by name
         Map<String, ProtoFieldMetadata> fieldsByName = new HashMap<>();

         // all the oneofs discovered in this class hierarchy
         Set<String> oneofs = new HashSet<>();

         discoverFields(annotatedClass, new HashSet<>(), fieldsByNumber, fieldsByName, oneofs);
         if (fieldsByNumber.isEmpty()) {
            //todo avoid this warning in case where not necessary
            // TODO [anistor] remove the "The class should be either annotated or it should have a custom marshaller" part after MessageMarshaller is removed in 5
            log.warnf("Class %s does not have any @ProtoField annotated members. The class should be either annotated or it should have a custom marshaller.", getAnnotatedClassName());
         }

         // If we have a factory method / constructor, we must ensure its parameters match the declared fields
         if (factory != null) {
            String factoryKind = factory instanceof XConstructor ? "constructor" : (factory.isStatic() ? "static method" : "method");
            XClass[] parameterTypes = factory.getParameterTypes();
            int startPos = 0;
            if (isIndexedContainer || isIterableContainer) {
               if (parameterTypes.length == 0 || parameterTypes[0] != typeFactory.fromClass(int.class)) {
                  throw Log.LOG.factorySignatureMismatch(factoryKind, factory.toGenericString());
               }
               startPos = 1;
            }
            String[] parameterNames = factory.getParameterNames();
            if (parameterNames.length != fieldsByNumber.size() + startPos) {
               throw Log.LOG.factorySignatureMismatch(factoryKind, fieldsByNumber.size() + startPos, parameterNames.length, factory.toGenericString());
            }
            for (; startPos < parameterNames.length; startPos++) {
               String parameterName = parameterNames[startPos];
               ProtoFieldMetadata fieldMetadata = getFieldByPropertyName(parameterName);
               if (fieldMetadata == null) {
                  throw Log.LOG.factorySignatureMismatch(factoryKind, parameterName, factory.toGenericString());
               }
               XClass parameterType = parameterTypes[startPos];
               boolean paramTypeMismatch = false;
               if (fieldMetadata.isArray()) {
                  if (!parameterType.isArray() || parameterType.getComponentType() != fieldMetadata.getJavaType()) {
                     paramTypeMismatch = true;
                  }
               } else if (fieldMetadata.isRepeated()) {
                  if (!fieldMetadata.getRepeatedImplementation().isAssignableTo(parameterType)) {
                     paramTypeMismatch = true;
                  }
                  // todo [anistor] also check the collection's type parameter
               } else if (fieldMetadata.getJavaType() != parameterType) {
                  paramTypeMismatch = true;
               }
               if (paramTypeMismatch) {
                  throw Log.LOG.factorySignatureMismatchType(factoryKind, factory.toGenericString(), parameterName);
               }
            }
         }
      }
   }

   private ProtoFieldMetadata getFieldByPropertyName(String propName) {
      for (ProtoFieldMetadata field : fieldsByNumber.values()) {
         if (propName.equals(field.getPropertyName())) {
            return field;
         }
      }
      return null;
   }

   /**
    * Ensure we either have a suitable constructor or a factory method.
    */
   private void checkInstantiability() {
      // ensure the class is not abstract
      if (annotatedClass.isAbstract() || annotatedClass.isInterface()) {
         throw Log.LOG.abstractClassNotAllowed(getAnnotatedClassName());
      }
      // ensure it is not a local or anonymous class
      if (annotatedClass.isLocal()) {
         throw Log.LOG.localOrAnonymousClass(getAnnotatedClassName());
      }
      // ensure the class is not a non-static inner class
      if (annotatedClass.getEnclosingClass() != null && !annotatedClass.isStatic()) {
         throw Log.LOG.nonStaticInnerClass(getAnnotatedClassName());
      }
      if (javaClass.isRecord()) {
         Iterable<? extends XConstructor> declaredConstructors = javaClass.getDeclaredConstructors();
         factory = declaredConstructors.iterator().next();
      }

      for (XConstructor c : annotatedClass.getDeclaredConstructors()) {
         if (c.getAnnotation(ProtoFactory.class) != null) {
            if (factory != null) {
               throw Log.LOG.multipleFactories(c.toString());
            }
            if (c.isPrivate()) {
               throw Log.LOG.privateFactory(c.toString());
            }
            factory = c;
         }
      }

      for (XMethod m : annotatedClass.getDeclaredMethods()) {
         if (m.getAnnotation(ProtoFactory.class) != null) {
            if (factory != null) {
               throw Log.LOG.multipleFactories(m.toString());
            }
            if (!isAdapter && !m.isStatic()) {
               throw Log.LOG.nonStaticFactory(m.toString());
            }
            if (m.isPrivate()) {
               throw Log.LOG.privateFactory(m.toString());
            }
            if (m.getReturnType() != javaClass) {
               throw Log.LOG.wrongFactoryReturnType(m.toString());
            }
            factory = m;
         }
      }

      if (factory == null) {
         if (isAdapter || isContainer()) {
            throw new ProtoSchemaBuilderException("The class " + getJavaClassName() +
                  " must be instantiable using an accessible @ProtoFactory annotated method defined by " + getAnnotatedClassName());
         }
         // If no factory method/constructor was found we need to ensure the class has an accessible no-argument constructor
         XConstructor ctor = javaClass.getDeclaredConstructor();
         if (ctor == null || ctor.isPrivate()) {
            throw new ProtoSchemaBuilderException("The class " + getJavaClassName() + " must be instantiable using an accessible no-argument constructor.");
         }
      }
   }

   private void discoverFields(XClass clazz, Set<XClass> examinedClasses, Map<Integer, ProtoFieldMetadata> fieldsByNumber, Map<String, ProtoFieldMetadata> fieldsByName, Set<String> oneofs) {
      if (!examinedClasses.add(clazz)) {
         // avoid re-examining classes due to multiple interface inheritance
         return;
      }
      if (clazz.isRecord()) {
         discoverFieldsFromRecord(clazz, fieldsByNumber, fieldsByName);
         return;
      }
      if (clazz.getSuperclass() != null) {
         discoverFields(clazz.getSuperclass(), examinedClasses, fieldsByNumber, fieldsByName, oneofs);
      }
      for (XClass i : clazz.getInterfaces()) {
         discoverFields(i, examinedClasses, fieldsByNumber, fieldsByName, oneofs);
      }
      discoverFieldsFromClassFields(clazz, fieldsByNumber, fieldsByName, oneofs);
      discoverFieldsFromClassMethods(clazz, fieldsByNumber, fieldsByName, oneofs);
   }

   private void discoverFieldsFromClassMethods(XClass clazz, Map<Integer, ProtoFieldMetadata> fieldsByNumber, Map<String, ProtoFieldMetadata> fieldsByName, Set<String> oneofs) {
      Set<XMethod> skipMethods = new HashSet<>();

      for (XMethod method : clazz.getDeclaredMethods()) {
         if (skipMethods.contains(method)) {
            continue;
         }

         if (method.getAnnotation(ProtoUnknownFieldSet.class) != null) {
            if (unknownFieldSetField != null || unknownFieldSetGetter != null || unknownFieldSetSetter != null) {
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet annotation should not occur more than once in a class and its superclasses and superinterfaces : " + method);
            }
            if (method.getAnnotation(ProtoField.class) != null) {
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet and @ProtoField annotations cannot be used together: " + method);
            }
            String propertyName;
            if (method.getReturnType() == typeFactory.fromClass(void.class)) {
               // this method is expected to be a setter
               propertyName = detectPropertyNameFromSetter(method);
               //TODO [anistor] also check setter args
               unknownFieldSetSetter = method;
               unknownFieldSetGetter = findGetter(propertyName, method.getParameterTypes()[0]);
               checkForbiddenAnnotations(unknownFieldSetGetter, unknownFieldSetSetter);
               skipMethods.add(unknownFieldSetGetter);
            } else {
               // this method is expected to be a getter
               propertyName = determinePropertyNameFromGetter(method);
               //TODO [anistor] also check getter args
               unknownFieldSetGetter = method;
               unknownFieldSetSetter = findSetter(propertyName, unknownFieldSetGetter.getReturnType());
               checkForbiddenAnnotations(unknownFieldSetSetter, unknownFieldSetGetter);
               skipMethods.add(unknownFieldSetSetter);
            }
         } else {
            ProtoField annotation = method.getAnnotation(ProtoField.class);
            if (annotation != null) {
               if (method.isPrivate()) {
                  throw new ProtoSchemaBuilderException("Private methods cannot be @ProtoField annotated: " + method);
               }
               if (!isAdapter && method.isStatic()) {
                  throw new ProtoSchemaBuilderException("Static methods cannot be @ProtoField annotated: " + method);
               }
               String propertyName;
               XMethod getter;
               XMethod setter;
               XClass getterReturnType;
               // we can have the annotation present on either getter or setter but not both
               if (method.getReturnType() == typeFactory.fromClass(void.class)) {
                  // this method is expected to be a setter
                  propertyName = detectPropertyNameFromSetter(method);
                  //TODO [anistor] also check setter args
                  setter = method;
                  getter = findGetter(propertyName, method.getParameterTypes()[0]);
                  checkForbiddenAnnotations(getter, setter);
                  skipMethods.add(getter);
                  getterReturnType = getter.getReturnType();
                  if (getterReturnType == typeFactory.fromClass(Optional.class)) {
                     getterReturnType = getter.determineOptionalReturnType();
                  }
               } else {
                  // this method is expected to be a getter
                  propertyName = determinePropertyNameFromGetter(method);
                  //TODO [anistor] also check getter args
                  getter = method;
                  getterReturnType = getter.getReturnType();
                  if (getterReturnType == typeFactory.fromClass(Optional.class)) {
                     getterReturnType = getter.determineOptionalReturnType();
                  }
                  if (factory == null) {
                     setter = findSetter(propertyName, getterReturnType);
                     checkForbiddenAnnotations(setter, getter);
                     skipMethods.add(setter);
                  } else {
                     setter = null;
                  }
               }

               int number = getNumber(annotation, method);

               String fieldName = annotation.name();
               if (fieldName.isEmpty()) {
                  fieldName = propertyName;
               }

               Type protobufType = defaultType(annotation, getterReturnType);
               boolean isArray = isArray(getterReturnType, protobufType);
               boolean isRepeated = isRepeated(getterReturnType, protobufType);
               boolean isRequired = annotation.required();
               if (isRequired && protoSchemaGenerator.syntax() != ProtoSyntax.PROTO2) {
                  throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of " + clazz.getCanonicalName() + " cannot be marked required when using \"" + protoSchemaGenerator.syntax() + "\" syntax");
               }
               boolean isMap = isMap(getterReturnType);
               if (isMap && protoSchemaGenerator.syntax() == ProtoSyntax.PROTO2) {
                  throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of " + clazz.getCanonicalName() + " of type map is not supported when using \"" + protoSchemaGenerator.syntax() + "\" syntax");
               }
               if (isRepeated && isRequired) {
                  throw new ProtoSchemaBuilderException("Repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " cannot be marked required.");
               }
               XClass javaType = getJavaTypeFromAnnotation(annotation);
               if (javaType == typeFactory.fromClass(void.class)) {
                  javaType = isRepeated ? getter.determineRepeatedElementType() : getterReturnType;
               }
               if (javaType == typeFactory.fromClass(byte[].class) && protobufType == Type.MESSAGE) {
                  // MESSAGE is the default and stands for 'undefined', we can override it with a better default
                  protobufType = Type.BYTES;
               }
               if (!javaType.isArray() && !javaType.isPrimitive() && javaType.isAbstract() && !javaType.isEnum()) {
                  throw Log.LOG.abstractType(javaType.getCanonicalName(), fieldName, clazz.getCanonicalName());
               }

               protobufType = getProtobufType(javaType, protobufType);

               ProtoTypeMetadata protoTypeMetadata = null;
               if (protobufType.getJavaType() == JavaType.ENUM || protobufType.getJavaType() == JavaType.MESSAGE) {
                  protoTypeMetadata = protoSchemaGenerator.scanAnnotations(javaType);
               }

               ProtoFieldMetadata fieldMetadata;
               if (isMap) {
                  // Determine the map implementation
                  XClass repeatedImplementation = getMapImplementation(clazz, getterReturnType, getMapImplementationFromAnnotation(annotation), fieldName, isRepeated);
                  XClass keyJavaType = getter.getTypeArgument(0);
                  Type keyProtobufType = getProtobufType(keyJavaType, Type.MESSAGE);
                  if (!keyProtobufType.isValidMapKey()) {
                     throw new ProtoSchemaBuilderException("The key of the map field '" + fieldName + "' of " + clazz.getName() + " must be either a String or an integral type");
                  }
                  fieldMetadata = new ProtoMapMetadata(number, fieldName, keyJavaType, javaType, repeatedImplementation, keyProtobufType, protobufType, protoTypeMetadata, propertyName, method, getter, setter);
               } else {
                  // Determine the collection implementation
                  XClass repeatedImplementation = getCollectionImplementation(clazz, getterReturnType, getCollectionImplementationFromAnnotation(annotation), fieldName, isRepeated);
                  if (isArray) {
                     repeatedImplementation = typeFactory.fromClass(ArrayList.class);
                  }
                  String oneof = validateOneOf(clazz, fieldsByName, oneofs, annotation, fieldName, isRepeated, isRequired);
                  // Determine default value
                  Object defaultValue = getDefaultValue(clazz, fieldName, javaType, protobufType, annotation.defaultValue(), isRepeated);
                  if (!isRequired && !isRepeated && javaType.isPrimitive() && defaultValue == null) {
                     throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz.getCanonicalName() + " is not nullable so it should be either marked required or should have a default value, while processing " + this.protoSchemaGenerator.generator);
                  }
                  // Create the field metadata
                  fieldMetadata = new ProtoFieldMetadata(number, fieldName, oneof, javaType, repeatedImplementation,
                        protobufType, protoTypeMetadata, isRequired, isRepeated, isArray, defaultValue,
                        propertyName, method, getter, setter);
               }

               ProtoFieldMetadata existing = fieldsByNumber.get(number);
               if (isDuplicateField(existing, fieldMetadata)) {
                  throw new ProtoSchemaBuilderException("Duplicate field definition. Found two field definitions with number " + number + ": in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation());
               }
               existing = fieldsByName.get(fieldMetadata.getName());
               if (isDuplicateField(existing, fieldMetadata)) {
                  throw new ProtoSchemaBuilderException("Duplicate field definition. Found two field definitions with name '" + fieldMetadata.getName() + "': in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation());
               }

               checkReserved(fieldMetadata);
               fieldsByNumber.put(number, fieldMetadata);
               fieldsByName.put(fieldName, fieldMetadata);
            }
         }
      }
   }

   private boolean isDuplicateField(ProtoFieldMetadata existing, ProtoFieldMetadata newField) {
      if (existing == null)
         return false;

      if (existing.isArray() || newField.isArray())
         return true;

      return !newField.getJavaType().isAssignableTo(existing.getJavaType());
   }

   private Type defaultType(ProtoField annotation, XClass type) {
      Type protobufType = annotation == null ? Type.MESSAGE : annotation.type();
      if (type == typeFactory.fromClass(byte[].class) && protobufType == Type.MESSAGE) {
         // MESSAGE is the default and stands for 'undefined', we can override it with a better default
         protobufType = Type.BYTES;
      }
      return protobufType;
   }

   private void discoverFieldsFromClassFields(XClass clazz, Map<Integer, ProtoFieldMetadata> fieldsByNumber, Map<String, ProtoFieldMetadata> fieldsByName, Set<String> oneofs) {
      boolean implicitFields = clazz.getAnnotation(Proto.class) != null;
      int position = 0;
      for (XField field : clazz.getDeclaredFields()) {
         position++;
         if (field.getAnnotation(ProtoUnknownFieldSet.class) != null) {
            if (isAdapter) {
               throw new ProtoSchemaBuilderException("No ProtoStream annotations should be present on fields when @ProtoAdapter is present on a class : " + clazz.getCanonicalName() + '.' + field);
            }
            if (unknownFieldSetField != null || unknownFieldSetGetter != null || unknownFieldSetSetter != null) {
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet annotation should not occur more than once in a class and its superclasses and superinterfaces : " + clazz.getCanonicalName() + '.' + field);
            }
            if (field.getAnnotation(ProtoField.class) != null) {
               throw new ProtoSchemaBuilderException("The @ProtoUnknownFieldSet and @ProtoField annotations cannot be used together: " + field);
            }
            unknownFieldSetField = field;
         } else {
            ProtoField annotation = field.getAnnotation(ProtoField.class);
            if (annotation != null || implicitFields) {
               validateField(clazz, field);
               int number = annotation != null ? getNumber(annotation, field) : position;
               String fieldName = getName(annotation, field);
               Type protobufType = defaultType(annotation, field.getType());
               boolean isArray = isArray(field.getType(), protobufType);
               boolean isRepeated = isRepeated(field.getType(), protobufType);
               boolean isRequired = annotation != null && annotation.required();
               if (isRequired && protoSchemaGenerator.syntax() != ProtoSyntax.PROTO2) {
                  throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of " + clazz.getCanonicalName() + " cannot be marked required when using \"" + protoSchemaGenerator.syntax() + "\" syntax, while processing " + this.protoSchemaGenerator.generator);
               }
               boolean isMap = isMap(field.getType());
               if (isMap && protoSchemaGenerator.syntax() == ProtoSyntax.PROTO2) {
                  throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of " + clazz.getCanonicalName() + " of type map is not supported when using \"" + protoSchemaGenerator.syntax() + "\" syntax, while processing " + this.protoSchemaGenerator.generator);
               }
               if (isRepeated && isRequired) {
                  throw new ProtoSchemaBuilderException("Repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " cannot be marked required.");
               }
               XClass javaType = getJavaTypeFromAnnotation(annotation);
               if (javaType == typeFactory.fromClass(void.class)) {
                  javaType = isRepeated ? field.determineRepeatedElementType() : field.getType();
               }
               if (javaType == typeFactory.fromClass(byte[].class) && protobufType == Type.MESSAGE) {
                  // MESSAGE is the default and stands for 'undefined', we can override it with a better default
                  protobufType = Type.BYTES;
               }
               if (!javaType.isArray() && !javaType.isPrimitive() && javaType.isAbstract() && !javaType.isEnum()) {
                  throw Log.LOG.abstractType(javaType.getCanonicalName(), fieldName, clazz.getCanonicalName());
               }

               protobufType = getProtobufType(javaType, protobufType);

               ProtoTypeMetadata protoTypeMetadata = null;
               if (protobufType.getJavaType() == JavaType.ENUM || protobufType.getJavaType() == JavaType.MESSAGE) {
                  protoTypeMetadata = protoSchemaGenerator.scanAnnotations(javaType);
               }

               ProtoFieldMetadata fieldMetadata;
               if (isMap) {
                  // Determine the map implementation
                  XClass repeatedImplementation = getMapImplementation(clazz, field.getType(), getMapImplementationFromAnnotation(annotation), fieldName, isRepeated);
                  XClass keyJavaType = field.getTypeArgument(0);
                  Type keyProtobufType = getProtobufType(keyJavaType, Type.MESSAGE);
                  if (!keyProtobufType.isValidMapKey()) {
                     throw new ProtoSchemaBuilderException("The key of the map field '" + fieldName + "' of " + clazz.getName() + " must be either a String or an integral type, while processing " + this.protoSchemaGenerator.generator);
                  }
                  fieldMetadata = new ProtoMapMetadata(number, fieldName, keyJavaType, javaType, repeatedImplementation, keyProtobufType, protobufType, protoTypeMetadata, field);
               } else {
                  Object defaultValue = getDefaultValue(clazz, fieldName, javaType, protobufType, annotation == null ? "" : annotation.defaultValue(), isRepeated);
                  if (!isRequired && !isRepeated && javaType.isPrimitive() && defaultValue == null) {
                     throw new ProtoSchemaBuilderException("Primitive field '" + fieldName + "' of " + clazz.getCanonicalName() + " is not nullable so it should be either marked required or should have a default value, while processing " + this.protoSchemaGenerator.generator);
                  }
                  // Handle oneof
                  String oneof = validateOneOf(clazz, fieldsByName, oneofs, annotation, fieldName, isRepeated, isRequired);
                  // Determine the collection implementation
                  XClass repeatedImplementation;
                  if (isArray) {
                     repeatedImplementation = typeFactory.fromClass(ArrayList.class);
                  } else {
                     repeatedImplementation = getCollectionImplementation(clazz, field.getType(), getCollectionImplementationFromAnnotation(annotation), fieldName, isRepeated);
                  }
                  fieldMetadata = new ProtoFieldMetadata(number, fieldName, oneof, javaType, repeatedImplementation,
                        protobufType, protoTypeMetadata, isRequired, isRepeated, isArray, defaultValue, field);
               }

               ProtoFieldMetadata existing = fieldsByNumber.get(number);
               if (existing != null) {
                  throw new ProtoSchemaBuilderException("Duplicate field number definition. Found two field definitions with number " + number + ": in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation() + ", while processing " + this.protoSchemaGenerator.generator);
               }
               existing = fieldsByName.get(fieldMetadata.getName());
               if (existing != null) {
                  throw new ProtoSchemaBuilderException("Duplicate field name definition. Found two field definitions with name '" + fieldMetadata.getName() + "': in "
                        + fieldMetadata.getLocation() + " and in " + existing.getLocation() + ", while processing " + this.protoSchemaGenerator.generator);
               }

               checkReserved(fieldMetadata);
               fieldsByNumber.put(fieldMetadata.getNumber(), fieldMetadata);
               fieldsByName.put(fieldName, fieldMetadata);
            }
         }
      }
   }

   private static String getName(ProtoField annotation, XField field) {
      if (annotation == null || annotation.name().isEmpty()) {
         return field.getName();
      } else {
         return annotation.name();
      }
   }

   private String validateOneOf(XClass clazz, Map<String, ProtoFieldMetadata> fieldsByName, Set<String> oneofs, ProtoField annotation, String fieldName, boolean isRepeated, boolean isRequired) {
      if (annotation == null) {
         return null;
      }
      String oneof = annotation.oneof();
      if (oneof.isEmpty()) {
         oneof = null;
      } else {
         if (oneof.equals(fieldName) || fieldsByName.containsKey(oneof)) {
            throw new ProtoSchemaBuilderException("The field named '" + fieldName + "' of " + clazz.getName() + " is member of the '" + oneof + "' oneof which collides with an existing field or oneof, while processing " + this.protoSchemaGenerator.generator);
         }
         if (isRepeated || isRequired) {
            throw new ProtoSchemaBuilderException("The field named '" + fieldName + "' of " + clazz.getName() + " cannot be marked repeated or required since it is member of the '" + oneof + " oneof, while processing " + this.protoSchemaGenerator.generator);
         }
         oneofs.add(oneof);
      }
      return oneof;
   }

   private void validateField(XClass clazz, XField field) {
      if (isAdapter) {
         throw new ProtoSchemaBuilderException("No ProtoStream annotations should be present on fields when @ProtoAdapter is present on a class : " + clazz.getCanonicalName() + '.' + field);
      }
      if (field.isStatic()) {
         throw new ProtoSchemaBuilderException("Static fields cannot be @ProtoField annotated: " + clazz.getCanonicalName() + '.' + field);
      }
      if (factory == null && field.isFinal()) { //todo [anistor] maybe allow this
         throw new ProtoSchemaBuilderException("Final fields cannot be @ProtoField annotated: " + clazz.getCanonicalName() + '.' + field);
      }
      if (field.isPrivate()) {
         throw new ProtoSchemaBuilderException("Private fields cannot be @ProtoField annotated: " + clazz.getCanonicalName() + '.' + field);
      }
   }

   private String detectPropertyNameFromSetter(XMethod method) {
      if (isAdapter && method.getParameterTypes().length != 2 || !isAdapter && method.getParameterTypes().length != 1) {
         throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
      }
      if (method.getName().startsWith("set") && method.getName().length() > 3) {
         return Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
      } else {
         return method.getName(); // throw new ProtoSchemaBuilderException("Illegal setter method signature: " + method);
      }
   }

   private String determinePropertyNameFromGetter(XMethod method) {
      if (isAdapter && method.getParameterTypes().length != 1 || !isAdapter && method.getParameterTypes().length != 0) {
         throw new ProtoSchemaBuilderException("Illegal getter method signature: " + method);
      }
      if (method.getName().startsWith("get") && method.getName().length() > 3) {
         return Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
      } else if (method.getName().startsWith("is") && method.getName().length() > 2) {
         return Character.toLowerCase(method.getName().charAt(2)) + method.getName().substring(3);
      } else {
         // not a standard java-beans getter
         return method.getName();  //throw new ProtoSchemaBuilderException("Illegal getter method signature: " + method);
      }
   }

   private void discoverFieldsFromRecord(XClass clazz, Map<Integer, ProtoFieldMetadata> fieldsByNumber, Map<String, ProtoFieldMetadata> fieldsByName) {
      String[] parameterNames = factory.getParameterNames();
      XClass[] parameterTypes = factory.getParameterTypes();
      for (int i = 0; i < factory.getParameterCount(); i++) {
         int fieldNumber = i + 1;
         String fieldName = parameterNames[i];
         XClass javaType = parameterTypes[i];
         ProtoField annotation = javaType.getAnnotation(ProtoField.class);

         Type protobufType = defaultType(annotation, javaType);

         XMethod getter = clazz.getMethod(fieldName);
         boolean isArray = isArray(javaType, protobufType);
         boolean isRepeated = isRepeated(javaType, protobufType);
         boolean isMap = isMap(javaType);

         // Determine the collection/map implementation
         XClass repeatedImplementation = null;
         if (isMap) {
            repeatedImplementation = getMapImplementation(clazz, javaType, getMapImplementationFromAnnotation(annotation), fieldName, true);
         } else if (isArray) {
            repeatedImplementation = typeFactory.fromClass(ArrayList.class);
         } else if (isRepeated) {
            repeatedImplementation = getCollectionImplementation(clazz, javaType, getCollectionImplementationFromAnnotation(annotation), fieldName, true);
         }

         if (isRepeated) {
            javaType = getter.determineRepeatedElementType();
         }

         if (javaType == typeFactory.fromClass(byte[].class) && protobufType == Type.MESSAGE) {
            // MESSAGE is the default and stands for 'undefined', we can override it with a better default
            protobufType = Type.BYTES;
         }
         if (!javaType.isArray() && !javaType.isPrimitive() && javaType.isAbstract() && !javaType.isEnum()) {
            throw new ProtoSchemaBuilderException("The type " + javaType.getCanonicalName() + " of field '" + fieldName + "' of " + clazz.getCanonicalName() + " should not be abstract.");
         }

         protobufType = getProtobufType(javaType, protobufType);
         ProtoTypeMetadata protoTypeMetadata = null;
         if (protobufType.getJavaType() == JavaType.ENUM || protobufType.getJavaType() == JavaType.MESSAGE) {
            protoTypeMetadata = protoSchemaGenerator.scanAnnotations(javaType);
         }
         String oneof = null;
         Object defaultValue;
         if (annotation == null) {
            defaultValue = getDefaultValue(clazz, fieldName, javaType, protobufType, "", false);
         } else {
            if (annotation.number() > 0) fieldNumber = annotation.number();
            if (!annotation.name().isEmpty()) fieldName = annotation.name();
            if (!annotation.oneof().isEmpty()) oneof = annotation.oneof();
            defaultValue = getDefaultValue(clazz, fieldName, javaType, protobufType, annotation.defaultValue(), false);
         }
         ProtoFieldMetadata fieldMetadata;
         if (isMap) {
            XClass keyJavaType = getter.getTypeArgument(0);
            Type keyType = getProtobufType(keyJavaType, Type.MESSAGE);
            if (!keyType.isValidMapKey()) {
               throw new ProtoSchemaBuilderException("The key of the map field '" + fieldName + "' of " + clazz.getName() + " must be either a String or an integral type, while processing " + this.protoSchemaGenerator.generator);
            }
            fieldMetadata = new ProtoMapMetadata(fieldNumber, fieldName, keyJavaType, javaType, repeatedImplementation, keyType, protobufType, protoTypeMetadata, fieldName, getter, getter, null);
         } else {
            fieldMetadata = new ProtoFieldMetadata(fieldNumber, fieldName, oneof, javaType,
                  repeatedImplementation, protobufType, protoTypeMetadata,
                  false, isRepeated, isArray, defaultValue, fieldName,
                  getter, getter, null);
         }
         checkReserved(fieldMetadata);
         fieldsByNumber.put(fieldMetadata.getNumber(), fieldMetadata);
         fieldsByName.put(fieldMetadata.getName(), fieldMetadata);
      }
   }

   private static int getNumber(ProtoField annotation, XMember member) {
      int number = annotation.number();
      if (number == 0) {
         number = annotation.value();
      } else if (annotation.value() != 0) {
         throw new ProtoSchemaBuilderException("@ProtoField.number() and value() are mutually exclusive: " + member);
      }
      if (number < 1) {
         throw new ProtoSchemaBuilderException("Protobuf field numbers specified by @ProtoField.number() or value() must be greater than 0: " + member);
      }
      return number;
   }

   private static void checkReserved(ProtoFieldMetadata fieldMetadata) {
      if (fieldMetadata.getNumber() >= 19000 && fieldMetadata.getNumber() <= 19999) {
         throw new ProtoSchemaBuilderException("Field numbers 19000 through 19999 are reserved for internal use by the protobuf specification: "
               + fieldMetadata.getLocation());
      }
      // TODO [anistor] IPROTO-98 also check reserved numbers and names
   }

   protected XClass getCollectionImplementationFromAnnotation(ProtoField annotation) {
      return annotation == null ? typeFactory.fromClass(Collection.class) : typeFactory.fromClass(annotation.collectionImplementation());
   }

   protected XClass getMapImplementationFromAnnotation(ProtoField annotation) {
      return annotation == null ? typeFactory.fromClass(Map.class) : typeFactory.fromClass(annotation.mapImplementation());
   }

   protected XClass getJavaTypeFromAnnotation(ProtoField annotation) {
      return annotation == null ? typeFactory.fromClass(void.class) : typeFactory.fromClass(annotation.javaType());
   }

   /**
    * Parses the value from string form (coming from proto schema) to an actual Java instance value, according to its
    * type.
    */
   private Object getDefaultValue(XClass clazz, String fieldName, XClass fieldType, Type protobufType, String value, boolean isRepeated) {
      if (protoSchemaGenerator.syntax() == ProtoSyntax.PROTO2 ||
            (protoSchemaGenerator.allowNullFields() && !fieldType.isPrimitive())) {
         if (value == null || value.isEmpty()) {
            return null;
         }
      }
      if (fieldType == typeFactory.fromClass(String.class)) {
         return isRepeated ? null : value;
      }
      Optional<String> defaultValue = value == null || value.isEmpty() ? Optional.empty() : Optional.of(value);
      if (fieldType.isEnum()) {
         ProtoTypeMetadata protoEnumTypeMetadata = protoSchemaGenerator.scanAnnotations(fieldType);
         ProtoEnumValueMetadata enumVal;
         if (defaultValue.isEmpty()) {
            enumVal = protoEnumTypeMetadata.getEnumMemberByNumber(0);
         } else {
            enumVal = protoEnumTypeMetadata.getEnumMemberByName(defaultValue.get());
         }
         if (enumVal == null) {
            throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue.orElse("0") + " is not a member of " + protoEnumTypeMetadata.getFullName() + " enum");
         }
         return enumVal;
      }
      if (fieldType == typeFactory.fromClass(Character.class) || fieldType == typeFactory.fromClass(char.class)) {
         if (defaultValue.isEmpty() || defaultValue.get().length() > 1) {
            throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue);
         }
         return defaultValue.get().charAt(0);
      }
      if (fieldType == typeFactory.fromClass(Boolean.class) || fieldType == typeFactory.fromClass(boolean.class)) {
         return Boolean.valueOf(defaultValue.orElse("false"));
      }
      try {
         if (fieldType == typeFactory.fromClass(Integer.class) || fieldType == typeFactory.fromClass(int.class)) {
            if (defaultValue.isEmpty() && isRepeated) {
               return null;
            }
            int v = defaultValue.map(ProtoMessageTypeMetadata::parseInt).orElse(0);
            if (v < 0 && protobufType.isUnsigned()) {
               throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of unsigned Protobuf type " + protobufType + " from class " + clazz.getCanonicalName() + " does not allow a negative default value : " + defaultValue);
            }
            return v;
         }
         if (fieldType == typeFactory.fromClass(Long.class) || fieldType == typeFactory.fromClass(long.class)) {
            if (defaultValue.isEmpty() && isRepeated) {
               return null;
            }
            long v = defaultValue.map(ProtoMessageTypeMetadata::parseLong).orElse(0L);
            if (v < 0 && protobufType.isUnsigned()) {
               throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of unsigned Protobuf type " + protobufType + " from class " + clazz.getCanonicalName() + " does not allow a negative default value : " + defaultValue);
            }
            return v;
         }
         if (fieldType == typeFactory.fromClass(Short.class) || fieldType == typeFactory.fromClass(short.class)) {
            if (defaultValue.isEmpty() && isRepeated) {
               return null;
            }
            int v = defaultValue.map(ProtoMessageTypeMetadata::parseInt).orElse(0);
            if (v < 0 && protobufType.isUnsigned()) {
               throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of unsigned Protobuf type " + protobufType + " from class " + clazz.getCanonicalName() + " does not allow a negative default value : " + defaultValue);
            }
            if (v < Short.MIN_VALUE || v > Short.MAX_VALUE) {
               throw new NumberFormatException("Value out of range for \"" + protobufType + "\": \"" + defaultValue);
            }
            return (short) v;
         }
         if (fieldType == typeFactory.fromClass(Double.class) || fieldType == typeFactory.fromClass(double.class)) {
            if (defaultValue.isEmpty() && isRepeated) {
               return null;
            }
            return defaultValue.map(Double::parseDouble).orElse(0.0D);
         }
         if (fieldType == typeFactory.fromClass(Float.class) || fieldType == typeFactory.fromClass(float.class)) {
            if (defaultValue.isEmpty() && isRepeated) {
               return null;
            }
            return defaultValue.map(Float::parseFloat).orElse(0.0F);
         }
         if (fieldType == typeFactory.fromClass(Byte.class) || fieldType == typeFactory.fromClass(byte.class)) {
            if (defaultValue.isEmpty() && isRepeated) {
               return null;
            }
            int v = defaultValue.map(ProtoMessageTypeMetadata::parseInt).orElse(0);
            if (v < 0 && protobufType.isUnsigned()) {
               throw new ProtoSchemaBuilderException("Field '" + fieldName + "' of unsigned Protobuf type " + protobufType + " from class " + clazz.getCanonicalName() + " does not allow a negative default value : " + defaultValue);
            }
            if (v < Byte.MIN_VALUE || v > Byte.MAX_VALUE) {
               throw new NumberFormatException("Value out of range for \"" + protobufType + "\": \"" + defaultValue);
            }
            return (byte) v;
         }
         if (fieldType.isAssignableTo(Date.class) || fieldType.isAssignableTo(Instant.class)) {
            return defaultValue.map(Long::parseUnsignedLong).orElse(0L);
         }
      } catch (NumberFormatException e) {
         throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue, e);
      }
      if (protobufType == Type.BYTES) {
         if (fieldType == typeFactory.fromClass(byte[].class)) {
            return cescape(defaultValue);
         } else {
            throw new ProtoSchemaBuilderException("Invalid default value for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName() + ": " + defaultValue);
         }
      }
      if (defaultValue.isPresent()) {
         throw new ProtoSchemaBuilderException("No default value is allowed for field '" + fieldName + "' of Java type " + fieldType.getCanonicalName() + " from class " + clazz.getCanonicalName());
      }
      return null;
   }

   /**
    * C-style escaping using 3 digit octal escapes ({@code "\xxx"}) for all non-ASCII chars.
    */
   static byte[] cescape(Optional<String> s) {
      return s.map(string -> cescape(string.getBytes(StandardCharsets.UTF_8))).orElse(EMPTY_BYTES);
   }

   static byte[] cescape(byte[] bytes) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
      for (byte b : bytes) {
         int ub = Byte.toUnsignedInt(b);
         if (ub < 32 || ub > 127) {  // printable 7-bit chars are fine, the rest of all non-US-ASCII chars get escaped !
            baos.write('\\');
            baos.write('0' + ((ub >> 6) & 0x7));  // 3 digit octal code
            baos.write('0' + ((ub >> 3) & 0x7));
            baos.write('0' + (ub & 0x7));
         } else {
            baos.write(ub);
         }
      }
      return baos.toByteArray();
   }

   private static long parseLong(String value) {
      if (value == null) {
         throw new IllegalArgumentException("value argument cannot be null");
      }
      if (value.isEmpty()) {
         throw new NumberFormatException("Empty input string");
      }
      String s = value;
      boolean isNegative = false;

      if (s.charAt(0) == '-') {
         isNegative = true;
         s = s.substring(1);
      }

      int radix;
      if (s.length() > 1 && s.charAt(0) == '0') {
         if (s.length() > 2 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
            radix = 16;
            s = s.substring(2);
         } else {
            radix = 8;
            s = s.substring(1);
         }
      } else {
         radix = 10;
      }

      try {
         long v = Long.parseUnsignedLong(s, radix);
         return isNegative ? -v : v;
      } catch (NumberFormatException e) {
         throw new NumberFormatException("For input string: \"" + value + "\"");
      }
   }

   private static int parseInt(String value) {
      if (value == null) {
         throw new IllegalArgumentException("value argument cannot be null");
      }
      if (value.isEmpty()) {
         throw new NumberFormatException("Empty input string");
      }
      String s = value;
      boolean isNegative = false;

      if (s.charAt(0) == '-') {
         isNegative = true;
         s = s.substring(1);
      }

      int radix;
      if (s.length() > 1 && s.charAt(0) == '0') {
         if (s.length() > 2 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
            radix = 16;
            s = s.substring(2);
         } else {
            radix = 8;
            s = s.substring(1);
         }
      } else {
         radix = 10;
      }

      try {
         int v = Integer.parseUnsignedInt(s, radix);
         return isNegative ? -v : v;
      } catch (NumberFormatException e) {
         throw new NumberFormatException("For input string: \"" + value + "\"");
      }
   }

   private XClass getCollectionImplementation(XClass clazz, XClass fieldType, XClass configuredCollection, String fieldName, boolean isRepeated) {
      XClass collectionImplementation;

      XClass javaUtilCollectionClass = typeFactory.fromClass(Collection.class);
      if (isRepeated && !fieldType.isArray()) {
         collectionImplementation = configuredCollection;
         if (collectionImplementation == javaUtilCollectionClass) {   // default
            if (fieldType == typeFactory.fromClass(Set.class)) {
               collectionImplementation = typeFactory.fromClass(HashSet.class);
            } else if (fieldType == typeFactory.fromClass(List.class) || fieldType == typeFactory.fromClass(Collection.class)) {
               collectionImplementation = typeFactory.fromClass(ArrayList.class);
            } else {
               collectionImplementation = fieldType;
            }
         }
         if (!collectionImplementation.isAssignableTo(javaUtilCollectionClass)) {
            throw new ProtoSchemaBuilderException("The collection class of repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " must implement java.util.Collection.");
         }
         if (collectionImplementation.isAbstract()) {
            throw new ProtoSchemaBuilderException("The collection class (" + collectionImplementation.getCanonicalName() + ") of repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " must not be abstract. Please specify an appropriate class in collectionImplementation member.");
         }
         XConstructor ctor = collectionImplementation.getDeclaredConstructor();
         if (ctor == null || ctor.isPrivate()) {
            throw new ProtoSchemaBuilderException("The collection class ('" + collectionImplementation.getCanonicalName() + "') of repeated field '"
                  + fieldName + "' of " + clazz.getCanonicalName() + " must have a public no-argument constructor.");
         }
         if (!collectionImplementation.isAssignableTo(fieldType)) {
            throw new ProtoSchemaBuilderException("The collection implementation class ('" + collectionImplementation.getCanonicalName() + "') of repeated field '"
                  + fieldName + "' of " + clazz.getCanonicalName() + " is not assignable to this field's type.");
         }
      } else {
         if (configuredCollection != javaUtilCollectionClass) {
            throw new ProtoSchemaBuilderException("Specifying the collection implementation class is only allowed for collection (repeated) fields: '" + fieldName + "' of " + clazz.getCanonicalName());
         }
         collectionImplementation = null;
      }

      return collectionImplementation;
   }

   private XClass getMapImplementation(XClass clazz, XClass fieldType, XClass configuredMap, String fieldName, boolean isRepeated) {
      XClass mapImplementation;

      XClass javaUtilMapClass = typeFactory.fromClass(Map.class);
      if (isRepeated && !fieldType.isArray()) {
         mapImplementation = configuredMap;
         if (mapImplementation == javaUtilMapClass) {   // default
            if (fieldType == typeFactory.fromClass(Map.class)) {
               mapImplementation = typeFactory.fromClass(HashMap.class);
            } else if (fieldType == typeFactory.fromClass(ConcurrentMap.class) || fieldType == typeFactory.fromClass(ConcurrentHashMap.class)) {
               mapImplementation = typeFactory.fromClass(ConcurrentHashMap.class);
            } else if (fieldType == typeFactory.fromClass(SortedMap.class)) {
               mapImplementation = typeFactory.fromClass(TreeMap.class);
            } else {
               mapImplementation = fieldType;
            }
         }
         if (!mapImplementation.isAssignableTo(javaUtilMapClass)) {
            throw new ProtoSchemaBuilderException("The map class of repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " must implement java.util.Map.");
         }
         if (mapImplementation.isAbstract()) {
            throw new ProtoSchemaBuilderException("The map class (" + mapImplementation.getCanonicalName() + ") of repeated field '" + fieldName + "' of " + clazz.getCanonicalName() + " must not be abstract. Please specify an appropriate class in mapImplementation member.");
         }
         XConstructor ctor = mapImplementation.getDeclaredConstructor();
         if (ctor == null || ctor.isPrivate()) {
            throw new ProtoSchemaBuilderException("The map class ('" + mapImplementation.getCanonicalName() + "') of repeated field '"
                  + fieldName + "' of " + clazz.getCanonicalName() + " must have a public no-argument constructor.");
         }
         if (!mapImplementation.isAssignableTo(fieldType)) {
            throw new ProtoSchemaBuilderException("The map implementation class ('" + mapImplementation.getCanonicalName() + "') of repeated field '"
                  + fieldName + "' of " + clazz.getCanonicalName() + " is not assignable to this field's type.");
         }
      } else {
         if (configuredMap != javaUtilMapClass) {
            throw new ProtoSchemaBuilderException("Specifying the map implementation class is only allowed for map fields: '" + fieldName + "' of " + clazz.getCanonicalName());
         }
         mapImplementation = null;
      }

      return mapImplementation;
   }

   private Type getProtobufType(XClass javaType, Type declaredType) {
      switch (declaredType) {
         case MESSAGE:
            // MESSAGE means either 'unspecified' or MESSAGE
            if (javaType.isEnum()) {
               ProtoTypeMetadata m = protoSchemaGenerator.scanAnnotations(javaType);
               if (!m.isEnum()) {
                  throw new ProtoSchemaBuilderException(javaType.getCanonicalName() + " is not a Protobuf marshallable enum type");
               }
               return Type.ENUM;
            } else if (javaType == typeFactory.fromClass(String.class)) {
               return Type.STRING;
            } else if (javaType == typeFactory.fromClass(Double.class) || javaType == typeFactory.fromClass(double.class)) {
               return Type.DOUBLE;
            } else if (javaType == typeFactory.fromClass(Float.class) || javaType == typeFactory.fromClass(float.class)) {
               return Type.FLOAT;
            } else if (javaType == typeFactory.fromClass(Long.class) || javaType == typeFactory.fromClass(long.class)) {
               return Type.INT64;
            } else if (javaType == typeFactory.fromClass(Integer.class) || javaType == typeFactory.fromClass(int.class) ||
                  javaType == typeFactory.fromClass(Short.class) || javaType == typeFactory.fromClass(short.class) ||
                  javaType == typeFactory.fromClass(Byte.class) || javaType == typeFactory.fromClass(byte.class) ||
                  javaType == typeFactory.fromClass(Character.class) || javaType == typeFactory.fromClass(char.class)) {
               return Type.INT32;
            } else if (javaType == typeFactory.fromClass(Boolean.class) || javaType == typeFactory.fromClass(boolean.class)) {
               return Type.BOOL;
            } else if (javaType.isAssignableTo(Date.class)) {
               return Type.FIXED64;
            } else if (javaType.isAssignableTo(Instant.class)) {
               return Type.FIXED64;
            } else {
               ProtoTypeMetadata m = protoSchemaGenerator.scanAnnotations(javaType);
               if (m.isEnum()) {
                  throw new ProtoSchemaBuilderException(javaType.getCanonicalName() + " is not a Protobuf marshallable message type");
               }
            }
            break;
         case ENUM:
            if (!javaType.isEnum()) {
               throw new ProtoSchemaBuilderException(javaType.getCanonicalName() + " is not a Protobuf marshallable enum type");
            }
            break;
         case GROUP:
            ProtoTypeMetadata m = protoSchemaGenerator.scanAnnotations(javaType);
            if (m.isEnum()) {
               throw new ProtoSchemaBuilderException(javaType.getCanonicalName() + " is not a Protobuf marshallable message type");
            }
            break;
         case STRING:
            if (javaType != typeFactory.fromClass(String.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case BYTES:
            if (javaType != typeFactory.fromClass(byte[].class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case DOUBLE:
            if (javaType != typeFactory.fromClass(Double.class) && javaType != typeFactory.fromClass(double.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case FLOAT:
            if (javaType != typeFactory.fromClass(Float.class) && javaType != typeFactory.fromClass(float.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case BOOL:
            if (javaType != typeFactory.fromClass(Boolean.class) && javaType != typeFactory.fromClass(boolean.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case INT32:
         case UINT32:
         case FIXED32:
         case SFIXED32:
         case SINT32:
            if (javaType != typeFactory.fromClass(Integer.class) && javaType != typeFactory.fromClass(int.class)
                  && javaType != typeFactory.fromClass(Short.class) && javaType != typeFactory.fromClass(short.class)
                  && javaType != typeFactory.fromClass(Byte.class) && javaType != typeFactory.fromClass(byte.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
         case INT64:
         case UINT64:
         case FIXED64:
         case SFIXED64:
         case SINT64:
            if (javaType != typeFactory.fromClass(Long.class) && javaType != typeFactory.fromClass(long.class)
                  && !javaType.isAssignableTo(Date.class) && !javaType.isAssignableTo(Instant.class))
               throw new ProtoSchemaBuilderException("Incompatible types : " + javaType.getCanonicalName() + " vs " + declaredType);
            break;
      }
      return declaredType;
   }

   private boolean isArray(XClass javaType, Type type) {
      if (type == Type.BYTES && javaType == typeFactory.fromClass(byte[].class)) {
         // A byte[] mapped to BYTES needs special handling. This will not be mapped to a repeatable field.
         return false;
      }
      return javaType.isArray();
   }

   private boolean isRepeated(XClass javaType, Type type) {
      if (type == Type.BYTES && javaType == typeFactory.fromClass(byte[].class)) {
         // A byte[] mapped to BYTES needs special handling. This will not be mapped to a repeatable field.
         return false;
      }
      return javaType.isArray() || javaType.isAssignableTo(Collection.class) || javaType.isAssignableTo(Map.class);
   }

   private boolean isMap(XClass javaType) {
      return javaType.isAssignableTo(Map.class);
   }

   private XMethod findGetter(String propertyName, XClass propertyType) {
      boolean isBoolean = propertyType == typeFactory.fromClass(boolean.class) || propertyType == typeFactory.fromClass(Boolean.class);
      String methodName = (isBoolean ? "is" : "get") + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

      XMethod getter;
      if (isAdapter) {
         // lookup a java-bean style method first
         getter = annotatedClass.getMethod(methodName);
         if (getter == null && isBoolean) {
            // retry with 'get' instead of 'is'
            methodName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            getter = annotatedClass.getMethod(methodName, javaClass);
         }
         if (getter == null) {
            // try the property name directly
            getter = annotatedClass.getMethod(propertyName, javaClass);
         }
         if (getter == null) {
            throw new ProtoSchemaBuilderException("No getter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getAnnotatedClassName());
         }
         XClass returnType = getter.getReturnType();
         if (returnType == typeFactory.fromClass(Optional.class)) {
            returnType = getter.determineOptionalReturnType();
         }
         if (returnType != propertyType) {
            throw new ProtoSchemaBuilderException("No suitable getter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getAnnotatedClassName()
                  + ". The candidate method does not have a suitable return type: " + getter);
         }
      } else {
         // lookup a java-bean style method first
         getter = javaClass.getMethod(methodName);
         if (getter == null && isBoolean) {
            // retry with 'get' instead of 'is'
            methodName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            getter = javaClass.getMethod(methodName);
         }
         if (getter == null) {
            // try the property name directly
            getter = javaClass.getMethod(propertyName);
         }
         if (getter == null) {
            throw new ProtoSchemaBuilderException("No getter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + javaClass.getCanonicalName());
         }
         XClass returnType = getter.getReturnType();
         if (returnType == typeFactory.fromClass(Optional.class)) {
            returnType = getter.determineOptionalReturnType();
         }
         if (returnType != propertyType) {
            throw new ProtoSchemaBuilderException("No suitable getter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + javaClass.getCanonicalName()
                  + ". The candidate method does not have a suitable return type: " + getter);
         }
      }
      return getter;
   }

   private XMethod findSetter(String propertyName, XClass propertyType) {
      String methodName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      XMethod setter;
      if (isAdapter) {
         // lookup a java-bean style method first
         setter = annotatedClass.getMethod(methodName, javaClass, propertyType);
         if (setter == null) {
            // try the property name directly
            setter = annotatedClass.getMethod(propertyName, javaClass, propertyType);
         }
         if (setter == null) {
            throw new ProtoSchemaBuilderException("No setter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getAnnotatedClassName());
         }
         if (setter.getReturnType() != typeFactory.fromClass(void.class)) {
            throw new ProtoSchemaBuilderException("No suitable setter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getAnnotatedClassName()
                  + ". The candidate method does not have a suitable return type: " + setter);
         }
      } else {
         // lookup a java-bean style method first
         setter = javaClass.getMethod(methodName, propertyType);
         if (setter == null) {
            // try the property name directly
            setter = javaClass.getMethod(propertyName, propertyType);
         }
         if (setter == null) {
            throw new ProtoSchemaBuilderException("No setter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getJavaClassName());
         }
         if (setter.getReturnType() != typeFactory.fromClass(void.class)) {
            throw new ProtoSchemaBuilderException("No suitable setter method found for property '" + propertyName
                  + "' of type " + propertyType.getCanonicalName() + " in class " + getJavaClassName()
                  + ". The candidate method does not have a suitable return type: " + setter);
         }
      }
      return setter;
   }

   private void checkForbiddenAnnotations(XMethod m1, XMethod m2) {
      if (m1.getAnnotation(ProtoComment.class) != null
            || m1.getAnnotation(ProtoField.class) != null
            || m1.getAnnotation(ProtoUnknownFieldSet.class) != null) {
         throw new ProtoSchemaBuilderException("No @ProtoDoc, @ProtoField or @ProtoUnknownFieldSet annotations allowed on method " + m1 + ". They should have been added to " + m2);
      }
   }

   @Override
   public String toString() {
      return "ProtoMessageTypeMetadata{" +
            "name='" + name + '\'' +
            ", javaClass=" + javaClass +
            ", annotatedClass=" + annotatedClass +
            ", isAdapter=" + isAdapter +
            ", fieldsByNumber=" + fieldsByNumber +
            ", factory=" + factory +
            ", unknownFieldSetField=" + unknownFieldSetField +
            ", unknownFieldSetGetter=" + unknownFieldSetGetter +
            ", unknownFieldSetSetter=" + unknownFieldSetSetter +
            ", innerTypes=" + innerTypes +
            '}';
   }
}
