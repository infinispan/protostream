package org.infinispan.protostream.types.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;

public class PrimitiveCollections {

   @ProtoField(number = 1)
   final List<String> listOf;

   @ProtoField(number = 2, collectionImplementation = ArrayList.class)
   final ArrayList<String> arrayList;

   @ProtoField(number = 3, collectionImplementation = HashSet.class)
   final HashSet<String> hashSet;

   @ProtoField(number = 4, collectionImplementation = LinkedHashSet.class)
   final LinkedHashSet<String> linkedHashSet;

   @ProtoField(number = 5, collectionImplementation = LinkedList.class)
   final LinkedList<String> linkedList;

   @ProtoField(number = 6, collectionImplementation = TreeSet.class)
   final TreeSet<String> treeSet;

   @ProtoField(number = 7, mapImplementation = HashMap.class)
   final Map<String, String> hashMap;

   @ProtoField(number = 8, collectionImplementation = ArrayList.class)
   final ArrayList<Book> books;

   final Map<String, ArrayList<Book>> booksByOwner;

   @ProtoFactory
   public PrimitiveCollections(List<String> listOf,
                               ArrayList<String> arrayList,
                               HashSet<String> hashSet,
                               LinkedHashSet<String> linkedHashSet,
                               LinkedList<String> linkedList,
                               TreeSet<String> treeSet,
                               Map<String, String> hashMap,
                               ArrayList<Book> books,
                               Map<String, ArrayList<Book>> booksByOwner) {
      this.listOf = listOf;
      this.arrayList = arrayList;
      this.hashSet = hashSet;
      this.linkedHashSet = linkedHashSet;
      this.linkedList = linkedList;
      this.treeSet = treeSet;
      this.hashMap = hashMap;
      this.books = books;
      this.booksByOwner = booksByOwner;
   }

   @ProtoField(number = 9, mapImplementation = HashMap.class)
   public Map<String, ArrayList> getBooksByOwner() {
      return (Map<String, ArrayList>) ((Object) booksByOwner);
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      PrimitiveCollections that = (PrimitiveCollections) o;
      return Objects.equals(listOf, that.listOf)
            && Objects.equals(arrayList, that.arrayList)
            && Objects.equals(hashSet, that.hashSet)
            && Objects.equals(linkedHashSet, that.linkedHashSet)
            && Objects.equals(linkedList, that.linkedList)
            && Objects.equals(treeSet, that.treeSet)
            && Objects.equals(hashMap, that.hashMap)
            && Objects.equals(books, that.books)
            && Objects.equals(booksByOwner, that.booksByOwner);
   }

   @Override
   public int hashCode() {
      return Objects.hash(listOf, arrayList, hashSet, linkedHashSet, linkedList, treeSet, hashMap, books, booksByOwner);
   }

   @Override
   public String toString() {
      return "PrimitiveCollections{" +
            "listOf=" + listOf +
            ", arrayList=" + arrayList +
            ", hashSet=" + hashSet +
            ", linkedHashSet=" + linkedHashSet +
            ", linkedList=" + linkedList +
            ", treeSet=" + treeSet +
            ", hashMap=" + hashMap +
            ", books=" + books +
            ", booksByOwner=" + booksByOwner +
            '}';
   }

   @ProtoSchema(
         syntax = ProtoSyntax.PROTO3,
         dependsOn = {
               BookSchema.class,
               CommonContainerTypes.class,
         },
         includeClasses = {
               PrimitiveCollections.class
         },
         schemaFileName = "primitive-collections.proto",
         schemaFilePath = "proto/",
         schemaPackageName = "collections"
   )
   public interface PrimitiveCollectionsSchema extends GeneratedSchema {
   }
}
