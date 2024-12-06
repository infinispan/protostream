package org.infinispan.protostream.types.java;

import java.util.Arrays;
import java.util.List;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@ProtoAdapter(
      value = List.class,
      subClassNames = {
            "java.util.ImmutableCollections$ListN",
            "java.util.ImmutableCollections$List12"
      }
)
public class ListOfAdapter {
   @ProtoFactory
   List<?> create(WrappedMessage[] elements) {
      return elements == null ? null :
      List.of(Arrays.stream(elements).map(WrappedMessage::getValue).toArray());
   }

   @ProtoField(1)
   WrappedMessage[] elements(List<?> list) {
      return list == null ? null : list.stream().map(WrappedMessage::new).toArray(WrappedMessage[]::new);
   }
}
