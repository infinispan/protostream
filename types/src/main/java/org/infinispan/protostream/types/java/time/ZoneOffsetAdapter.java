package org.infinispan.protostream.types.java.time;

import java.time.ZoneOffset;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

@ProtoAdapter(ZoneOffset.class)
public class ZoneOffsetAdapter {
   @ProtoFactory
   ZoneOffset create(String id) {
      return ZoneOffset.of(id);
   }
   @ProtoField(number = 1, type = Type.STRING)
   String getId(ZoneOffset offset) {
      return offset.getId();
   }
}
