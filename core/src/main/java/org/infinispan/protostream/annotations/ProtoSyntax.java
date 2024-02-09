package org.infinispan.protostream.annotations;

public enum ProtoSyntax {
   PROTO2,
   PROTO3;

   @Override
   public String toString() {
      return name().toLowerCase();
   }
}
