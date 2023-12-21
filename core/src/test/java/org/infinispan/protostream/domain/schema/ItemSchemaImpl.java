package org.infinispan.protostream.domain.schema;

import org.infinispan.protostream.domain.Item;

public class ItemSchemaImpl implements Item.ItemSchema {

   private static final String PROTO_SCHEMA =
   "syntax = \"proto2\";\n" +
   "\n" +
   "message Item {\n" +
   "   \n" +
   "   optional string code = 1;\n" +
   "   \n" +
   "   optional bytes byteVector = 2;\n" +
   "   \n" +
   "   repeated float floatVector = 3;\n" +
   "   \n" +
   "   repeated int32 integerVector = 4;\n" +
   "   \n" +
   "   optional string buggy = 5;\n" +
   "}\n" +
   "";
   
   @Override
   public String getProtoFileName() { return "ItemSchema.proto"; }
   
   @Override
   public String getProtoFile() { return PROTO_SCHEMA; }
   
   @Override
   public void registerSchema(org.infinispan.protostream.SerializationContext serCtx) {
      serCtx.registerProtoFiles(org.infinispan.protostream.FileDescriptorSource.fromString(getProtoFileName(), getProtoFile()));
   }
   
   @Override
   public void registerMarshallers(org.infinispan.protostream.SerializationContext serCtx) {
      serCtx.registerMarshaller(new ItemMarshaller());
   }
}
