package org.infinispan.protostream.domain;

import java.math.BigDecimal;
import java.util.Date;

import org.infinispan.protostream.Message;
import org.infinispan.protostream.UnknownFieldSet;

/**
 * @author anistor@redhat.com
 */
public class Transaction implements Message {

   private int id;
   private String description;
   private String longDescription;
   private String notes;
   private int accountId;
   private Date date;
   private BigDecimal amount;
   private boolean isDebit;
   private boolean isValid;

   private UnknownFieldSet unknownFieldSet;

   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getLongDescription() {
      return longDescription;
   }

   public void setLongDescription(String longDescription) {
      this.longDescription = longDescription;
   }

   public String getNotes() {
      return notes;
   }

   public void setNotes(String notes) {
      this.notes = notes;
   }

   public int getAccountId() {
      return accountId;
   }

   public void setAccountId(int accountId) {
      this.accountId = accountId;
   }

   public Date getDate() {
      return date;
   }

   public void setDate(Date date) {
      this.date = date;
   }

   public double getAmount() {
      return amount.doubleValue();
   }

   public void setAmount(double amount) {
      this.amount = new BigDecimal(amount);
   }

   public void setAmount(BigDecimal amount) {
      this.amount = amount;
   }

   public boolean isDebit() {
      return isDebit;
   }

   public void setDebit(boolean isDebit) {
      this.isDebit = isDebit;
   }

   public boolean isValid() {
      return isValid;
   }

   public void setValid(boolean isValid) {
      this.isValid = isValid;
   }

   @Override
   public UnknownFieldSet getUnknownFieldSet() {
      return unknownFieldSet;
   }

   @Override
   public void setUnknownFieldSet(UnknownFieldSet unknownFieldSet) {
      this.unknownFieldSet = unknownFieldSet;
   }

   @Override
   public String toString() {
      return "Transaction{" +
            "id=" + id +
            ", description='" + description + '\'' +
            ", longDescription='" + longDescription + '\'' +
            ", notes='" + notes + '\'' +
            ", accountId=" + accountId +
            ", date=" + date +
            ", amount=" + amount +
            ", isDebit=" + isDebit +
            ", isValid=" + isValid +
            ", unknownFieldSet=" + unknownFieldSet +
            '}';
   }
}
