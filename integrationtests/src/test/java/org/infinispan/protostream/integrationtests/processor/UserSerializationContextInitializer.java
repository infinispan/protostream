package org.infinispan.protostream.integrationtests.processor;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.domain.User;
import org.infinispan.protostream.domain.Address;

@AutoProtoSchemaBuilder(
      includeClasses = {
            User.class,
            Address.class,
      },
      schemaFileName = "user.proto",
      schemaFilePath = "proto/",
      schemaPackageName = "user_sample")
public interface UserSerializationContextInitializer extends GeneratedSchema {
      public static UserSerializationContextInitializer INSTANCE = new UserSerializationContextInitializerImpl();
}
