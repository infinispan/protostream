package org.infinispan.protostream.schema;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * @since 5.0
 */
class SchemaByBuilder implements Schema {
   private final Syntax syntax;
   private final String name;
   private final String packageName;
   private final List<Enum> enums;
   private final List<Message> messages;
   private final Map<String, Object> options;
   private final List<String> comments;
   private final List<String> dependencies;
   private final List<String> publicDependencies;

   SchemaByBuilder(Builder builder) {
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

   @Override
   public String getContent() {
      return toString();
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

   @Override
   public String toString() {
      try {
         StringWriter w = new StringWriter();
         new SchemaWriter().write(w, this);
         return w.toString();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
