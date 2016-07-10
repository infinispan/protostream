package org.infinispan.protostream.domain;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.infinispan.protostream.BaseMessage;

/**
 * @author anistor@redhat.com
 */
public class Address extends BaseMessage implements Externalizable {  // implement Externalizable just for PerformanceTest

   private String street;
   private String postCode;
   private int number;

   public Address() {
   }

   public Address(String street, String postCode, int number) {
      this.street = street;
      this.postCode = postCode;
      this.number = number;
   }

   public String getStreet() {
      return street;
   }

   public void setStreet(String street) {
      this.street = street;
   }

   public String getPostCode() {
      return postCode;
   }

   public void setPostCode(String postCode) {
      this.postCode = postCode;
   }

   public int getNumber() {
      return number;
   }

   public void setNumber(int number) {
      this.number = number;
   }

   @Override
   public String toString() {
      return "Address{" +
            "street='" + street + '\'' +
            ", postCode='" + postCode + '\'' +
            ", number='" + number + '\'' +
            ", unknownFieldSet='" + unknownFieldSet + '\'' +
            '}';
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException {
      out.writeUTF(street);
      out.writeUTF(postCode);
      out.writeInt(number);
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      street = in.readUTF();
      postCode = in.readUTF();
      number = in.readInt();
   }
}
