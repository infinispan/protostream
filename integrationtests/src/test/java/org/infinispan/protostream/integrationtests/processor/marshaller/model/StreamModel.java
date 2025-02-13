package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import java.util.Arrays;
import java.util.stream.Stream;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoSchema;

public class StreamModel {

   private StreamModel() {}

   @ProtoSchema(
         includeClasses = {
               Factory.class,
               GetterSetter.class
         },
         schemaPackageName = "org.protostream.streams",
         schemaFileName = "streams.proto",
         schemaFilePath = "proto"
   )
   public interface StreamSchema extends GeneratedSchema {
      GeneratedSchema INSTANCE = new StreamSchemaImpl();
   }

   @ProtoName("StreamFactory")
   public static class Factory {
      public final String[] strings;

      @ProtoFactory
      public Factory(Stream<String> strings) {
         this.strings = strings.toArray(String[]::new);
      }

      @ProtoField(1)
      public Stream<String> getStrings() {
         return Stream.of(strings);
      }
   }

   @ProtoName("StreamGetterSetter")
   public static class GetterSetter {
      public String[] strings;

      public GetterSetter() {}

      public GetterSetter(String... strings) {
         this.strings = strings;
      }

      @ProtoField(1)
      public Stream<String> getStrings() {
         return Arrays.stream(strings);
      }

      public void setStrings(Stream<String> strings) {
         this.strings = strings.toArray(String[]::new);
      }
   }
}
