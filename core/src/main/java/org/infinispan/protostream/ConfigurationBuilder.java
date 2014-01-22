package org.infinispan.protostream;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class ConfigurationBuilder {

   private boolean logOutOfSequenceReads = true;

   private boolean logOutOfSequenceWrites = true;

   public ConfigurationBuilder() {
   }

   public boolean isLogOutOfSequenceReads() {
      return logOutOfSequenceReads;
   }

   public ConfigurationBuilder setLogOutOfSequenceReads(boolean logOutOfSequenceReads) {
      this.logOutOfSequenceReads = logOutOfSequenceReads;
      return this;
   }

   public boolean isLogOutOfSequenceWrites() {
      return logOutOfSequenceWrites;
   }

   public ConfigurationBuilder setLogOutOfSequenceWrites(boolean logOutOfSequenceWrites) {
      this.logOutOfSequenceWrites = logOutOfSequenceWrites;
      return this;
   }

   public Configuration build() {
      return new Configuration(logOutOfSequenceReads, logOutOfSequenceWrites);
   }
}
