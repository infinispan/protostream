package org.infinispan.protostream.domain.marshallers;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.domain.Account;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 */
public class AccountMarshaller implements MessageMarshaller<Account> {

   @Override
   public String getFullName() {
      return "sample_bank_account.Account";
   }

   @Override
   public Account readFrom(ProtoStreamReader reader) throws IOException {
      int id = reader.readInt("id");
      String description = reader.readString("description");

      Account account = new Account();
      account.setId(id);
      account.setDescription(description);
      return account;
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, Account account) throws IOException {
      writer.writeInt("id", account.getId());
      writer.writeString("description", account.getDescription());
   }
}
