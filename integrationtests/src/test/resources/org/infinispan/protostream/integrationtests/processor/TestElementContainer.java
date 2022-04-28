package test_element_container;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;
import org.infinispan.protostream.containers.IterableElementContainerAdapter;


@AutoProtoSchemaBuilder(
      schemaFilePath = "/",
      includeClasses = {
            IntArrayAdapter.class,
            ArrayListAdapter.class,
            HashSetAdapter.class
      }
)
public interface TestElementContainer extends GeneratedSchema {
}

@ProtoAdapter(int[].class)
@ProtoName("IntArray")
final class IntArrayAdapter implements IndexedElementContainerAdapter<int[], Integer> {

   @ProtoFactory
   public int[] create(int theSize) {
      return new int[theSize];
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

@ProtoAdapter(ArrayList.class)
final class ArrayListAdapter<T> implements IndexedElementContainerAdapter<ArrayList<T>, T> {

   @ProtoFactory
   public ArrayList<T> create(int numElements) {
      return new ArrayList<>(numElements);
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
      while (container.size() <= index) {
         container.add(null);
      }
      container.set(index, element);
   }
}

@ProtoAdapter(HashSet.class)
final class HashSetAdapter<T> implements IterableElementContainerAdapter<HashSet<T>, T> {

   @ProtoFactory
   public HashSet<T> create(int size) {
      return new HashSet<>(size);
   }

   @Override
   public int getNumElements(HashSet<T> container) {
      return container.size();
   }

   @Override
   public Iterator<T> getElements(HashSet<T> container) {
      return container.iterator();
   }

   @Override
   public void appendElement(HashSet<T> container, T element) {
      container.add(element);
   }
}
