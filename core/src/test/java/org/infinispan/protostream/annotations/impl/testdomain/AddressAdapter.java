package org.infinispan.protostream.annotations.impl.testdomain;

import org.infinispan.protostream.annotations.ProtoAdapter;
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
@ProtoAdapter(Address.class)
public class AddressAdapter {

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

   public static final class Address2 {

      public final String street;
      public final String postCode;
      public final int number;
      public final Character houseLetter;

      public Address2(String street, int number, String postCode, Character houseLetter) {
         this.street = street;
         this.number = number;
         this.postCode = postCode;
         this.houseLetter = houseLetter;
      }
   }

   // A funny case. An adapter nested inside another adapter should work just fine!
   @ProtoName("Address2")
   @ProtoReserved(numbers = {6, 99})
   @ProtoTypeId(666)
   @ProtoAdapter(Address2.class)
   public static class AddressAdapter2 {

      @ProtoFactory
      public Address2 create(String street, int number, String postCode, Character houseLetter) {
         return new Address2(street, number, postCode, houseLetter);
      }

      @ProtoField(number = 1, name = "str")
      public String getStreet(Address2 a) {
         return a.street;
      }

      @ProtoField(2)
      public String getPostCode(Address2 a) {
         return a.postCode;
      }

      @ProtoField(number = 3, required = true)
      public int getNumber(Address2 a) {
         return a.number;
      }

      @ProtoField(number = 4, required = true)
      public Character getHouseLetter(Address2 a) {
         return a.houseLetter;
      }
   }
}
