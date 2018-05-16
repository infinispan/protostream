package org.infinispan.protostream.domain.marshallers;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.domain.Account;

/**
 * @author anistor@redhat.com
 */
public class LimitsMarshaller implements MessageMarshaller<Account.Limits> {

   @Override
   public String getTypeName() {
      return "sample_bank_account.Account.Limits";
   }

   @Override
   public Class<Account.Limits> getJavaClass() {
      return Account.Limits.class;
   }

   @Override
   public Account.Limits readFrom(ProtoStreamReader reader) throws IOException {
      double maxDailyLimit = reader.readDouble("maxDailyLimit");
      double maxTransactionLimit = reader.readDouble("maxTransactionLimit");
      String[] payees = reader.readArray("payees", String.class);

      Account.Limits account = new Account.Limits();
      account.setMaxDailyLimit(maxDailyLimit);
      account.setMaxTransactionLimit(maxTransactionLimit);
      account.setPayees(payees);
      return account;
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, Account.Limits limits) throws IOException {
      writer.writeDouble("maxDailyLimit", limits.getMaxDailyLimit());
      writer.writeDouble("maxTransactionLimit", limits.getMaxTransactionLimit());
      writer.writeArray("payees", limits.getPayees(), String.class);
   }
}
