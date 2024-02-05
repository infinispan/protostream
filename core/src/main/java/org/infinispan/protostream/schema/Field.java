package org.infinispan.protostream.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @since 5.0
 */
public class Field  {
   protected final Type type;
   protected final String name;
   protected final String fullName;
   protected final int number;
   protected final boolean repeated;
   protected final List<String> comments;
   protected final java.util.Map<String, Object> options;

   Field(Builder builder) {
      this.type = builder.type;
      this.name = builder.name;
      this.fullName = builder.getFullName();
      this.number = builder.number;
      this.repeated = builder.repeated;
      this.options = java.util.Map.copyOf(builder.options);
      this.comments = List.copyOf(builder.comments);
   }

   public Type getType() {
      return type;
   }

   public String getName() {
      return name;
   }

   public String getFullName() {
      return fullName;
   }

   public int getNumber() {
      return number;
   }

   public boolean isRepeated() {
      return repeated;
   }

   public List<String> getComments() {
      return comments;
   }

   public java.util.Map<String, Object> getOptions() {
      return options;
   }

   public static class Builder implements OptionContainer<Field.Builder>, CommentContainer<Field.Builder>, FieldContainer {
      protected final FieldContainer parent;
      protected final Type type;
      protected final String name;
      protected final int number;
      protected final boolean repeated;
      protected final List<String> comments = new ArrayList<>();
      protected final java.util.Map<String, Object> options = new HashMap<>();

      Builder(FieldContainer parent, Type type, String name, int number, boolean repeated) {
         this.parent = parent;
         this.type = type;
         this.name = name;
         this.number = number;
         this.repeated = repeated;
      }

      public Message.Builder addMessage(String name) {
         return parent.addMessage(name);
      }

      @Override
      public Field.Builder addField(Type type, String name, int number) {
         return parent.addField(type, name, number);
      }

      @Override
      public Map.Builder addMap(Type.Scalar keyType, Type valueType, String name, int number) {
         return parent.addMap(keyType, valueType, name, number);
      }

      @Override
      public Field.Builder addRepeatedField(Type type, String name, int number) {
         return parent.addRepeatedField(type, name, number);
      }

      @Override
      public Message.Builder addNestedMessage(String name, Consumer<Message.Builder> nested) {
         return parent.addNestedMessage(name, nested);
      }

      @Override
      public Message.Builder addNestedEnum(String name, Consumer<Enum.Builder> nested) {
         return parent.addNestedEnum(name, nested);
      }

      @Override
      public Enum.Builder addEnum(String name) {
         return parent.addEnum(name);
      }

      @Override
      public Message.Builder addOneOf(String name, Consumer<OneOf.Builder> oneof) {
         return parent.addOneOf(name, oneof);
      }

      @Override
      public Field.Builder addOption(String name, Object value) {
         Objects.requireNonNull(name);
         Objects.requireNonNull(value);
         options.put(name, value);
         return this;
      }

      @Override
      public Field.Builder addComment(String comment) {
         Objects.requireNonNull(comment);
         comments.add(comment.trim());
         return this;
      }

      protected Field create() {
         return new Field(this);
      }

      @Override
      public Schema build() {
         return parent.build();
      }

      @Override
      public String getFullName() {
         return parent.getFullName() + '.' + name;
      }
   }
}
