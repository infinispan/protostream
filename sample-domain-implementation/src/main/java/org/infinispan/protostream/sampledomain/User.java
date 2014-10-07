package org.infinispan.protostream.sampledomain;

import org.infinispan.protostream.BaseMessage;

import java.util.List;
import java.util.Set;

/**
 * @author anistor@redhat.com
 */
public class User extends BaseMessage {

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

   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public Set<Integer> getAccountIds() {
      return accountIds;
   }

   public void setAccountIds(Set<Integer> accountIds) {
      this.accountIds = accountIds;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getSurname() {
      return surname;
   }

   public void setSurname(String surname) {
      this.surname = surname;
   }

   public List<Address> getAddresses() {
      return addresses;
   }

   public void setAddresses(List<Address> addresses) {
      this.addresses = addresses;
   }

   public Integer getAge() {
      return age;
   }

   public void setAge(Integer age) {
      this.age = age;
   }

   public Gender getGender() {
      return gender;
   }

   public void setGender(Gender gender) {
      this.gender = gender;
   }

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
}
