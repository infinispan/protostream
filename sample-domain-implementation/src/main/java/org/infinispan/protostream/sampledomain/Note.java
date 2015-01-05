package org.infinispan.protostream.sampledomain;

import org.infinispan.protostream.annotations.ProtoField;

/**
 * An annotated entity.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public class Note {

   private String text;

   private User author;

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

   @Override
   public String toString() {
      return "Note{" +
            "text='" + text + '\'' +
            ", author=" + author +
            '}';
   }
}
