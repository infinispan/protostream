package org.infinispan.protostream.domain;

import org.infinispan.protostream.BaseMessage;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author anistor@redhat.com
 */
@ProtoDoc("@Indexed")
public class User extends BaseMessage implements Externalizable {   // implement Externalizable just for PerformanceTest

   public enum Gender {
      MALE, FEMALE
   }

   private int id;
   private String name;
   private String surname;
   private Set<Integer> accountIds;
   private List<Address> addresses;
   private Integer age;
   private Gender gender;
   private String notes;

   @ProtoField(number = 1, required = true)
   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   @ProtoField(number = 2, collectionImplementation = HashSet.class)
   public Set<Integer> getAccountIds() {
      return accountIds;
   }

   public void setAccountIds(Set<Integer> accountIds) {
      this.accountIds = accountIds;
   }

   @ProtoField(number = 3, required = true)
   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   @ProtoField(number = 4, required = true)
   public String getSurname() {
      return surname;
   }

   public void setSurname(String surname) {
      this.surname = surname;
   }

   @ProtoField(number = 5, collectionImplementation = ArrayList.class)
   public List<Address> getAddresses() {
      return addresses;
   }

   public void setAddresses(List<Address> addresses) {
      this.addresses = addresses;
   }

   @ProtoField(number = 6)
   public Integer getAge() {
      return age;
   }

   public void setAge(Integer age) {
      this.age = age;
   }

   @ProtoField(number = 7)
   public Gender getGender() {
      return gender;
   }

   public void setGender(Gender gender) {
      this.gender = gender;
   }

   @ProtoField(number = 8)
   public String getNotes() {
      return notes;
   }

   public void setNotes(String notes) {
      this.notes = notes;
   }

   @Override
   public String toString() {
      return "User{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", surname='" + surname + '\'' +
            ", accountIds=" + accountIds +
            ", addresses=" + addresses +
            ", age=" + age +
            ", gender=" + gender +
            ", notes=" + notes +
            ", unknownFieldSet=" + unknownFieldSet +
            '}';
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException {
      out.writeInt(id);
      out.writeUTF(name);
      out.writeUTF(surname);
      if (accountIds == null) {
         out.writeInt(-1);
      } else {
         out.writeInt(accountIds.size());
         for (Integer accountId : accountIds) {
            out.writeInt(accountId);
         }
      }
      if (addresses == null) {
         out.writeInt(-1);
      } else {
         out.writeInt(addresses.size());
         for (Address address : addresses) {
            out.writeObject(address);
         }
      }
      if (age != null) {
         out.writeBoolean(true);
         out.writeInt(age);
      } else {
         out.writeBoolean(false);
      }
      out.writeInt(gender.ordinal());
      out.writeUTF(notes);
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      id = in.readInt();
      name = in.readUTF();
      surname = in.readUTF();
      int numAccountIds = in.readInt();
      if (numAccountIds == -1) {
         accountIds = null;
      } else {
         accountIds = new HashSet<Integer>(numAccountIds);
         for (int i = 0; i < numAccountIds; i++) {
            accountIds.add(in.readInt());
         }
      }
      int numAddresses = in.readInt();
      if (numAddresses == -1) {
         addresses = null;
      } else {
         addresses = new ArrayList<Address>(numAddresses);
         for (int i = 0; i < numAddresses; i++) {
            addresses.add((Address) in.readObject());
         }
      }
      if (in.readBoolean()) {
         age = in.readInt();
      } else {
         age = null;
      }
      gender = User.Gender.values()[in.readInt()];
      notes = in.readUTF();
   }
}
