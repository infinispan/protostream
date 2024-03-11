package org.infinispan.protostream.impl;

import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.MalformedProtobufException;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.exception.ProtoStreamException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author anistor@redhat.com
 */
@MessageLogger(projectCode = "IPROTO")
public interface Log extends BasicLogger {
   Log LOG = Logger.getMessageLogger(Log.class, "org.infinispan.PROTOSTREAM");

   @LogMessage(level = WARN)
   @Message(value = "Field %s was read out of sequence leading to sub-optimal performance", id = 1)
   void fieldReadOutOfSequence(String fieldName);

   @LogMessage(level = WARN)
   @Message(value = "Field %s was written out of sequence and will lead to sub-optimal read performance", id = 2)
   void fieldWriteOutOfSequence(String fieldName);

   @Message(value = "Input data ended unexpectedly in the middle of a field. The message is corrupt.", id = 3)
   MalformedProtobufException messageTruncated(@Cause Throwable cause);

   default MalformedProtobufException messageTruncated() {
      return messageTruncated(null);
   }

   @Message(value = "Encountered a malformed varint.", id = 4)
   MalformedProtobufException malformedVarint();

   @Message(value = "Encountered a length delimited field with negative length.", id = 5)
   MalformedProtobufException negativeLength();

   @Message(value = "Protobuf message appears to be larger than the configured limit. The message is possibly corrupt.", id = 6)
   MalformedProtobufException globalLimitExceeded();

   @Message(value = "Ran out of buffer space", id = 7)
   IOException outOfWriteBufferSpace(@Cause Throwable cause);

   @Message(value = "The nested message depth appears to be larger than the configured limit of '%s'." +
         "It is possible that the entity to marshall with type '%s' can have some circular dependencies.", id = 8)
   ProtoStreamException maxNestedMessageDepth(int maxNestedMessageDepth, Class<?> entityType);

   @Message(value = "Not a repeatable field: %s#%s", id = 9)
   IllegalStateException notRepeatableField(String clazz, String fieldOrMethod);

   @Message(value = "Name '%s' is reserved on `%s`", id = 10)
   IllegalArgumentException reservedName(String name, String owner);

   @Message(value = "Number %d used by '%s' is reserved on '%s'", id = 11)
   IllegalArgumentException reservedNumber(int number, String name, String owner);

   @Message(value = "Unsupported protocol buffers syntax '%s'", id = 12)
   IllegalArgumentException unsupportedSyntax(FileDescriptor.Syntax s);

   @Message(value = "Error while parsing '%s': %s", id = 13)
   DescriptorParserException parserException(String filename, String message);

   @Message(value = "The type %s of field %s of %s should not be abstract.", id = 14)
   ProtoSchemaBuilderException abstractType(String canonicalName, String fieldName, String canonicalName1);

   @Message(value = "The field named '%s' of %s is a member of the '%s' oneof which collides with an existing field or oneof.", id = 15)
   ProtoSchemaBuilderException oneofCollision(String fieldName, String name, String oneof);

   @Message(value = "The field named '%s' of %s cannot be marked repeated or required since it is member of the '%s' oneof.", id = 16)
   ProtoSchemaBuilderException oneofRepeatedOrRequired(String fieldName, String name, String oneof);

   @Message(value = "Abstract classes are not allowed: '%s'", id = 17)
   ProtoSchemaBuilderException abstractClassNotAllowed(String annotatedClassName);

   @Message(value = "Local or anonymous classes are not allowed. The class '%s' must be instantiable using an accessible no-argument constructor.", id = 18)
   ProtoSchemaBuilderException localOrAnonymousClass(String annotatedClassName);

   @Message(value = "Non-static inner classes are not allowed. The class '%s' must be instantiable using an accessible no-argument constructor.", id = 19)
   ProtoSchemaBuilderException nonStaticInnerClass(String annotatedClassName);

   @Message(value = "Invalid default value for field '%s' of Java type %s from class %s: the %s enum must have a 0 value", id = 20)
   ProtoSchemaBuilderException noDefaultEnum(String fieldName, String canonicalName, String canonicalName1, String fullName);

   @Message(value = "@ProtoFactory annotated %s signature mismatch. The first parameter is expected to be of type 'int' : %s", id = 21)
   ProtoSchemaBuilderException factorySignatureMismatch(String kind, String factory);

   @Message(value = "@ProtoFactory annotated %s signature mismatch. Expected %d parameters but found %d : %s", id = 22)
   ProtoSchemaBuilderException factorySignatureMismatch(String kind, int expected, int found, String factory);

   @Message(value = "@ProtoFactory annotated %s signature mismatch. The parameter '%s' does not match any field : %s", id = 23)
   ProtoSchemaBuilderException factorySignatureMismatch(String kind, String parameterName, String factory);

   @Message(value = "@ProtoFactory annotated %s signature mismatch: %s. The parameter '%s' does not match the type from the field definition.", id = 24)
   ProtoSchemaBuilderException factorySignatureMismatchType(String kind, String factory, String parameterName);

   @Message(value = "Found more than one @ProtoFactory annotated method / constructor : %s", id = 25)
   ProtoSchemaBuilderException multipleFactories(String s);

   @Message(value = "@ProtoFactory annotated constructor must not be private: %s", id = 26)
   ProtoSchemaBuilderException privateFactory(String s);

   @Message(value = "@ProtoFactory annotated method must be static: %s", id =27)
   ProtoSchemaBuilderException nonStaticFactory(String s);

   @Message(value = "@ProtoFactory annotated method has wrong return type: %s", id = 28)
   ProtoSchemaBuilderException wrongFactoryReturnType(String s);

   @Message(value = "Value `%s` on enum `%s` must be annotated with @ProtoEnumValue", id = 29)
   ProtoSchemaBuilderException explicitEnumValueAnnotations(String value, String name);

   class LogFactory {
      public static Log getLog(Class<?> clazz) {
         return Logger.getMessageLogger(Log.class, clazz.getName());
      }
   }
}
