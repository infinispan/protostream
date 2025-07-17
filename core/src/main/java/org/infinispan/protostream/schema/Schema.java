package org.infinispan.protostream.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Interface that represents a Schema
 * Refers to programmatic schemas {@link Schema.Builder} and {@link org.infinispan.protostream.GeneratedSchema}
 */
public interface Schema {

    /**
     * Gets the name of the schema
     * @return the schema name
     */
    String getName();

    /**
     * Gets the content of the schema
     * @return the content
     */
    String getContent();

    /**
     * Gets the content of the schema
     *
     * Use {@link Schema#getContent()}
     * @deprecated
     */
    @Deprecated
    String toString();


    static Schema buildFromStringContent(String schemaName, String schemaContent) {
        return new SchemaByString(schemaName, schemaContent);
    }

    class Builder implements CommentContainer<Builder>, MessageContainer, OptionContainer<Builder>, EnumContainer {
        Syntax syntax = Syntax.PROTO3;
        final String name;
        String packageName;
        final java.util.Map<String, Enum.Builder> enums = new HashMap<>();
        final java.util.Map<String, Message.Builder> messages = new HashMap<>();
        final Map<String, Object> options = new HashMap<>();
        final List<String> comments = new ArrayList<>();
        final List<String> dependencies = new ArrayList<>();
        final List<String> publicDependencies = new ArrayList<>();

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
            return new SchemaByBuilder(this);
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
