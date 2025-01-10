package org.infinispan.protostream.domain;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@ProtoAdapter(Pair.class)
public class PairAdapter {

   @ProtoFactory
   public Pair<?, ?> create(WrappedMessage left, WrappedMessage right) {
      return new Pair<>(left.getValue(), right.getValue());
   }

   @ProtoField(number = 1)
   public WrappedMessage getLeft(Pair<?, ?> pair) {
      return new WrappedMessage(pair.left());
   }

   @ProtoField(number = 2)
   public WrappedMessage getRight(Pair<?, ?> pair) {
      return new WrappedMessage(pair.right());
   }
}
