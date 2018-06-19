package org.infinispan.protostream.impl;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.Type;
import org.junit.Test;

public class DynamicEntityTest {

   private SerializationContextImpl createContext() {
      return (SerializationContextImpl) ProtobufUtil.newSerializationContext(Configuration.builder().build());
   }

   // Inner class, represents the properties of the Dynamic Entity
   class Child<T> {
      String key;
      T property;

      Child(String key, T property) {
         this.key = key;
         this.property = property;
      }
   }

   // Dynamic entity
   class DynamicEntity {
      int id;
      String type;
      List<Child<?>> children;

      DynamicEntity(int id, String type, List<Child<?>> children) {
         this.id = id;
         this.type = type;
         this.children = children;
      }

      private String generateProto() {
         StringBuilder proto = new StringBuilder();
         AtomicInteger fieldNumber = new AtomicInteger();
         proto.append("message ").append(type).append(" {\n");
         proto.append("required int32 ").append("id").append("=").append(fieldNumber.incrementAndGet()).append(";\n");
         proto.append("required string ").append("type").append("=").append(fieldNumber.incrementAndGet()).append(";\n");
         children.forEach(c -> {
            String type = c.property instanceof Integer ? "int32" : "string";
            proto.append("required ").append(type).append(" ").append(c.key).append("=").append(fieldNumber.incrementAndGet()).append(";\n");
         });
         return proto.append(" } ").toString();
      }
   }

   // The Marshaller for the DynamicEntity
   class EntityMarshaller implements MessageMarshaller<DynamicEntity> {
      private String type;

      EntityMarshaller(String type) {
         this.type = type;
      }

      @Override
      public DynamicEntity readFrom(ProtoStreamReader reader) throws IOException {
         Descriptor descriptor = reader.getSerializationContext().getMessageDescriptor(this.getTypeName());
         int id = reader.readInt("id");
         String type = reader.readString("type");
         List<Child<?>> children = descriptor.getFields().stream()
               .filter(fd -> !fd.getName().equals("id") && !fd.getName().equals("type"))
               .map(field -> {
                  try {
                     String name = field.getName();
                     Object value;
                     if (field.getType() == Type.INT32) {
                        value = reader.readInt(name);
                     } else {
                        value = reader.readString(name);
                     }
                     return new Child<>(field.getName(), value);
                  } catch (Exception ignored) {
                     return null;
                  }
               })
               .collect(toList());

         return new DynamicEntity(id, type, children);
      }

      @Override
      public void writeTo(ProtoStreamWriter writer, DynamicEntity topLevel) throws IOException {
         writer.writeInt("id", topLevel.id);
         writer.writeString("type", topLevel.type);
         topLevel.children.forEach(c -> {
            try {
               String name = c.key;
               Object value = c.property;
               if (value instanceof Integer) {
                  writer.writeInt(name, (Integer) value);
               } else {
                  writer.writeString(name, value.toString());
               }
            } catch (Exception ignored) {
            }
         });

      }

      @Override
      public Class<? extends DynamicEntity> getJavaClass() {
         return DynamicEntity.class;
      }

      @Override
      public String getTypeName() {
         return type;
      }
   }

   class MarshallerProvider implements SerializationContext.DynamicMarshallerProvider {

      @Override
      public BaseMarshaller<?> getMarshaller(Object instance) {
         if (instance instanceof DynamicEntity) {
            String type = DynamicEntity.class.cast(instance).type;
            return new EntityMarshaller(type);
         }
         return null;
      }

      @Override
      public BaseMarshaller<?> getMarshaller(String typeName) {
         return new EntityMarshaller(typeName);
      }

   }

   @Test
   public void testMarshallerProviderDynamicTypes() throws IOException {
      // Create two dynamic types
      DynamicEntity dynamicEntity = new DynamicEntity(1, "type1",
            asList(new Child<>("eyes", "blue"), new Child<>("age", 23)));
      DynamicEntity otherDynamicEntity = new DynamicEntity(2, "type2",
            asList(new Child<>("country", "Jamaica"), new Child<>("currency", "jmd")));

      // Auto generate the proto file from the entities and register them
      SerializationContextImpl ctx = createContext();
      String protoFile1 = dynamicEntity.generateProto();
      String protoFile2 = otherDynamicEntity.generateProto();
      System.out.println(protoFile1);
      System.out.println(protoFile2);
      ctx.registerProtoFiles(new FileDescriptorSource()
            .addProtoFile(dynamicEntity.type + ".proto", protoFile1)
            .addProtoFile(otherDynamicEntity.type + ".proto", protoFile2));

      // Register a marshaller provider
      ctx.registerDynamicMarshallerProvider(new MarshallerProvider());

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, dynamicEntity);
      DynamicEntity input = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertEquals(1, input.id);
      assertEquals("type1", input.type);
      assertEquals(2, input.children.size());

      Child<?> first = input.children.get(0);
      assertEquals("eyes", first.key);
      assertEquals("blue", first.property);

      Child<?> second = input.children.get(1);
      assertEquals("age", second.key);
      assertEquals(23, second.property);

      byte[] anotherBytes = ProtobufUtil.toWrappedByteArray(ctx, otherDynamicEntity);
      DynamicEntity anotherInput = ProtobufUtil.fromWrappedByteArray(ctx, anotherBytes);

      assertEquals(2, anotherInput.id);
      assertEquals("type2", anotherInput.type);
      assertEquals(2, anotherInput.children.size());

      Child<?> anotherFirst = anotherInput.children.get(0);
      assertEquals("country", anotherFirst.key);
      assertEquals("Jamaica", anotherFirst.property);

      Child<?> anotherSecond = anotherInput.children.get(1);
      assertEquals("currency", anotherSecond.key);
      assertEquals("jmd", anotherSecond.property);
   }
}
