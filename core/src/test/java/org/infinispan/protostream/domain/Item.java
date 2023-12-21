package org.infinispan.protostream.domain;

import java.util.Arrays;
import java.util.Objects;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.domain.schema.ItemSchemaImpl;

public class Item {

   private String code;

   private byte[] byteVector;

   private float[] floatVector;

   private int[] integerVector;

   private String buggy;

   @ProtoFactory
   public Item(String code, byte[] byteVector, float[] floatVector, int[] integerVector, String buggy) {
      this.code = code;
      this.byteVector = byteVector;
      this.floatVector = floatVector;
      this.integerVector = integerVector;
      this.buggy = buggy;
   }

   @ProtoField(1)
   public String getCode() {
      return code;
   }

   @ProtoField(2)
   public byte[] getByteVector() {
      return byteVector;
   }

   @ProtoField(3)
   public float[] getFloatVector() {
      return floatVector;
   }

   @ProtoField(4)
   public int[] getIntegerVector() {
      return integerVector;
   }

   @ProtoField(5)
   public String getBuggy() {
      return buggy;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Item item = (Item) o;
      return Objects.equals(code, item.code) && Arrays.equals(byteVector, item.byteVector) && Arrays.equals(floatVector, item.floatVector) && Arrays.equals(integerVector, item.integerVector) && Objects.equals(buggy, item.buggy);
   }

   @Override
   public int hashCode() {
      int result = Objects.hash(code, buggy);
      result = 31 * result + Arrays.hashCode(byteVector);
      result = 31 * result + Arrays.hashCode(floatVector);
      result = 31 * result + Arrays.hashCode(integerVector);
      return result;
   }

   @Override
   public String toString() {
      return "Item{" +
            "code='" + code + '\'' +
            ", byteVector=" + Arrays.toString(byteVector) +
            ", floatVector=" + Arrays.toString(floatVector) +
            ", integerVector=" + Arrays.toString(integerVector) +
            ", buggy='" + buggy + '\'' +
            '}';
   }

   public interface ItemSchema extends GeneratedSchema {
      ItemSchema INSTANCE = new ItemSchemaImpl();
   }
}
