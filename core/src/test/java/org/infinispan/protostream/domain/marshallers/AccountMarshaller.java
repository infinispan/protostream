package org.infinispan.protostream.domain.marshallers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.domain.Account;

/**
 * @author anistor@redhat.com
 */
public class AccountMarshaller implements MessageMarshaller<Account> {

   @Override
   public String getTypeName() {
      return "sample_bank_account.Account";
   }

   @Override
   public Class<? extends Account> getJavaClass() {
      return Account.class;
   }

   @Override
   public Account readFrom(ProtoStreamReader reader) throws IOException {
      int id = reader.readInt("id");
      String description = reader.readString("description");
      Date creationDate = reader.readDate("creationDate");
      Account.Limits limits = reader.readObject("limits", Account.Limits.class);
      List<byte[]> blurb = reader.readCollection("blurb", new ArrayList<>(), byte[].class);

      Account account = new Account();
      account.setId(id);
      account.setDescription(description);
      account.setCreationDate(creationDate);
      account.setLimits(limits);
      account.setBlurb(blurb);
      return account;
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, Account account) throws IOException {
      writer.writeInt("id", account.getId());
      writer.writeString("description", account.getDescription());
      writer.writeDate("creationDate", account.getCreationDate());
      writer.writeObject("limits", account.getLimits(), Account.Limits.class);
      writer.writeCollection("blurb", account.getBlurb(), byte[].class);
   }
}
