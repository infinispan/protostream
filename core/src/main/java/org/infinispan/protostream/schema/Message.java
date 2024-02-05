package org.infinispan.protostream.schema;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @since 5.0
 */
public class Message {
   private final String name;
   private final String fullName;
   private final java.util.Map<String, Enum> nestedEnums;
   private final java.util.Map<String, Message> nestedMessages;
   private final java.util.Map<String, Field> fields;
   private final List<OneOf> oneOfs;
   private final BitSet reservedNumbers;
   private final Set<String> reservedNames;
   private final java.util.Map<String, Object> options;
   private final List<String> comments;

   Message(Builder builder) {
      this.name = builder.name;
      this.fullName = builder.getFullName();
      this.nestedEnums = builder.nestedEnums.entrySet().stream().collect(Collectors.toUnmodifiableMap(java.util.Map.Entry::getKey, e -> e.getValue().create()));
      this.nestedMessages = builder.nestedMessages.entrySet().stream().collect(Collectors.toUnmodifiableMap(java.util.Map.Entry::getKey, e -> e.getValue().create()));
      this.fields = builder.fields.entrySet().stream().collect(Collectors.toUnmodifiableMap(java.util.Map.Entry::getKey, e -> e.getValue().create()));
      this.oneOfs = builder.oneOfs.values().stream().map(OneOf.Builder::create).toList();
      this.reservedNumbers = builder.reservedNumbers;
      this.reservedNames = Set.copyOf(builder.reservedNames);
      this.options = java.util.Map.copyOf(builder.options);
      this.comments = List.copyOf(builder.comments);
   }

   public String getName() {
      return name;
   }

   public String getFullName() {
      return fullName;
   }

   public java.util.Map<String, Enum> getNestedEnums() {
      return nestedEnums;
   }

   public java.util.Map<String, Message> getNestedMessages() {
      return nestedMessages;
   }

   public java.util.Map<String, Field> getFields() {
      return fields;
   }

   public List<OneOf> getOneOfs() {
      return oneOfs;
   }

   public BitSet getReservedNumbers() {
      return reservedNumbers;
   }

   public Set<String> getReservedNames() {
      return reservedNames;
   }

   public java.util.Map<String, Object> getOptions() {
      return options;
   }

   public List<String> getComments() {
      return comments;
   }

   public static class Builder implements CommentContainer<Builder>, MessageContainer, FieldContainer, OptionContainer<Builder>, ReservedContainer<Builder>, EnumContainer {
      private final Schema.Builder schema;
      private final String name;
      private final java.util.Map<String, Enum.Builder> nestedEnums = new HashMap<>();
      private final java.util.Map<String, Message.Builder> nestedMessages = new HashMap<>();
      private final java.util.Map<String, Field.Builder> fields = new HashMap<>();
      private final java.util.Map<String, OneOf.Builder> oneOfs = new HashMap<>();
      private final BitSet reservedNumbers = new BitSet();
      private final Set<String> reservedNames = new HashSet<>();
      private final java.util.Map<String, Object> options = new HashMap<>();
      private final List<String> comments = new ArrayList<>();
      private final GenericContainer parent;

      Builder(Schema.Builder schema, String name) {
         this.schema = schema;
         this.name = name;
         this.parent = schema;
      }

      Builder(Builder builder, String name) {
         this.schema = builder.schema;
         this.name = name;
         this.parent = builder;
      }

      @Override
      public Message.Builder addMessage(String name) {
         Objects.requireNonNull(name, "name must not be null");
         return schema.addMessage(name);
      }

      @Override
      public Builder addNestedEnum(String name, Consumer<Enum.Builder> nested) {
         Objects.requireNonNull(name, "name must not be null");
         Objects.requireNonNull(nested, "nested must not be null");
         Enum.Builder e = new Enum.Builder(this, name);
         nestedEnums.put(name, e);
         nested.accept(e);
         return this;
      }

      @Override
      public Builder addNestedMessage(String name, Consumer<Message.Builder> nested) {
         Objects.requireNonNull(name, "name must not be null");
         Objects.requireNonNull(nested, "nested must not be null");
         Message.Builder message = new Message.Builder(this, name);
         nestedMessages.put(name, message);
         nested.accept(message);
         return this;
      }

      @Override
      public Builder addOption(String name, Object value) {
         Objects.requireNonNull(name, "name must not be null");
         Objects.requireNonNull(value, "value must not be null");
         options.put(name, value);
         return this;
      }

      @Override
      public Message.Builder addComment(String comment) {
         Objects.requireNonNull(comment, "comment must not be null");
         comments.add(comment.trim());
         return this;
      }

      @Override
      public Field.Builder addField(Type type, String name, int number) {
         Objects.requireNonNull(type, "type must not be null");
         Objects.requireNonNull(name, "name must not be null");
         checkDuplicate(name);
         Field.Builder field = new Field.Builder(this, type, name, number, false);
         fields.put(name, field);
         return field;
      }

      @Override
      public Field.Builder addRepeatedField(Type type, String name, int number) {
         Objects.requireNonNull(type, "type must not be null");
         Objects.requireNonNull(name, "name must not be null");
         checkDuplicate(name);
         Field.Builder field = new Field.Builder(this, type, name, number, true);
         fields.put(name, field);
         return field;
      }

      @Override
      public Map.Builder addMap(Type.Scalar keyType, Type valueType, String name, int number) {
         Objects.requireNonNull(keyType, "keyType must not be null");
         Objects.requireNonNull(valueType, "valueType must not be null");
         Objects.requireNonNull(name, "name must not be null");
         checkDuplicate(name);
         Map.Builder map = new Map.Builder(this, keyType, valueType, name, number);
         fields.put(name, map);
         return map;
      }

      @Override
      public Builder addOneOf(String name, Consumer<OneOf.Builder> oneof) {
         Objects.requireNonNull(name, "name must not be null");
         Objects.requireNonNull(oneof, "oneof must not be null");
         checkDuplicate(name);
         OneOf.Builder builder = new OneOf.Builder(this, name);
         oneOfs.put(name, builder);
         oneof.accept(builder);
         return this;
      }

      @Override
      public Builder addReserved(int... numbers) {
         Objects.requireNonNull(numbers, "number array must not be null");
         for (int number : numbers) {
            reservedNumbers.set(number);
         }
         return this;
      }

      @Override
      public Builder addReservedRange(int from, int to) {
         reservedNumbers.set(from, to + 1);
         return this;
      }

      @Override
      public Builder addReserved(String name) {
         Objects.requireNonNull(name, "name must not be null");
         reservedNames.add(name);
         return this;
      }

      @Override
      public Enum.Builder addEnum(String name) {
         return schema.addEnum(name);
      }

      Message create() {
         return new Message(this);
      }

      @Override
      public Schema build() {
         return schema.build();
      }

      @Override
      public String getFullName() {
         return parent.getFullName() + '.' + name;
      }


      private void checkDuplicate(String name) {
         if (fields.containsKey(name) || oneOfs.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate field name " + name);
         }
      }
   }
}
