package test_marshall_externals;

import java.util.UUID;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoReserved;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.protostream.descriptors.Type;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@AutoProtoSchemaBuilder(includeClasses = {
      AddressAdapter.class,
      UUIDAdapter.class,
      ColorEnumAdapter.class
   },
   schemaFilePath = "/"
)
interface TestInitializer extends SerializationContextInitializer {
}

enum Color {
   RED, GREEN, BLUE
}

@ProtoAdapter(Color.class)
@ProtoReserved({100, 99})
enum ColorEnumAdapter {

   @ProtoEnumValue(number = 0, name = "red")
   RED,

   @ProtoEnumValue(number = 1, name = "green")
   GREEN,

   @ProtoEnumValue(number = 2, name = "blue")
   BLUE

//TODO [anistor] this generates a compilation error too late
//   @ProtoEnumValue(number = 3, name = "black")
//   BLACK
}

@ProtoAdapter(UUID.class)
class UUIDAdapter {

   @ProtoFactory
   UUID create(long mostSigBits, long leastSigBits) {
      return new UUID(mostSigBits, leastSigBits);
   }

   @ProtoField(number = 1, type = Type.UINT64, defaultValue = "0")
   long getMostSigBits(UUID uuid) {
      return uuid.getMostSignificantBits();
   }

   @ProtoField(number = 2, type = Type.UINT64, defaultValue = "0")
   long getLeastSigBits(UUID uuid) {
      return uuid.getLeastSignificantBits();
   }
}

class Address {

   private String street_;

   private String postCode_;

   private int number_;

   public Address(String street_, String postCode_, int number_) {
      this.street_ = street_;
      this.postCode_ = postCode_;
      this.number_ = number_;
   }

   public String getStreet_() {
      return street_;
   }

   public void setStreet_(String street_) {
      this.street_ = street_;
   }

   public String getPostCode_() {
      return postCode_;
   }

   public void setPostCode_(String postCode_) {
      this.postCode_ = postCode_;
   }

   public int getNumber_() {
      return number_;
   }

   public void setNumber_(int number_) {
      this.number_ = number_;
   }
}

@ProtoAdapter(Address.class)
class AddressAdapter {

   @ProtoFactory
   public Address create(String street, Integer number, String postCode) {
      return new Address(street, postCode, number);
   }

   @ProtoField(1)
   public String getStreet(Address a) {
      return a.getStreet_();
   }

   public void setStreet(Address a, String street) {
      a.setStreet_(street);
   }

   @ProtoField(2)
   public String getPostCode(Address a) {
      return a.getPostCode_();
   }

   public void setPostCode(Address a, String postCode) {
      a.setPostCode_(postCode);
   }

   @ProtoField(number = 3, required = true)
   public Integer getNumber(Address a) {
      return a.getNumber_();
   }

   public void setNumber(Address a, Integer number) {
      a.setNumber_(number);
   }
}
