package test_marshall_externals;

import org.infinispan.protostream.*;
import org.infinispan.protostream.annotations.*;

@AutoProtoSchemaBuilder(includeClasses = AddressBridge.class, schemaFilePath = "/")
interface TestInitializer extends SerializationContextInitializer {
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

@ProtoBridgeFor(Address.class)
class AddressBridge {

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
