package org.infinispan.protostream.annotations.impl.testdomain;

import java.util.Collection;

import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public class TestArraysAndCollectionsClass2 {

   private Integer anInt1;

   private int anInt2;

   private Integer[] ints1;

   private int[] ints2;

   private Collection<Integer> ints3;

   private Collection<String> strings;

   @ProtoField(number = 1)
   public Integer getAnInt1() {
      return anInt1;
   }

   public void setAnInt1(Integer anInt1) {
      this.anInt1 = anInt1;
   }

   @ProtoField(number = 2)
   public int getAnInt2() {
      return anInt2;
   }

   public void setAnInt2(int anInt2) {
      this.anInt2 = anInt2;
   }

   @ProtoField(number = 3)
   public Integer[] getInts1() {
      return ints1;
   }

   public void setInts1(Integer[] ints1) {
      this.ints1 = ints1;
   }

   @ProtoField(number = 4)
   public int[] getInts2() {
      return ints2;
   }

   public void setInts2(int[] ints2) {
      this.ints2 = ints2;
   }

   @ProtoField(number = 5)
   public Collection<Integer> getInts3() {
      return ints3;
   }

   public void setInts3(Collection<Integer> ints3) {
      this.ints3 = ints3;
   }

   @ProtoField(number = 6)
   public Collection<String> getStrings() {
      return strings;
   }

   public void setStrings(Collection<String> strings) {
      this.strings = strings;
   }

   public TestArraysAndCollectionsClass2() {
   }
}
