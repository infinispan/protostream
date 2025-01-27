package org.infinispan.protostream.impl.json;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.containers.ElementContainerAdapter;
import org.infinispan.protostream.descriptors.GenericDescriptor;

final class JsonHelper {

   private JsonHelper() { }

   static final int MAP_KEY_FIELD = 1;
   static final int MAP_VALUE_FIELD = 2;
   static final String NULL = "null";
   static final byte[] EMPTY_ARRAY = {};
   static final String JSON_TYPE_FIELD = "_type";
   static final String JSON_VALUE_FIELD = "_value";
   static final String JSON_WHITESPACE = "   ";

   static boolean isContainerAdapter(ImmutableSerializationContext ctx, GenericDescriptor descriptor) {
      return descriptor != null && ctx.getMarshaller(descriptor.getFullName()) instanceof ElementContainerAdapter<?>;
   }
}
