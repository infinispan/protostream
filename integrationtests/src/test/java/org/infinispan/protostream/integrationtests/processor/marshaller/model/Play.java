package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import java.util.List;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoSchema;

@Proto
public record Play(String name, List<String> matches) {

   @ProtoSchema(includeClasses = { Play.class }, schemaPackageName = "my",
         schemaFileName = "play.proto", schemaFilePath = "proto")
   public interface PlaySchema extends GeneratedSchema {
   }
}
