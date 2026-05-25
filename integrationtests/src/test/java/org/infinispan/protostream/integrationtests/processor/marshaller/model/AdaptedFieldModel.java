package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import java.util.UUID;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class AdaptedFieldModel {

   private final UUID uuid;

   @ProtoFactory
   public AdaptedFieldModel(UUID uuid) {
      this.uuid = uuid;
   }

   @ProtoField(1)
   public UUID getUuid() {
      return uuid;
   }
}
