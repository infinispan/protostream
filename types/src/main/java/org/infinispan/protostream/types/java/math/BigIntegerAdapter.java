package org.infinispan.protostream.types.java.math;

import java.math.BigInteger;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@ProtoAdapter(BigInteger.class)
public class BigIntegerAdapter {

   @ProtoFactory
   BigInteger create(byte[] bytes) {
      return new BigInteger(bytes);
   }

   @ProtoField(1)
   byte[] getBytes(BigInteger bigInteger) {
      return bigInteger.toByteArray();
   }
}
