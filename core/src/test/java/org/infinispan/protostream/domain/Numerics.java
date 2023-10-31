package org.infinispan.protostream.domain;

import java.util.Objects;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class Numerics {

   public Numerics() { }

   @ProtoFactory
   public Numerics(byte simpleByte, short simpleShort,
                   int simpleInt, long simpleLong,
                   float simpleFloat, double simpleDouble) {
      this.simpleByte = simpleByte;
      this.simpleShort = simpleShort;
      this.simpleInt = simpleInt;
      this.simpleLong = simpleLong;
      this.simpleFloat = simpleFloat;
      this.simpleDouble = simpleDouble;
   }

   @ProtoField(number = 1, defaultValue = "0")
   byte simpleByte;

   @ProtoField(number = 2, defaultValue = "0")
   short simpleShort;

   @ProtoField(number = 3, defaultValue = "0")
   int simpleInt;

   @ProtoField(number = 4, defaultValue = "0")
   long simpleLong;

   @ProtoField(number = 5, defaultValue = "0")
   float simpleFloat;

   @ProtoField(number = 6, defaultValue = "0")
   double simpleDouble;

   public byte simpleByte() {
      return simpleByte;
   }

   public short simpleShort() {
      return simpleShort;
   }

   public int simpleInt() {
      return simpleInt;
   }

   public long simpleLong() {
      return simpleLong;
   }

   public float simpleFloat() {
      return simpleFloat;
   }

   public double simpleDouble() {
      return simpleDouble;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Numerics numerics = (Numerics) o;
      return simpleByte == numerics.simpleByte
            && simpleShort == numerics.simpleShort
            && simpleInt == numerics.simpleInt
            && simpleLong == numerics.simpleLong
            && Float.compare(simpleFloat, numerics.simpleFloat) == 0
            && Double.compare(simpleDouble, numerics.simpleDouble) == 0;
   }

   @Override
   public int hashCode() {
      return Objects.hash(simpleByte, simpleShort, simpleInt, simpleLong, simpleFloat, simpleDouble);
   }
}
