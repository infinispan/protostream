package org.infinispan.protostream.domain;

public class Pair<L, R> {
   private final L l;
   private final R r;

   public Pair(L l, R r) {
      this.l = l;
      this.r = r;
   }

   public L left() {
      return l;
   }

   public R right() {
      return r;
   }
}
