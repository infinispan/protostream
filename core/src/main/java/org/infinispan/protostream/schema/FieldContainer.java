package org.infinispan.protostream.schema;

import java.util.function.Consumer;

/**
 * @since 5.0
 */
public interface FieldContainer extends MessageContainer, GenericContainer {
   Field.Builder addField(Type type, String name, int number);

   Field.Builder addRepeatedField(Type type, String name, int number);

   Map.Builder addMap(Type.Scalar keyType, Type valueType, String name, int number);

   Message.Builder addOneOf(String name, Consumer<OneOf.Builder> oneof);
   
   Enum.Builder addEnum(String name);

   Message.Builder addNestedMessage(String name, Consumer<Message.Builder> nested);

   Message.Builder addNestedEnum(String name, Consumer<Enum.Builder> nested);
}
