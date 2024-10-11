package org.infinispan.protostream.domain.schema;

import org.infinispan.protostream.domain.Item;

public class ItemSchemaImpl implements Item.ItemSchema {

   private static final String PROTO_SCHEMA =
         """
               syntax = "proto2";
               message Item {
                  optional string code = 1;
                  optional bytes byteVector = 2;
                  repeated float floatVector = 3;
                  repeated int32 integerVector = 4;
                  optional string buggy = 5;
               }
               """;

   @Override
   public String getProtoFileName() {
      return "ItemSchema.proto";
   }

   @Override
   public String getProtoFile() {
      return PROTO_SCHEMA;
   }

   @Override
   public void registerSchema(org.infinispan.protostream.SerializationContext serCtx) {
      serCtx.registerProtoFiles(org.infinispan.protostream.FileDescriptorSource.fromString(getProtoFileName(), getProtoFile()));
   }

   @Override
   public void registerMarshallers(org.infinispan.protostream.SerializationContext serCtx) {
      serCtx.registerMarshaller(new ItemMarshaller());
   }
}
