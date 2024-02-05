package org.infinispan.protostream.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @since 5.0
 */
public class EnumValue {
   private final String name;
   private final String fullName;
   private final int number;
   private final List<String> comments;
   private final Map<String, Object> options;


   public EnumValue(Builder builder) {
      this.name = builder.name;
      this.fullName = builder.getFullName();
      this.number = builder.number;
      this.comments = List.copyOf(builder.comments);
      this.options = Map.copyOf(builder.options);
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

   public List<String> getComments() {
      return comments;
   }

   public Map<String, Object> getOptions() {
      return options;
   }

   public static class Builder implements OptionContainer<Builder>, CommentContainer<Builder>, ReservedContainer<Enum.Builder> {
      private final String name;
      private final int number;
      private final Enum.Builder parent;
      private final List<String> comments = new ArrayList<>();
      private final Map<String, Object> options = new HashMap<>();

      Builder(Enum.Builder parent, String name, int number) {
         this.parent = parent;
         this.name = name;
         this.number = number;
      }

      EnumValue create() {
         return new EnumValue(this);
      }

      public Schema build() {
         return parent.build();
      }

      @Override
      public String getFullName() {
         return parent.getFullName() + '.' + name;
      }

      @Override
      public Builder addOption(String name, Object value) {
         options.put(name, value);
         return this;
      }

      public EnumValue.Builder addValue(String name, int number) {
         return parent.addValue(name, number);
      }

      public Message.Builder addMessage(String name) {
         return parent.addMessage(name);
      }

      @Override
      public Builder addComment(String comment) {
         Objects.requireNonNull(comment, "comment must not be null");
         comments.add(comment.trim());
         return this;
      }

      @Override
      public Enum.Builder addReserved(int... numbers) {
         return parent.addReserved(numbers);
      }

      @Override
      public Enum.Builder addReservedRange(int from, int to) {
         return parent.addReservedRange(from, to);
      }

      @Override
      public Enum.Builder addReserved(String name) {
         return parent.addReserved(name);
      }
   }
}
