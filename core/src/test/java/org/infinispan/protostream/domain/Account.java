package org.infinispan.protostream.domain;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.infinispan.protostream.BaseMessage;

/**
 * @author anistor@redhat.com
 */
public class Account extends BaseMessage {

   public enum Currency {
      EUR, GBP, USD, BRL
   }

   private int id;
   private String description;
   private Date creationDate;
   private Limits limits;
   private List<byte[]> blurb;
   private Currency[] currencies;

   public static class Limits extends BaseMessage {

      private Double maxDailyLimit;

      private Double maxTransactionLimit;

      public Double getMaxDailyLimit() {
         return maxDailyLimit;
      }

      public void setMaxDailyLimit(Double maxDailyLimit) {
         this.maxDailyLimit = maxDailyLimit;
      }

      public Double getMaxTransactionLimit() {
         return maxTransactionLimit;
      }

      public void setMaxTransactionLimit(Double maxTransactionLimit) {
         this.maxTransactionLimit = maxTransactionLimit;
      }

      @Override
      public String toString() {
         return "Limits{" +
               "maxDailyLimit=" + maxDailyLimit +
               ", maxTransactionLimit=" + maxTransactionLimit +
               '}';
      }
   }

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

   public Date getCreationDate() {
      return creationDate;
   }

   public void setCreationDate(Date creationDate) {
      this.creationDate = creationDate;
   }

   public Limits getLimits() {
      return limits;
   }

   public void setLimits(Limits limits) {
      this.limits = limits;
   }

   public List<byte[]> getBlurb() {
      return blurb;
   }

   public void setBlurb(List<byte[]> blurb) {
      this.blurb = blurb;
   }

   public Currency[] getCurrencies() {
      return currencies;
   }

   public void setCurrencies(Currency[] currencies) {
      this.currencies = currencies;
   }

   @Override
   public String toString() {
      return "Account{" +
            "id=" + id +
            ", description='" + description + '\'' +
            ", creationDate='" + creationDate + '\'' +
            ", limits=" + limits +
            ", blurb=" + blurb +
            ", currencies=" + Arrays.toString(currencies) +
            ", unknownFieldSet='" + unknownFieldSet + '\'' +
            '}';
   }
}
