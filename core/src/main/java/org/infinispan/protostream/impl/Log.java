package org.infinispan.protostream.impl;

import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author anistor@redhat.com
 */
@MessageLogger(projectCode = "PROTOSTREAM")
public interface Log extends BasicLogger {

   @LogMessage(level = WARN)
   @Message(value = "Field %s was read out of sequence leading to sub-optimal performance", id = 1)
   void fieldReadOutOfSequence(String fieldName);

   @LogMessage(level = WARN)
   @Message(value = "Field %s was written out of sequence and will lead to sub-optimal read performance", id = 2)
   void fieldWriteOutOfSequence(String fieldName);

   class LogFactory {
      public static Log getLog(Class<?> clazz) {
         return Logger.getMessageLogger(Log.class, clazz.getName());
      }
   }
}
