package org.infinispan.protostream.domain.marshallers;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.domain.Note;
import org.infinispan.protostream.domain.User;

import java.io.IOException;

/**
 * @author anistor@redhat.com
 * @since 3.0
 */
public class NoteMarshaller implements MessageMarshaller<Note> {

   @Override
   public Note readFrom(ProtoStreamReader reader) throws IOException {
      String text = reader.readString("text");
      User author = reader.readObject("author", User.class);

      Note note = new Note();
      note.setText(text);
      note.setAuthor(author);
      return note;
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, Note note) throws IOException {
      writer.writeString("text", note.getText());
      writer.writeObject("author", note.getAuthor(), User.class);
   }

   @Override
   public Class<? extends Note> getJavaClass() {
      return Note.class;
   }

   @Override
   public String getTypeName() {
      return "sample_bank_account.Note";
   }
}
