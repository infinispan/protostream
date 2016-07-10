package org.infinispan.protostream.annotations.impl.testdomain;

import java.util.ArrayList;
import java.util.Collection;

import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public class TestArraysAndCollectionsClass {

   @ProtoField(number = 1)
   public Integer anInt1;

   @ProtoField(number = 2, defaultValue = "0")
   public int anInt2;

   @ProtoField(number = 3)
   public Integer[] ints1;

   @ProtoField(number = 4)
   public int[] ints2;

   @ProtoField(number = 5, collectionImplementation = ArrayList.class)
   public Collection<Integer> ints3;

   @ProtoField(number = 6, collectionImplementation = ArrayList.class)
   public Collection<String> strings;

   public TestArraysAndCollectionsClass() {
   }
}
