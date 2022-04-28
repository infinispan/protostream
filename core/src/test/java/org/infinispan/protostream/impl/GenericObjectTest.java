package org.infinispan.protostream.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Type;
import org.junit.Test;

/**
 * Test marshalling of a generic Java type mapped to a fixed set of protobuf types.
 */
public class GenericObjectTest {

   static final class GenericObjectMarshaller implements MessageMarshaller<GenericObject> {

      private final String typeName;

      GenericObjectMarshaller(String typeName) {
         this.typeName = typeName;
      }

      @Override
      public GenericObject readFrom(ProtoStreamReader reader) throws IOException {
         Descriptor descriptor = reader.getSerializationContext().getMessageDescriptor(getTypeName());
         String type = reader.readString("typeName");
         List<GenericObject.ObjectProperty<?>> props = new ArrayList<>();
         for (FieldDescriptor fd : descriptor.getFields()) {
            if (!fd.getName().equals("typeName")) {
               Object value = fd.getType() == Type.INT32 ? reader.readInt(fd.getName()) : reader.readString(fd.getName());
               props.add(new GenericObject.ObjectProperty<>(fd.getName(), value));
            }
         }
         return new GenericObject(type, props);
      }

      @Override
      public void writeTo(ProtoStreamWriter writer, GenericObject dynamicEntity) throws IOException {
         writer.writeString("typeName", dynamicEntity.typeName);
         for (GenericObject.ObjectProperty<?> c : dynamicEntity.props) {
            if (c.value instanceof Integer) {
               writer.writeInt(c.name, (Integer) c.value);
            } else {
               writer.writeString(c.name, c.value.toString());
            }
         }
      }

      @Override
      public Class<GenericObject> getJavaClass() {
         return GenericObject.class;
      }

      @Override
      public String getTypeName() {
         return typeName;
      }
   }

   static class GenericObjectMarshallerProvider implements SerializationContext.InstanceMarshallerProvider<GenericObject> {

      private final Set<String> typeNames;

      public GenericObjectMarshallerProvider(String... typeNames) {
         this.typeNames = new HashSet<>(Arrays.asList(typeNames));
      }

      @Override
      public Set<String> getTypeNames() {
         return typeNames;
      }

      @Override
      public String getTypeName(GenericObject instance) {
         return instance.typeName;
      }

      @Override
      public Class<GenericObject> getJavaClass() {
         return GenericObject.class;
      }

      @Override
      public BaseMarshaller<GenericObject> getMarshaller(GenericObject instance) {
         return new GenericObjectMarshaller(instance.typeName);
      }

      @Override
      public BaseMarshaller<GenericObject> getMarshaller(String typeName) {
         return new GenericObjectMarshaller(typeName);
      }
   }

   @Test
   public void testMarshallerProviderDynamicTypes() throws Exception {
      GenericObject obj1 = new GenericObject("type1")
            .setProperty("eyes", "blue")
            .setProperty("age", 23);

      GenericObject obj2 = new GenericObject("type2")
            .setProperty("country", "Jamaica")
            .setProperty("currency", "jmd");

      // generate the message definitions for the entities and register them
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      String protoFile1 = obj1.toSchema();
      String protoFile2 = obj2.toSchema();
      ctx.registerProtoFiles(FileDescriptorSource.fromString("testFlatDynamicEntity.proto", protoFile1 + protoFile2));

      // Register a marshaller provider
      ctx.registerMarshallerProvider(new GenericObjectMarshallerProvider("type1", "type2"));

      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, obj1);
      GenericObject input = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

      assertEquals("type1", input.typeName);
      assertEquals(2, input.props.size());

      assertEquals("eyes", input.props.get(0).name);
      assertEquals("blue", input.props.get(0).value);

      assertEquals("age", input.props.get(1).name);
      assertEquals(23, input.props.get(1).value);

      byte[] anotherBytes = ProtobufUtil.toWrappedByteArray(ctx, obj2);
      GenericObject anotherInput = ProtobufUtil.fromWrappedByteArray(ctx, anotherBytes);

      assertEquals("type2", anotherInput.typeName);
      assertEquals(2, anotherInput.props.size());

      assertEquals("country", anotherInput.props.get(0).name);
      assertEquals("Jamaica", anotherInput.props.get(0).value);

      assertEquals("currency", anotherInput.props.get(1).name);
      assertEquals("jmd", anotherInput.props.get(1).value);
   }
}

final class GenericObject {

   // The actual protobuf type name
   String typeName;

   List<ObjectProperty<?>> props;

   GenericObject(String type) {
      this(type, new ArrayList<>());
   }

   GenericObject(String type, List<ObjectProperty<?>> props) {
      this.typeName = type;
      this.props = props;
   }

   GenericObject setProperty(String name, Object value) {
      props.add(new ObjectProperty<>(name, value));
      return this;
   }

   String toSchema() {
      StringBuilder sb = new StringBuilder();
      sb.append("message ").append(typeName).append(" {\n");
      sb.append("\trequired string typeName = 1;\n");
      // the child properties are actual fields in this message instead of being nested messages
      int n = 2;
      for (ObjectProperty<?> p : props) {
         // only int32 and string supported; enough for demonstration purposes
         sb.append("\toptional ").append(p.value instanceof Integer ? "int32" : "string")
               .append(' ').append(p.name).append('=').append(n++).append(";\n");
      }
      return sb.append("}\n\n").toString();
   }

   final static class ObjectProperty<T> {

      final String name;

      final T value;

      ObjectProperty(String name, T value) {
         if (name == null || value == null) {
            throw new IllegalArgumentException("key and value must both not be null");
         }
         if (!(value instanceof Integer || value instanceof String)) {
            throw new IllegalArgumentException("value must be java.lang.Integer or java.lang.String");
         }

         this.name = name;
         this.value = value;
      }
   }
}
