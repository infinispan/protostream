package org.infinispan.protostream.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;
import org.infinispan.protostream.containers.IterableElementContainerAdapter;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author anistor@redhat.com
 */
public class ElementContainerTest {

   @ProtoAdapter(int[].class)
   @ProtoName("IntArray")
   public static final class IntArrayAdapter implements IndexedElementContainerAdapter<int[], Integer> {

      //todo [anistor] The generated marshaller must get size from context and pass it to factory/constructor
      @ProtoFactory
      public int[] create(/*int whateverNameFirstArgForSize*/) {
         return new int[5/*whateverNameFirstArgForSize*/];
      }

      @Override
      public int getNumElements(int[] container) {
         return container.length;
      }

      @Override
      public Integer getElement(int[] container, int index) {
         return container[index];
      }

      @Override
      public void setElement(int[] container, int index, Integer element) {
         container[index] = element;
      }
   }

   @Test
   public void testIntArrayMarshallingWithAdapter() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      String schema = new ProtoSchemaBuilder()
            .fileName("test_container.proto")
            .packageName("myTestPackage")
            .addClass(IntArrayAdapter.class)
            .build(ctx);

      assertTrue(schema.contains("\nmessage IntArray {\n}\n"));
      assertTrue(ctx.canMarshall(int[].class));

      int[] dataIn = new int[]{3, 1, 4, 1, 5};
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, dataIn);

      Object dataOut = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertTrue(dataOut instanceof int[]);
      assertArrayEquals(dataIn, (int[]) dataOut);
   }

   @ProtoAdapter(ArrayList.class)
   public static final class ArrayListAdapter1<T> implements IndexedElementContainerAdapter<ArrayList<T>, T> {

      //todo [anistor] The generated marshaller must get size from context and pass it to factory/constructor
      @ProtoFactory
      public ArrayList<?> create(/*int whateverNameFirstArgForSize*/) {
         return new ArrayList<>(/*whateverNameFirstArgForSize*/);
      }

      @Override
      public int getNumElements(ArrayList<T> container) {
         return container.size();
      }

      @Override
      public T getElement(ArrayList<T> container, int index) {
         return container.get(index);
      }

      @Override
      public void setElement(ArrayList<T> container, int index, T element) {
         if (index >= container.size())
            container.add(null); //todo [anistor] The generated marshaller must get size from context and pass it to factory/constructor
         container.set(index, element);
      }
   }

   @Test
   public void testArrayListMarshallingWithAdapter1() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      String schema = new ProtoSchemaBuilder()
            .fileName("test_container.proto")
            .packageName("myTestPackage")
            .addClass(ArrayListAdapter1.class)
            .build(ctx);

      assertTrue(schema.contains("\nmessage ArrayList {\n}\n"));
      assertTrue(ctx.canMarshall(ArrayList.class));

      ArrayList<Integer> dataIn = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5));
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, dataIn);

      Object dataOut = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertTrue(dataOut instanceof ArrayList);
      assertEquals(dataIn, dataOut);
   }

   @ProtoAdapter(ArrayList.class)
   public static final class ArrayListAdapter2<T> implements IterableElementContainerAdapter<ArrayList<T>, T> {

      @ProtoFactory
      public ArrayList<?> create(/*int whateverNameFirstArgForSize*/) {
         return new ArrayList<>(/*whateverNameFirstArgForSize*/);
      }

      @Override
      public int getNumElements(ArrayList<T> container) {
         return container.size();
      }

      @Override
      public Iterator<T> getElements(ArrayList<T> container) {
         return container.iterator();
      }

      @Override
      public void appendElement(ArrayList<T> container, T element) {
         container.add(element);
      }
   }

   @Test
   public void testArrayListMarshallingWithAdapter2() throws Exception {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();

      String schema = new ProtoSchemaBuilder()
            .fileName("test_container.proto")
            .packageName("myTestPackage")
            .addClass(ArrayListAdapter2.class)
            .build(ctx);

      assertTrue(schema.contains("\nmessage ArrayList {\n}\n"));
      assertTrue(ctx.canMarshall(ArrayList.class));

      ArrayList<Integer> dataIn = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5));
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, dataIn);

      Object dataOut = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertTrue(dataOut instanceof ArrayList);
      assertEquals(dataIn, dataOut);
   }
}
