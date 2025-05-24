package org.infinispan.protostream.types.java;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
        includeClasses = {
                Book.class
        },
        schemaFileName = "book.proto",
        schemaFilePath = "org/infinispan/protostream/types",
        schemaPackageName = "library")
public interface BookSchema extends GeneratedSchema {
}
