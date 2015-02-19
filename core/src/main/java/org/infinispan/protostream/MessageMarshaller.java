package org.infinispan.protostream;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface MessageMarshaller<T> extends BaseMarshaller<T> {

   T readFrom(ProtoStreamReader reader) throws IOException;

   void writeTo(ProtoStreamWriter writer, T t) throws IOException;

   /**
    * An high-level interface for the wire encoding of a protobuf stream that allows reading named and typed message
    * fields.
    */
   interface ProtoStreamReader {

      SerializationContext getSerializationContext();

      /**
       * Can't return an {@code int} here because the field might be declared optional and missing so we might need to
       * return a {@code null}.
       */
      Integer readInt(String fieldName) throws IOException;

      Long readLong(String fieldName) throws IOException;

      Date readDate(String fieldName) throws IOException;

      Float readFloat(String fieldName) throws IOException;

      Double readDouble(String fieldName) throws IOException;

      Boolean readBoolean(String fieldName) throws IOException;

      String readString(String fieldName) throws IOException;

      byte[] readBytes(String fieldName) throws IOException;

      <E> E readObject(String fieldName, Class<E> clazz) throws IOException;

      <E, C extends Collection<? super E>> C readCollection(String fieldName, C collection, Class<E> elementClass) throws IOException;

      <E> E[] readArray(String fieldName, Class<? extends E> elementClass) throws IOException;
   }

   interface ProtoStreamWriter {

      SerializationContext getSerializationContext();

      void writeInt(String fieldName, Integer value) throws IOException;

      void writeInt(String fieldName, int value) throws IOException;

      void writeLong(String fieldName, long value) throws IOException;

      void writeLong(String fieldName, Long value) throws IOException;

      void writeDate(String fieldName, Date value) throws IOException;

      void writeDouble(String fieldName, double value) throws IOException;

      void writeDouble(String fieldName, Double value) throws IOException;

      void writeFloat(String fieldName, float value) throws IOException;

      void writeFloat(String fieldName, Float value) throws IOException;

      void writeBoolean(String fieldName, boolean value) throws IOException;

      void writeBoolean(String fieldName, Boolean value) throws IOException;

      void writeString(String fieldName, String value) throws IOException;

      void writeBytes(String fieldName, byte[] value) throws IOException;

      <E> void writeObject(String fieldName, E value, Class<? extends E> clazz) throws IOException;

      <E> void writeCollection(String fieldName, Collection<? super E> collection, Class<E> elementClass) throws IOException;

      <E> void writeArray(String fieldName, E[] array, Class<? extends E> elementClass) throws IOException;
   }
}
