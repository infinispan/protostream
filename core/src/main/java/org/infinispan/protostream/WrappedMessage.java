package org.infinispan.protostream;

/**
 * @author anistor@redhat.com
 */
public final class WrappedMessage {

   public static final String PROTOBUF_TYPE_NAME = "org.infinispan.protostream.WrappedMessage";

   private final Object value;

   public WrappedMessage(Object value) {
      if (value == null) {
         throw new IllegalArgumentException("value cannot be null");
      }
      this.value = value;
   }

   public Object getValue() {
      return value;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      WrappedMessage that = (WrappedMessage) o;
      return value.equals(that.value);

   }

   @Override
   public int hashCode() {
      return value.hashCode();
   }
}
