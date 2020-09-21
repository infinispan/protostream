package org.infinispan.protostream.domain;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.descriptors.Type;

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

   private byte[] digest;

   private byte[] blurb;

   @ProtoDoc("First line of documentation.")
   @ProtoDoc("some foo bar\nand some more\n @Field(index=Index.YES, store=Store.NO, analyze=Analyze.NO)")
   @ProtoField(3)
   public Note note;

   @ProtoDoc("@Field")
   @ProtoField(4)
   public List<Note> notes;

   @ProtoField(1)
   public String getText() {
      return text;
   }

   public void setText(String text) {
      this.text = text;
   }

   @ProtoDoc("@Field")
   @ProtoField(2)
   public User getAuthor() {
      return author;
   }

   public void setAuthor(User author) {
      this.author = author;
   }

   @ProtoDoc("@Field")
   @ProtoField(number = 5, type = Type.UINT64, defaultValue = "0")
   public Date getCreationDate() {
      return creationDate;
   }

   public void setCreationDate(Date creationDate) {
      this.creationDate = creationDate;
   }

   @ProtoDoc("@Field")
   @ProtoField(6)
   public byte[] getDigest() {
      return digest;
   }

   public void setDigest(byte[] digest) {
      this.digest = digest;
   }

   @ProtoDoc("@Field")
   @ProtoField(number = 7, required = true)
   public byte[] getBlurb() {
      return blurb;
   }

   public void setBlurb(byte[] blurb) {
      this.blurb = blurb;
   }

   @Override
   public String toString() {
      return "Note{" +
            "text='" + text + '\'' +
            ", author=" + author +
            ", creationDate='" + creationDate + '\'' +
            ", digest=" + Arrays.toString(digest) +
            ", blurb=" + Arrays.toString(blurb) +
            ", note=" + note +
            ", notes=" + notes +
            '}';
   }
}
