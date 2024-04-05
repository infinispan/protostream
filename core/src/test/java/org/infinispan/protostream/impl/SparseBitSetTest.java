package org.infinispan.protostream.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SparseBitSetTest {
   @Test
   public void testNextSetBit() {
      SparseBitSet set = new SparseBitSet();
      set.set(1);
      set.set(30);
      set.set(1000);
      assertEquals(1, set.nextSetBit(1));
      assertEquals(30, set.nextSetBit(2));
      assertEquals(1000, set.nextSetBit(31));
      assertEquals(-1, set.nextSetBit(1001));
   }

   @Test
   public void testNextClearBit() {
      SparseBitSet set = new SparseBitSet();
      set.set(1);
      set.set(30, 35);
      assertEquals(2, set.nextClearBit(1));
      assertEquals(35, set.nextClearBit(30));
   }
}
