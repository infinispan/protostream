package org.infinispan.protostream.integrationtests.processor;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.PairAdapter;
import org.infinispan.protostream.domain.User;

@AutoProtoSchemaBuilder(
      includeClasses = {
            User.class,
            Address.class,
            PairAdapter.class
      },
      schemaFileName = "user.proto",
      schemaFilePath = "proto/",
      schemaPackageName = "user_sample",
      syntax = ProtoSyntax.PROTO3
)
public interface UserSerializationContextInitializer extends GeneratedSchema {
      UserSerializationContextInitializer INSTANCE = new UserSerializationContextInitializerImpl();
}
