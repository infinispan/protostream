package org.infinispan.protostream.domain;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An annotated entity.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
@ProtoDoc("@Indexed")
public class Note {

   private String text;

   private User author;

   private Date creationDate;

   @ProtoDoc("@IndexedField")
   @ProtoField(number = 3)
   public Note note;

   @ProtoDoc("@IndexedField")
   @ProtoField(number = 4, collectionImplementation = ArrayList.class)
   public List<Note> notes;

   @ProtoField(number = 1)
   public String getText() {
      return text;
   }

   public void setText(String text) {
      this.text = text;
   }

   @ProtoDoc("@IndexedField")
   @ProtoField(number = 2)
   public User getAuthor() {
      return author;
   }

   public void setAuthor(User author) {
      this.author = author;
   }

   @ProtoDoc("@IndexedField")
   @ProtoField(number = 5, type = Type.UINT64, required = false, defaultValue = "0")
   public Date getCreationDate() {
      return creationDate;
   }

   public void setCreationDate(Date creationDate) {
      this.creationDate = creationDate;
   }
}
