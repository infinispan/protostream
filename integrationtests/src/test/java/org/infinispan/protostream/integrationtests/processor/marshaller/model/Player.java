package org.infinispan.protostream.integrationtests.processor.marshaller.model;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class Player {

   private String name;
   private FootballTeam footballTeam;
   private Integer shirtNumber;
   private int matchRating;
   private byte[] bytes;

   @ProtoFactory
   public Player(String name, FootballTeam footballTeam, Integer shirtNumber, int matchRating, byte[] bytes) {
      this.name = name;
      this.footballTeam = footballTeam;
      this.shirtNumber = shirtNumber;
      this.matchRating = matchRating;
      this.bytes = bytes;
   }

   @ProtoField(value = 1)
   public String getName() {
      return name;
   }

   @ProtoField(value = 2)
   public FootballTeam getFootballTeam() {
      return footballTeam;
   }

   @ProtoField(3)
   public Integer getShirtNumber() {
      return shirtNumber;
   }

   @ProtoField(value = 4, defaultValue = "0")
   public int getMatchRating() {
      return matchRating;
   }

   @ProtoField(5)
   public byte[] getBytes() {
      return bytes;
   }
}
