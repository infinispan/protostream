package org.infinispan.protostream.domain;

import org.infinispan.protostream.BaseMessage;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author anistor@redhat.com
 */
public class Address extends BaseMessage implements Externalizable {  // implement Externalizable just for PerformanceTest

   private String street;
   private String postCode;

   public Address() {
   }

   public Address(String street, String postCode) {
      this.street = street;
      this.postCode = postCode;
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

   @Override
   public String toString() {
      return "Address{" +
            "street='" + street + '\'' +
            ", postCode='" + postCode + '\'' +
            ", unknownFieldSet='" + unknownFieldSet + '\'' +
            '}';
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException {
      out.writeUTF(street);
      out.writeUTF(postCode);
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      street = in.readUTF();
      postCode = in.readUTF();
   }
}
