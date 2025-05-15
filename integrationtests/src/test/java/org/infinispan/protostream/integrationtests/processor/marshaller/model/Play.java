package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import java.util.List;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoSchema;

@Proto
public record Play(
      @ProtoField(number = 1, name = "play_name")
      String name,
      @ProtoField(number = 2)
      List<String> matches
) {

   @ProtoSchema(includeClasses = {Play.class}, schemaPackageName = "my",
         schemaFileName = "play.proto", schemaFilePath = "proto")
   public interface PlaySchema extends GeneratedSchema {
   }
}
