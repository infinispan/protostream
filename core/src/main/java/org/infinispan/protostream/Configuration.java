package org.infinispan.protostream;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class Configuration {

   private final boolean logOutOfSequenceReads;

   private final boolean logOutOfSequenceWrites;

   Configuration(boolean logOutOfSequenceReads, boolean logOutOfSequenceWrites) {
      this.logOutOfSequenceReads = logOutOfSequenceReads;
      this.logOutOfSequenceWrites = logOutOfSequenceWrites;
   }

   public boolean logOutOfSequenceReads() {
      return logOutOfSequenceReads;
   }

   public boolean logOutOfSequenceWrites() {
      return logOutOfSequenceWrites;
   }

   @Override
   public String toString() {
      return "Configuration{" +
            "logOutOfSequenceReads=" + logOutOfSequenceReads +
            ", logOutOfSequenceWrites=" + logOutOfSequenceWrites +
            '}';
   }
}