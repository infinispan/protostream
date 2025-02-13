package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoSchema;

public class IterableModel {

   private IterableModel() {}

   @ProtoSchema(
         includeClasses = {
               CustomCollectionFactory.class,
               DefaultCollectionFactory.class,
               GetterSetter.class,
               IterableFactory.class
         },
         schemaPackageName = "org.protostream.iterables",
         schemaFileName = "iterables.proto",
         schemaFilePath = "proto"
   )
   public interface Schema extends GeneratedSchema {
      GeneratedSchema INSTANCE = new SchemaImpl();
   }

   public static class CustomCollectionFactory {
      public final List<String> strings;

      @ProtoFactory
      public CustomCollectionFactory(LinkedList<String> strings) {
         this.strings = strings;
      }

      @ProtoField(value = 1, collectionImplementation = LinkedList.class)
      Iterable<String> getStrings() {
         return strings;
      }
   }

   public static class DefaultCollectionFactory {
      public final List<String> strings;

      public DefaultCollectionFactory(List<String> strings) {
         this.strings = strings;
      }

      @ProtoFactory
      // Constructor arg can be default collection implementation ArrayList or Iterable
      public DefaultCollectionFactory(ArrayList<String> strings) {
         this.strings = strings;
      }

      @ProtoField(1)
      Iterable<String> getStrings() {
         return strings;
      }
   }

   public static class GetterSetter {
      public Iterable<String> strings;

      @ProtoField(1)
      public Iterable<String> getStrings() {
         return strings;
      }

      public void setStrings(Iterable<String> strings) {
         this.strings = strings;
      }
   }

   public static class IterableFactory {
      public final List<String> strings;

      @ProtoFactory
      public IterableFactory(Iterable<String> strings) {
         this.strings = new ArrayList<>();
         strings.forEach(this.strings::add);
      }

      @ProtoField(1)
      Iterable<String> getStrings() {
         return strings;
      }
   }
}
