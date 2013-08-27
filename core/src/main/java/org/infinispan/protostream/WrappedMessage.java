package org.infinispan.protostream;

/**
 * @author anistor@redhat.com
 */
public final class WrappedMessage {

   public static final String PROTOBUF_TYPE_NAME = "org.infinispan.protostream.WrappedMessage";

   private final Object value;

   public WrappedMessage(Object value) {
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

      return !(value != null ? !value.equals(that.value) : that.value != null);
   }

   @Override
   public int hashCode() {
      return value != null ? value.hashCode() : 0;
   }
}
