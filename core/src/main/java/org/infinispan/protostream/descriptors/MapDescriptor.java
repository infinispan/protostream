package org.infinispan.protostream.descriptors;

public class MapDescriptor extends FieldDescriptor {
   private final String keyTypeName;
   private final Type keyType;
   private final Descriptor descriptor;

   private MapDescriptor(Builder builder) {
      super(builder);
      keyTypeName = builder.keyTypeName;
      keyType = Type.primitiveFromString(keyTypeName);
      Descriptor.Builder b = new Descriptor.Builder().withName(name).withFullName(fullName);
      FieldDescriptor.Builder kb = new FieldDescriptor.Builder().withNumber(1).withName("key").withTypeName(keyTypeName);
      b.addField(kb);
      FieldDescriptor.Builder vb = new FieldDescriptor.Builder().withNumber(2).withName("value").withTypeName(typeName);
      b.addField(vb);
      descriptor = b.build();
   }

   public Descriptor asDescriptor() {
      return descriptor;
   }

   @Override
   public boolean isRepeated() {
      return true;
   }

   @Override
   public boolean isMap() {
      return true;
   }

   public Type getKeyType() {
      return keyType;
   }

   public String getKeyTypeName() {
      return keyTypeName;
   }

   public JavaType getKeyJavaType() {
      return getKeyType().getJavaType();
   }

   public int getWireTag() {
      return WireType.makeTag(number, WireType.LENGTH_DELIMITED);
   }

   public int getKeyWireTag() {
      return WireType.makeTag(1, keyType.getWireType());
   }

   public int getValueWireTag() {
      return WireType.makeTag(2, type.getWireType());
   }

   @Override
   public Label getLabel() {
      return Label.OPTIONAL;
   }

   @Override
   void setMessageType(Descriptor descriptor) {
      super.setMessageType(descriptor);
      this.descriptor.getFields().get(1).setMessageType(descriptor);
   }

   @Override
   public String toString() {
      return "MapDescriptor{" +
            "keyTypeName='" + keyTypeName + '\'' +
            ", valueTypeName='" + typeName + '\'' +
            ", name='" + name + '\'' +
            ", number='" + number + '\'' +
            '}';
   }


   public static class Builder extends FieldDescriptor.Builder {
      String keyTypeName;

      @Override
      public Builder withName(String name) {
         super.withName(name);
         return this;
      }

      public Builder withKeyTypeName(String keyTypeName) {
         this.keyTypeName = keyTypeName;
         return this;
      }

      public Builder withValueTypeName(String valueTypeName) {
         this.withTypeName(valueTypeName);
         return this;
      }

      @Override
      public MapDescriptor build() {
         return new MapDescriptor(this);
      }
   }
}
