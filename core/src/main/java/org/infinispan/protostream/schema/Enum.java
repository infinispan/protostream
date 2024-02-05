package org.infinispan.protostream.schema;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @since 5.0
 */
public class Enum {
   private final String name;
   private final String fullName;
   private final Map<String, Object> options;
   private final List<String> comments;
   private final Map<String, EnumValue> values;
   private final BitSet reservedNumbers;
   private final Set<String> reservedNames;

   private Enum(Builder builder) {
      this.name = builder.name;
      this.fullName = builder.getFullName();
      this.values = builder.values.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().create()));
      this.reservedNumbers = builder.reservedNumbers;
      this.reservedNames = Set.copyOf(builder.reservedNames);
      this.options = Map.copyOf(builder.options);
      this.comments = List.copyOf(builder.comments);
   }

   public String getName() {
      return name;
   }

   public String getFullName() {
      return fullName;
   }

   public Map<String, EnumValue> getValues() {
      return values;
   }

   public Map<String, Object> getOptions() {
      return options;
   }

   public List<String> getComments() {
      return comments;
   }

   public BitSet getReservedNumbers() {
      return reservedNumbers;
   }

   public Set<String> getReservedNames() {
      return reservedNames;
   }

   public static class Builder implements CommentContainer<Builder>, ReservedContainer<Builder>, OptionContainer<Builder> {
      private final EnumContainer parent;
      private final String name;
      private final Map<String, EnumValue.Builder> values = new HashMap<>();
      private final BitSet reservedNumbers = new BitSet();
      private final Set<String> reservedNames = new HashSet<>();
      private final Map<String, Object> options = new HashMap<>();
      private final List<String> comments = new ArrayList<>();

      Builder(EnumContainer parent, String name) {
         this.parent = parent;
         this.name = name;
      }

      public EnumValue.Builder addValue(String name, int number) {
         Objects.requireNonNull(name, "name must not be null");
         checkDuplicate(name);
         EnumValue.Builder value = new EnumValue.Builder(this, name, number);
         values.put(name, value);
         return value;
      }

      private void checkDuplicate(String name) {
         if (values.containsKey(name) || reservedNames.contains(name)) {
            throw new IllegalArgumentException("Duplicate name " + name);
         }
      }

      @Override
      public Builder addReserved(int... numbers) {
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
      public Builder addOption(String name, Object value) {
         Objects.requireNonNull(name, "name must not be null");
         Objects.requireNonNull(value, "value must not be null");
         options.put(name, value);
         return this;
      }

      @Override
      public Builder addComment(String comment) {
         Objects.requireNonNull(comment, "comment must not be null");
         comments.add(comment.trim());
         return this;
      }

      public Message.Builder addMessage(String name) {
         return parent.addMessage(name);
      }

      public Enum.Builder addEnum(String name) {
         return parent.addEnum(name);
      }

      Enum create() {
         return new Enum(this);
      }

      public Schema build() {
         return parent.build();
      }

      @Override
      public String getFullName() {
         return parent.getFullName() + '.' + name;
      }
   }
}
