package org.infinispan.protostream.types.java;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
      includeClasses = {
            ListOfAdapter.class,
            EmptyListAdapter.class,
            SingletonListAdapter.class
      },
      schemaFileName = "list.proto",
      schemaFilePath = "proto/",
      schemaPackageName = "collections")
public interface ListSchema extends GeneratedSchema {
}
