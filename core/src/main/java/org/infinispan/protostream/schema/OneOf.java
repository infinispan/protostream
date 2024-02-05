package org.infinispan.protostream.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @since 5.0
 */
public class OneOf {

   private final String name;
   private final String fullName;
   private final java.util.Map<String, Field> fields;
   private final List<String> comments;

   OneOf(Builder builder) {
      this.name = builder.name;
      this.fullName = builder.getFullName();
      this.fields = builder.fields.entrySet().stream().collect(Collectors.toUnmodifiableMap(java.util.Map.Entry::getKey, e -> e.getValue().create()));
      this.comments = List.copyOf(builder.comments);
   }

   public String getName() {
      return name;
   }

   public String getFullName() {
      return fullName;
   }

   public List<String> getComments() {
      return comments;
   }

   public java.util.Map<String, Field> getFields() {
      return fields;
   }

   public static class FieldBuilder implements OptionContainer<FieldBuilder>, CommentContainer<FieldBuilder> {
      private final Builder parent;
      private final Field.Builder builder;

      FieldBuilder(Builder parent, Type type, String name, int number) {
         this.parent = parent;
         this.builder = new Field.Builder(parent.message, type, name, number, false);
      }

      public FieldBuilder addOneOfField(Type type, String name, int number) {
         return parent.addOneOfField(type, name, number);
      }

      public Field create() {
         return builder.create();
      }

      @Override
      public FieldBuilder addComment(String comment) {
         builder.addComment(comment);
         return this;
      }

      @Override
      public Schema build() {
         throw new UnsupportedOperationException();
      }

      @Override
      public String getFullName() {
         return parent.getFullName();
      }

      @Override
      public FieldBuilder addOption(String name, Object value) {
         builder.addOption(name, value);
         return null;
      }
   }

   public static class Builder implements CommentContainer<Builder> {

      private final Message.Builder message;
      private final String name;
      private final java.util.Map<String, FieldBuilder> fields = new HashMap<>();
      private final List<String> comments = new ArrayList<>();

      Builder(Message.Builder message, String name) {
         this.message = message;
         this.name = name;
      }

      public FieldBuilder addOneOfField(Type type, String name, int number) {
         Objects.requireNonNull(type, "type must not be null");
         Objects.requireNonNull(name, "name must not be null");
         checkDuplicate(name);
         FieldBuilder field = new FieldBuilder(this, type, name, number);
         fields.put(name, field);
         return field;
      }

      @Override
      public Builder addComment(String comment) {
         Objects.requireNonNull(comment, "comment must not be null");
         comments.add(comment.trim());
         return this;
      }

      OneOf create() {
         return new OneOf(this);
      }

      public String getFullName() {
         return message.getFullName() + '.' + name;
      }

      private void checkDuplicate(String name) {
         if (fields.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate field name " + name);
         }
      }
   }
}
