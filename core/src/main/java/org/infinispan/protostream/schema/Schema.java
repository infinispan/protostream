package org.infinispan.protostream.schema;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @since 5.0
 */
public class Schema {
   private final Syntax syntax;
   private final String name;
   private final String packageName;
   private final List<Enum> enums;
   private final List<Message> messages;
   private final Map<String, Object> options;
   private final List<String> comments;
   private final List<String> dependencies;
   private final List<String> publicDependencies;

   private Schema(Builder builder) {
      this.syntax = builder.syntax;
      this.dependencies = List.copyOf(builder.dependencies);
      this.publicDependencies = List.copyOf(builder.publicDependencies);
      this.name = builder.name;
      this.packageName = builder.packageName;
      this.options = Map.copyOf(builder.options);
      this.enums = builder.enums.values().stream().map(Enum.Builder::create).toList();
      this.messages = builder.messages.values().stream().map(Message.Builder::create).toList();
      this.comments = List.copyOf(builder.comments);
   }

   public Syntax getSyntax() {
      return syntax;
   }

   public String getName() {
      return name;
   }

   public String getPackageName() {
      return packageName;
   }

   public List<Enum> getEnums() {
      return enums;
   }

   public List<Message> getMessages() {
      return messages;
   }

   public List<String> getComments() {
      return comments;
   }

   public List<String> getDependencies() {
      return dependencies;
   }

   public List<String> getPublicDependencies() {
      return publicDependencies;
   }

   public Map<String, Object> getOptions() {
      return options;
   }

   public String toString() {
      try {
         StringWriter w = new StringWriter();
         new SchemaWriter().write(w, this);
         return w.toString();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public static class Builder implements CommentContainer<Builder>, MessageContainer, OptionContainer<Builder>, EnumContainer {
      private Syntax syntax = Syntax.PROTO3;
      private final String name;
      private String packageName;
      private final Map<String, Enum.Builder> enums = new HashMap<>();
      private final Map<String, Message.Builder> messages = new HashMap<>();
      private final Map<String, Object> options = new HashMap<>();
      private final List<String> comments = new ArrayList<>();
      private final List<String> dependencies = new ArrayList<>();
      private final List<String> publicDependencies = new ArrayList<>();

      public Builder(String name) {
         this.name = name;
      }

      public Builder syntax(Syntax syntax) {
         this.syntax = syntax;
         return this;
      }

      public Builder addImport(String i) {
         dependencies.add(i);
         return this;
      }

      public Builder addPublicImport(String i) {
         publicDependencies.add(i);
         return this;
      }

      public Builder packageName(String packageName) {
         this.packageName = packageName;
         return this;
      }

      @Override
      public Builder addOption(String name, Object value) {
         options.put(name, value);
         return this;
      }

      @Override
      public Enum.Builder addEnum(String name) {
         checkDuplicate(name);
         Enum.Builder e = new Enum.Builder(this, name);
         enums.put(name, e);
         return e;
      }

      @Override
      public Message.Builder addMessage(String name) {
         checkDuplicate(name);
         Message.Builder message = new Message.Builder(this, name);
         messages.put(name, message);
         return message;
      }

      @Override
      public Builder addComment(String comment) {
         comments.add(comment.trim());
         return this;
      }

      @Override
      public Schema build() {
         return new Schema(this);
      }

      @Override
      public String getFullName() {
         return Objects.requireNonNullElse(packageName, "");
      }

      private void checkDuplicate(String name) {
         if (messages.containsKey(name) || enums.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate name " + name);
         }
      }
   }
}
