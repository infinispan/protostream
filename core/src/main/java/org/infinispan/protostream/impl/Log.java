package org.infinispan.protostream.impl;

import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;

import org.infinispan.protostream.MalformedProtobufException;
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

   class LogFactory {
      public static Log getLog(Class<?> clazz) {
         return Logger.getMessageLogger(Log.class, clazz.getName());
      }
   }
}
