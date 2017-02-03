package org.infinispan.protostream.domain.marshallers;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;

//todo write a code generator to generate this kind of boilerplate to provide a starting point for our users

/**
 * This is not the most exciting code to write but it allows maximum flexibility.
 *
 * @author anistor@redhat.com
 */
public class UserMarshaller implements MessageMarshaller<User> {

   @Override
   public String getTypeName() {
      return "sample_bank_account.User";
   }

   @Override
   public Class<? extends User> getJavaClass() {
      return User.class;
   }

   @Override
   public User readFrom(ProtoStreamReader reader) throws IOException {
      int id = reader.readInt("id");
      Set<Integer> accountIds = reader.readCollection("accountIds", new HashSet<>(), Integer.class);

      // Read them out of order. It still works but logs a warning!
      String surname = reader.readString("surname");
      String name = reader.readString("name");

      List<Address> addresses = reader.readCollection("addresses", new ArrayList<>(), Address.class);

      Integer age = reader.readInt("age");
      User.Gender gender = reader.readEnum("gender", User.Gender.class);
      String notes = reader.readString("notes");
      Instant creationDate = reader.readInstant("creationDate");
      Instant passwordExpirationDate = reader.readInstant("passwordExpirationDate");

      User user = new User();
      user.setId(id);
      user.setAccountIds(accountIds);
      user.setName(name);
      user.setSurname(surname);
      user.setAge(age);
      user.setGender(gender);
      user.setAddresses(addresses);
      user.setNotes(notes);
      user.setCreationDate(creationDate);
      user.setPasswordExpirationDate(passwordExpirationDate);
      return user;
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, User user) throws IOException {
      writer.writeInt("id", user.getId());
      writer.writeCollection("accountIds", user.getAccountIds(), Integer.class);
      writer.writeString("name", user.getName());
      writer.writeString("surname", user.getSurname());
      writer.writeCollection("addresses", user.getAddresses(), Address.class);
      writer.writeInt("age", user.getAge());
      writer.writeEnum("gender", user.getGender(), User.Gender.class);
      writer.writeString("notes", user.getNotes());
      writer.writeInstant("creationDate", user.getCreationDate());
      writer.writeInstant("passwordExpirationDate", user.getPasswordExpirationDate());
   }
}
