package org.infinispan.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Contract to be implemented by manually written marshallers for Protobuf message (entity) types. The marshaller
 * implementation must be stateless and thread-safe.
 *
 * @author anistor@redhat.com
 * @since 1.0
 */
public interface MessageMarshaller<T> extends BaseMarshaller<T> {

   /**
    * Read the fields written by {@link #writeTo(ProtoStreamWriter, Object)}. Should read them in the exact same order
    * as were written to ensure maximum performance. Not obeying the order will lead to poor performance and will cause
    * warnings to be logged but will still work.
    */
   T readFrom(ProtoStreamReader reader) throws IOException;

   /**
    * Write the fields defined in the schema. Please establish a consistent order and always write them in the same
    * order. Two common choices for ordering field writes are schema definition order and field number order.
    */
   void writeTo(ProtoStreamWriter writer, T t) throws IOException;

   /**
    * A high-level interface for the wire encoding of a Protobuf stream that allows reading named (and typed) message
    * fields.
    */
   interface ProtoStreamReader {

      /**
       * During reading, a marshaller can obtain the current {@link ImmutableSerializationContext} and use it in order
       * to access the schema or marshaller information.
       */
      ImmutableSerializationContext getSerializationContext();

      /**
       * Reads an integer field given a field name. The field name and type are checked against the schema first. Can't
       * return an {@code int} here because the field might be declared optional and actually missing so we have to be
       * able to signal that by returning {@code null}.
       */
      Integer readInt(String fieldName) throws IOException;

      int[] readInts(String fieldName) throws IOException;

      Long readLong(String fieldName) throws IOException;  //TODO implement 'type-based' default values where a default value was not specified in schema

      long[] readLongs(String fieldName) throws IOException;

      Date readDate(String fieldName) throws IOException;

      Instant readInstant(String fieldName) throws IOException;

      Float readFloat(String fieldName) throws IOException;

      float[] readFloats(String fieldName) throws IOException;

      Double readDouble(String fieldName) throws IOException;

      double[] readDoubles(String fieldName) throws IOException;

      Boolean readBoolean(String fieldName) throws IOException;

      boolean[] readBooleans(String fieldName) throws IOException;

      String readString(String fieldName) throws IOException;

      byte[] readBytes(String fieldName) throws IOException;

      InputStream readBytesAsInputStream(String fieldName) throws IOException;

      <E extends Enum<E>> E readEnum(String fieldName, Class<E> clazz) throws IOException;

      <E> E readObject(String fieldName, Class<E> clazz) throws IOException;

      <E, C extends Collection<? super E>> C readCollection(String fieldName, C collection, Class<E> elementClass) throws IOException;

      <E> E[] readArray(String fieldName, Class<? extends E> elementClass) throws IOException;

      <K, V, M extends Map<? super K, ? super V>> M readMap(String fieldName, M map, Class<K> keyClass, Class<V> valueClass) throws IOException;
   }

   /**
    * A high-level interface for the wire encoding of a Protobuf stream that allows writing named (and typed) message
    * fields.
    */
   interface ProtoStreamWriter {

      /**
       * During reading, a marshaller can obtain the current {@link ImmutableSerializationContext} and use it in order
       * to access the schema or marshaller information.
       */
      ImmutableSerializationContext getSerializationContext();

      void writeInt(String fieldName, int value) throws IOException;

      void writeInt(String fieldName, Integer value) throws IOException;

      void writeInts(String fieldName, int[] values) throws IOException;

      void writeLong(String fieldName, long value) throws IOException;

      void writeLong(String fieldName, Long value) throws IOException;

      void writeLongs(String fieldName, long[] values) throws IOException;

      void writeDate(String fieldName, Date value) throws IOException;

      void writeInstant(String fieldName, Instant value) throws IOException;

      void writeDouble(String fieldName, double value) throws IOException;

      void writeDouble(String fieldName, Double value) throws IOException;

      void writeDoubles(String fieldName, double[] values) throws IOException;

      void writeFloat(String fieldName, float value) throws IOException;

      void writeFloat(String fieldName, Float value) throws IOException;

      void writeFloats(String fieldName, float[] values) throws IOException;

      void writeBoolean(String fieldName, boolean value) throws IOException;

      void writeBoolean(String fieldName, Boolean value) throws IOException;

      void writeBooleans(String fieldName, boolean[] values) throws IOException;

      void writeString(String fieldName, String value) throws IOException;

      void writeBytes(String fieldName, byte[] value) throws IOException;

      void writeBytes(String fieldName, InputStream input) throws IOException;

      <E> void writeObject(String fieldName, E value, Class<? extends E> clazz) throws IOException;

      /**
       * Writes an enum value.
       *
       * @param fieldName the field name
       * @param value     the enum value
       */
      <E extends Enum<E>> void writeEnum(String fieldName, E value) throws IOException;

      <E> void writeCollection(String fieldName, Collection<? super E> collection, Class<E> elementClass) throws IOException;

      <E> void writeArray(String fieldName, E[] array, Class<? extends E> elementClass) throws IOException;

      <K, V> void writeMap(String fieldName, Map<? super K, ? super V> map, Class<K> keyClass, Class<V> valueClass) throws IOException;
   }
}
