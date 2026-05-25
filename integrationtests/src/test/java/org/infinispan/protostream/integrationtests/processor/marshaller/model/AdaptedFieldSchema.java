package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.types.java.CommonTypes;

@ProtoSchema(
      syntax = ProtoSyntax.PROTO3,
      schemaPackageName = "adapted",
      schemaFilePath = "proto",
      schemaFileName = "adapted.proto",
      dependsOn = CommonTypes.class,
      includeClasses = AdaptedFieldModel.class
)
public interface AdaptedFieldSchema extends GeneratedSchema {
   AdaptedFieldSchema INSTANCE = new AdaptedFieldSchemaImpl();
}
