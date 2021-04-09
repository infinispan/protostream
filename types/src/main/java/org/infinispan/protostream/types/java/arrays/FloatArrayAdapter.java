package org.infinispan.protostream.types.java.arrays;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoAdapter(float[].class)
@ProtoName("FloatArray")
public final class FloatArrayAdapter implements IndexedElementContainerAdapter<float[], Float> {

   @ProtoFactory
   public float[] create(int size) {
      return new float[size];
   }

   @Override
   public int getNumElements(float[] array) {
      return array.length;
   }

   @Override
   public Float getElement(float[] array, int index) {
      return array[index];
   }

   @Override
   public void setElement(float[] array, int index, Float element) {
      array[index] = element;
   }
}
