package org.infinispan.protostream.impl;

import org.junit.Test;

import java.util.BitSet;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * @author anistor@redhat.com
 */
public class FastIntegerSetTest {

   @Test
   public void testSetOperations() {
      BitSet bitSet = new BitSet(10);
      bitSet.set(1);
      bitSet.set(2);
      bitSet.set(3);
      bitSet.set(5);
      bitSet.set(8);
      bitSet.set(13);
      bitSet.set(21);

      // test size
      FastIntegerSet intSet = new FastIntegerSet(bitSet);
      assertEquals(7, intSet.size());
      assertFalse(intSet.isEmpty());

      // test add/remove
      assertTrue(intSet.contains(5));
      bitSet.clear(5);
      assertFalse(intSet.contains(5));
      intSet.add(5);
      assertTrue(bitSet.get(5));
      intSet.remove(5);
      assertFalse(bitSet.get(5));

      // test iteration
      Integer[] values = new Integer[intSet.size()];
      int k = 0;
      for (Iterator<Integer> it = intSet.iterator(); it.hasNext(); ) {
         values[k++] = it.next();
      }
      assertArrayEquals(new Integer[]{1, 2, 3, 8, 13, 21}, values);

      // test clear
      intSet.clear();
      assertEquals(0, intSet.size());
      assertTrue(intSet.isEmpty());
      assertEquals(0, bitSet.cardinality());
   }
}
