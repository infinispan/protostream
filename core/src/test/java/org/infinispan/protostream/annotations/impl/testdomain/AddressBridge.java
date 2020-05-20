package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.annotations.ProtoBridgeFor;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoReserved;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.protostream.domain.Address;

/**
 * @author anistor@redhat.com
 * @since 4.4
 */
@ProtoName("Address")
@ProtoBridgeFor(Address.class)
public class AddressBridge {

   //todo [anistor] when setters are present, allow factory with 0 args
   @ProtoFactory
   public Address create(String street, Integer number, String postCode) {
      return new Address(street, postCode, number);
   }

   @ProtoField(number = 1)
   public String getStreet(Address a) {
      return a.getStreet();
   }

   public void setStreet(Address a, String street) {
      a.setStreet(street);
   }

   @ProtoField(2)
   public String getPostCode(Address a) {
      return a.getPostCode();
   }

   public void setPostCode(Address a, String postCode) {
      a.setPostCode(postCode);
   }

   @ProtoField(number = 3, required = true)
   public Integer getNumber(Address a) {
      return a.getNumber();
   }

   public void setNumber(Address a, Integer number) {
      a.setNumber(number);
   }

   // A funny case. A bridge nested inside a bridge should work just fine!
   @ProtoName("Address2")
   @ProtoReserved(numbers = {6, 99})
   @ProtoTypeId(666)
   @ProtoBridgeFor(Address.class)
   public static class AddressBridge2 {

      @ProtoFactory
      public Address create(String street, Integer number, String postCode) {
         return new Address(street, postCode, number);
      }

      @ProtoField(number = 1, name = "str")
      public String getStreet(Address a) {
         return a.getStreet();
      }

      @ProtoField(2)
      public String getPostCode(Address a) {
         return a.getPostCode();
      }

      @ProtoField(number = 3, required = true)
      public Integer getNumber(Address a) {
         return a.getNumber();
      }
   }
}
