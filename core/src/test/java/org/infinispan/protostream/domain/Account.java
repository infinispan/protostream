package org.infinispan.protostream.domain;

import org.infinispan.protostream.BaseMessage;

import java.util.Date;
import java.util.List;

/**
 * @author anistor@redhat.com
 */
public class Account extends BaseMessage {

   private int id;
   private String description;
   private Date creationDate;
   private Limits limits;
   private List<byte[]> blurb;

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

   @Override
   public String toString() {
      return "Account{" +
            "id=" + id +
            ", description='" + description + '\'' +
            ", creationDate='" + creationDate + '\'' +
            ", limits=" + limits +
            ", blurb=" + blurb +
            ", unknownFieldSet='" + unknownFieldSet + '\'' +
            '}';
   }
}
