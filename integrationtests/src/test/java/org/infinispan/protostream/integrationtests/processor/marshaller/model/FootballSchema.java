package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { FootballTeam.class, Player.class }, schemaPackageName = "org.football",
      schemaFileName = "football.proto", schemaFilePath = "proto")
public interface FootballSchema extends GeneratedSchema {

}
