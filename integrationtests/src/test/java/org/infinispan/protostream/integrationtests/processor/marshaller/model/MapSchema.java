package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.types.java.CommonTypes;

@ProtoSchema(
        syntax = ProtoSyntax.PROTO3,
        schemaPackageName = "generic",
        schemaFilePath = "proto",
        schemaFileName = "generic.proto",
        dependsOn = CommonTypes.class,
        includeClasses = {
                ModelWithMap.class,
                SimpleEnum.class
        }
)
public interface MapSchema extends GeneratedSchema {

    MapSchema INSTANCE = new MapSchemaImpl();
}
