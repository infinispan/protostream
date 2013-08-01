package org.infinispan.protostream.impl;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.MessageLogger;

/**
 * @author anistor@redhat.com
 */
@MessageLogger(projectCode = "PROTOSTREAM")
public interface Log extends BasicLogger {

   class LogFactory {
      public static Log getLog(Class<?> clazz) {
         return Logger.getMessageLogger(Log.class, clazz.getName());
      }
   }
}