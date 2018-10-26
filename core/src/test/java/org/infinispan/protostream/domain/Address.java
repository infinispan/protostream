package org.infinispan.protostream.domain;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import org.infinispan.protostream.BaseMessage;

/**
 * @author anistor@redhat.com
 */
public class Address extends BaseMessage implements Externalizable {  // implement Externalizable just for PerformanceTest

   private String street;
   private String postCode;
   private int number;
   private boolean isCommercial;

   public Address() {
   }

   public Address(String street, String postCode, int number) {
      this(street, postCode, number, false);
   }

   public Address(String street, String postCode, int number, boolean isCommercial) {
      this.street = street;
      this.postCode = postCode;
      this.number = number;
      this.isCommercial = isCommercial;
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

   public boolean isCommercial() {
      return isCommercial;
   }

   public void setCommercial(boolean isCommercial) {
      this.isCommercial = isCommercial;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Address address = (Address) o;
      return number == address.number &&
            isCommercial == address.isCommercial &&
            Objects.equals(street, address.street) &&
            Objects.equals(postCode, address.postCode);
   }

   @Override
   public int hashCode() {
      return Objects.hash(street, postCode, number, isCommercial);
   }

   @Override
   public String toString() {
      return "Address{" +
            "street='" + street + '\'' +
            ", postCode='" + postCode + '\'' +
            ", number=" + number +
            ", isCommercial=" + isCommercial +
            ", unknownFieldSet=" + unknownFieldSet +
            '}';
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException {
      out.writeUTF(street);
      out.writeUTF(postCode);
      out.writeInt(number);
      out.writeBoolean(isCommercial);
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException {
      street = in.readUTF();
      postCode = in.readUTF();
      number = in.readInt();
      isCommercial = in.readBoolean();
   }
}
