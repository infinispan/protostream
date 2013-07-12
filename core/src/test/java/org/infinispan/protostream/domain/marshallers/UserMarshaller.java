package org.infinispan.protostream.domain.marshallers;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.domain.Address;
import org.infinispan.protostream.domain.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//todo write a code generator to generate this kind of boilerplate to provide a starting point for our users

/**
 * This is not the most exciting code to write but it allows maximum flexibility.
 *
 * @author anistor@redhat.com
 */
public class UserMarshaller implements MessageMarshaller<User> {

   @Override
   public String getFullName() {
      return "sample_bank_account.User";
   }

   // todo for efficiency's sake we must recommend user to write and read in the same order (which should also be identical with order declared in proto file)
   // todo (efficiency note) for collections we need to parse ahead, potentially until EOF
   @Override
   public User readFrom(ProtobufReader reader) throws IOException {   //todo must validate a non-repeated field is not present or attempted read multiple times
      int id = reader.readInt("id");
      List<Integer> accountIds = reader.readCollection("accountId", new ArrayList<Integer>(), Integer.class);

      // Read them out of order. It still works!
      String surname = reader.readString("surname");
      String name = reader.readString("name");

      //todo also handle readMap eventually
      List<Address> addresses = reader.readCollection("address", new ArrayList<Address>(), Address.class);

      Integer age = reader.readInt("age");
      User.Gender gender = reader.readObject("gender", User.Gender.class);

      User user = new User();
      user.setId(id);
      user.setAccountIds(accountIds);
      user.setName(name);
      user.setSurname(surname);
      user.setAge(age);
      user.setGender(gender);
      user.setAddresses(addresses);
      return user;
   }

   @Override
   public void writeTo(ProtobufWriter writer, User user) throws IOException {
      writer.writeInt("id", user.getId());
      writer.writeCollection("accountId", user.getAccountIds(), Integer.class);
      writer.writeString("name", user.getName());
      writer.writeString("surname", user.getSurname());
      writer.writeCollection("address", user.getAddresses(), Address.class);
      writer.writeInt("age", user.getAge());
      writer.writeObject("gender", user.getGender(), User.Gender.class);
   }
}

