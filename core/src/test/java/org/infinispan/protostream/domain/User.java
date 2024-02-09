package org.infinispan.protostream.domain;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.infinispan.custom.annotations.Indexed;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author anistor@redhat.com
 */
@Indexed
public class User implements Externalizable {   // implement Externalizable just for PerformanceTest

   public enum Gender {
      @ProtoEnumValue MALE, @ProtoEnumValue(value = 1) FEMALE
   }

   private int id;
   private String name;
   private String surname;
   private String salutation;
   private Set<Integer> accountIds;
   private List<Address> addresses;
   private Integer age;
   private Gender gender;
   private String notes;
   private Instant creationDate;
   private Instant passwordExpirationDate;
   private Long qrCode;
   private Address primaryAddress;
   private Date someDate;
   private float someFloat = 0.1f;
   private String someString;
   private double someDouble = 0.2;
   private boolean someBoolean = true;
   private long someLong = 34;
   private boolean someOtherBoolean;

   @ProtoField(number = 1, defaultValue = "0")
   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   @ProtoField(number = 2)
   public Set<Integer> getAccountIds() {
      return accountIds;
   }

   public void setAccountIds(Set<Integer> accountIds) {
      this.accountIds = accountIds;
   }

   @ProtoField(number = 3)
   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   @ProtoField(number = 4)
   public String getSurname() {
      return surname;
   }

   public void setSurname(String surname) {
      this.surname = surname;
   }

   @ProtoField(number = 5)
   public String getSalutation() {
      return salutation;
   }

   public void setSalutation(String salutation) {
      this.salutation = salutation;
   }

   @ProtoField(number = 6)
   public List<Address> getAddresses() {
      return addresses;
   }

   public void setAddresses(List<Address> addresses) {
      this.addresses = addresses;
   }

   @ProtoField(number = 7)
   public Integer getAge() {
      return age;
   }

   public void setAge(Integer age) {
      this.age = age;
   }

   @ProtoField(number = 8)
   public Gender getGender() {
      return gender;
   }

   public void setGender(Gender gender) {
      this.gender = gender;
   }

   @ProtoField(number = 9)
   public String getNotes() {
      return notes;
   }

   public void setNotes(String notes) {
      this.notes = notes;
   }

   @ProtoField(number = 10)
   public Instant getCreationDate() {
      return creationDate;
   }

   public void setCreationDate(Instant creationDate) {
      this.creationDate = creationDate;
   }

   @ProtoField(number = 11)
   public Instant getPasswordExpirationDate() {
      return passwordExpirationDate;
   }

   public void setPasswordExpirationDate(Instant passwordExpirationDate) {
      this.passwordExpirationDate = passwordExpirationDate;
   }

   @ProtoField(number = 12)
   public Long getQrCode() {
      return qrCode;
   }

   public void setQrCode(Long qrCode) {
      this.qrCode = qrCode;
   }

   @ProtoField(number = 13)
   public Address getPrimaryAddress() {
      return primaryAddress;
   }

   public void setPrimaryAddress(Address primaryAddress) {
      this.primaryAddress = primaryAddress;
   }

   @ProtoField(number = 14)
   public Date getSomeDate() {
      return someDate;
   }

   public void setSomeDate(Date someDate) {
      this.someDate = someDate;
   }

   @ProtoField(number = 15, defaultValue = "0.1f")
   public float getSomeFloat() {
      return someFloat;
   }

   public void setSomeFloat(float someFloat) {
      this.someFloat = someFloat;
   }

   @ProtoField(number = 16)
   public String getSomeString() {
      return someString;
   }

   public void setSomeString(String someString) {
      this.someString = someString;
   }

   @ProtoField(number = 17, defaultValue = "0.2")
   public double getSomeDouble() {
      return someDouble;
   }

   public void setSomeDouble(double someDouble) {
      this.someDouble = someDouble;
   }

   @ProtoField(number = 18, defaultValue = "true")
   public boolean isSomeBoolean() {
      return someBoolean;
   }

   public void setSomeBoolean(boolean someBoolean) {
      this.someBoolean = someBoolean;
   }

   @ProtoField(number = 19, defaultValue = "34")
   public long getSomeLong() {
      return someLong;
   }

   public void setSomeLong(long someLong) {
      this.someLong = someLong;
   }

   @ProtoField(number = 20, defaultValue = "false")
   public boolean isSomeOtherBoolean() {
      return someOtherBoolean;
   }

   public void setSomeOtherBoolean(boolean someOtherBoolean) {
      this.someOtherBoolean = someOtherBoolean;
   }

   @Override
   public String toString() {
      return "User{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", surname='" + surname + '\'' +
            ", salutation='" + salutation + '\'' +
            ", accountIds=" + accountIds +
            ", addresses=" + addresses +
            ", age=" + age +
            ", gender=" + gender +
            ", notes=" + notes +
            ", creationDate='" + creationDate + '\'' +
            ", passwordExpirationDate='" + passwordExpirationDate + '\'' +
            ", qrCode=" + qrCode +
            ", primaryAddress=" + primaryAddress +
            ", someDate=" + someDate +
            ", someFloat=" + someFloat +
            ", someString=" + someString +
            ", someDouble=" + someDouble +
            ", someBoolean=" + someBoolean +
            ", someLong=" + someLong +
            ", someOtherBoolean=" + someOtherBoolean +
            '}';
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException {
      out.writeInt(id);
      out.writeUTF(name);
      if (surname != null) {
         out.writeBoolean(true);
         out.writeUTF(surname);
      } else {
         out.writeBoolean(false);
      }
      if (salutation != null) {
         out.writeBoolean(true);
         out.writeUTF(salutation);
      } else {
         out.writeBoolean(false);
      }
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
      if (gender != null) {
         out.writeBoolean(true);
         out.writeInt(gender.ordinal());
      } else {
         out.writeBoolean(false);
      }
      if (notes != null) {
         out.writeBoolean(true);
         out.writeUTF(notes);
      } else {
         out.writeBoolean(false);
      }
      if (creationDate != null) {
         out.writeBoolean(true);
         out.writeLong(creationDate.getEpochSecond());
         out.writeInt(creationDate.getNano());
      } else {
         out.writeBoolean(false);
      }
      if (passwordExpirationDate != null) {
         out.writeBoolean(true);
         out.writeLong(passwordExpirationDate.getEpochSecond());
         out.writeInt(passwordExpirationDate.getNano());
      } else {
         out.writeBoolean(false);
      }
      if (qrCode != null) {
         out.writeBoolean(true);
         out.writeLong(qrCode);
      } else {
         out.writeBoolean(false);
      }
      if (primaryAddress != null) {
         out.writeBoolean(true);
         out.writeObject(primaryAddress);
      } else {
         out.writeBoolean(false);
      }
      if (someDate != null) {
         out.writeBoolean(true);
         out.writeObject(someDate);
      } else {
         out.writeBoolean(false);
      }
      out.writeFloat(someFloat);
      if (someString != null) {
         out.writeBoolean(true);
         out.writeObject(someString);
      } else {
         out.writeBoolean(false);
      }
      out.writeDouble(someDouble);
      out.writeBoolean(someBoolean);
      out.writeLong(someLong);
      out.writeBoolean(someOtherBoolean);
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      id = in.readInt();
      name = in.readUTF();
      if (in.readBoolean()) {
         surname = in.readUTF();
      }
      if (in.readBoolean()) {
         salutation = in.readUTF();
      }
      int numAccountIds = in.readInt();
      if (numAccountIds >= 0) {
         accountIds = new HashSet<>(numAccountIds);
         for (int i = 0; i < numAccountIds; i++) {
            accountIds.add(in.readInt());
         }
      }
      int numAddresses = in.readInt();
      if (numAddresses >= 0) {
         addresses = new ArrayList<>(numAddresses);
         for (int i = 0; i < numAddresses; i++) {
            addresses.add((Address) in.readObject());
         }
      }
      if (in.readBoolean()) {
         age = in.readInt();
      }
      if (in.readBoolean()) {
         gender = Gender.values()[in.readInt()];
      }
      if (in.readBoolean()) {
         notes = in.readUTF();
      }
      if (in.readBoolean()) {
         long seconds = in.readLong();
         int nanos = in.readInt();
         creationDate = Instant.ofEpochSecond(seconds, nanos);
      }
      if (in.readBoolean()) {
         long seconds = in.readLong();
         int nanos = in.readInt();
         passwordExpirationDate = Instant.ofEpochSecond(seconds, nanos);
      }
      if (in.readBoolean()) {
         qrCode = in.readLong();
      }
      if (in.readBoolean()) {
         primaryAddress = (Address) in.readObject();
      }
      if (in.readBoolean()) {
         someDate = (Date) in.readObject();
      }
      someFloat = in.readFloat();
      if (in.readBoolean()) {
         someString = (String) in.readObject();
      }
      someDouble = in.readDouble();
      someBoolean = in.readBoolean();
      someLong = in.readLong();
      someOtherBoolean = in.readBoolean();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      User user = (User) o;
      return id == user.id &&
            Objects.equals(name, user.name) &&
            Objects.equals(surname, user.surname) &&
            Objects.equals(salutation, user.salutation) &&
            Objects.equals(accountIds, user.accountIds) &&
            Objects.equals(addresses, user.addresses) &&
            Objects.equals(age, user.age) &&
            gender == user.gender &&
            Objects.equals(notes, user.notes) &&
            Objects.equals(creationDate, user.creationDate) &&
            Objects.equals(passwordExpirationDate, user.passwordExpirationDate) &&
            Objects.equals(qrCode, user.qrCode) &&
            Objects.equals(primaryAddress, user.primaryAddress) &&
            Objects.equals(someDate, user.someDate) &&
            someFloat == user.someFloat &&
            Objects.equals(someString, user.someString) &&
            someDouble == user.someDouble &&
            someBoolean == user.someBoolean &&
            someLong == user.someLong &&
            someOtherBoolean == user.someOtherBoolean;
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, name, surname, salutation, accountIds, addresses, age, gender, notes, creationDate,
            passwordExpirationDate, qrCode, primaryAddress, someDate, someFloat, someString, someDouble, someBoolean,
            someLong, someOtherBoolean);
   }
}
