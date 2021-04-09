package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(double[].class)
@ProtoName("DoubleArray")
public final class DoubleArrayAdapter implements IndexedElementContainerAdapter<double[], Double> {

   @ProtoFactory
   public double[] create(int size) {
      return new double[size];
   }

   @Override
   public int getNumElements(double[] array) {
      return array.length;
   }

   @Override
   public Double getElement(double[] array, int index) {
      return array[index];
   }

   @Override
   public void setElement(double[] array, int index, Double element) {
      array[index] = element;
   }
}
