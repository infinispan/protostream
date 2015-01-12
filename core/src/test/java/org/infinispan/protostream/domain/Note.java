package org.infinispan.protostream.domain;

import org.infinispan.protostream.annotations.ProtoField;

import java.util.ArrayList;
import java.util.List;

/**
 * An annotated entity.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public class Note {

   private String text;

   private User author;

   @ProtoField(number = 3)
   public Note note;

   @ProtoField(number = 4, collectionImplementation = ArrayList.class)
   public List<Note> notes;

   @ProtoField(number = 1)
   public String getText() {
      return text;
   }

   public void setText(String text) {
      this.text = text;
   }

   @ProtoField(number = 2)
   public User getAuthor() {
      return author;
   }

   public void setAuthor(User author) {
      this.author = author;
   }
}
