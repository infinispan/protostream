package org.infinispan.protostream.impl;

import java.io.IOException;

import org.infinispan.protostream.Message;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.ProtoStreamMarshaller;
import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.UnknownFieldSetHandler;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
final class MessageMarshallerDelegate<T> extends BaseMarshallerDelegate<T> {

   private final MessageMarshaller<T> marshaller;

   private final Descriptor messageDescriptor;

   private final FieldDescriptor[] fieldDescriptors;

   private final UnknownFieldSetHandler<T> unknownFieldSetHandler;

   private static final UnknownFieldSetHandler<Message> legacyUnknownFieldSetHandler = new UnknownFieldSetHandler<Message>() {
      @Override
      public UnknownFieldSet getUnknownFieldSet(Message message) {
         return message.getUnknownFieldSet();
      }

      @Override
      public void setUnknownFieldSet(Message message, UnknownFieldSet unknownFieldSet) {
         message.setUnknownFieldSet(unknownFieldSet);
      }
   };

   MessageMarshallerDelegate(MessageMarshaller<T> marshaller, Descriptor messageDescriptor) {
      this.marshaller = marshaller;
      this.messageDescriptor = messageDescriptor;
      this.fieldDescriptors = messageDescriptor.getFields().toArray(new FieldDescriptor[0]);

      if (marshaller instanceof UnknownFieldSetHandler) {
         unknownFieldSetHandler = (UnknownFieldSetHandler<T>) marshaller;
      } else if (Message.class.isAssignableFrom(marshaller.getJavaClass())) {
         unknownFieldSetHandler = (UnknownFieldSetHandler<T>) legacyUnknownFieldSetHandler;
      } else {
         unknownFieldSetHandler = null;
      }
   }

   @Override
   public MessageMarshaller<T> getMarshaller() {
      return marshaller;
   }

   @Override
   public void marshall(ProtoStreamMarshaller.WriteContext ctx, FieldDescriptor fieldDescriptor, T message) throws IOException {
      ProtoStreamWriterImpl writer = (ProtoStreamWriterImpl) ctx.getWriter();
      ProtoStreamWriterImpl.WriteMessageContext messageContext = writer.enterContext(fieldDescriptor, messageDescriptor, (TagWriterImpl) ctx);

      marshaller.writeTo(writer, message);

      UnknownFieldSet unknownFieldSet = unknownFieldSetHandler != null ? unknownFieldSetHandler.getUnknownFieldSet(message) : null;

      if (unknownFieldSet != null && !unknownFieldSet.isEmpty()) {
         // validate that none of the unknown fields are actually declared by the known descriptor
         for (FieldDescriptor fd : fieldDescriptors) {
            if (unknownFieldSet.hasTag(fd.getWireTag())) {
               throw new IOException("Field " + fd.getFullName() + " is a known field so it is illegal to be present in the unknown field set");
            }
         }
         // write the unknown fields
         unknownFieldSet.writeTo(messageContext.out);
      }

      // validate that all the required fields were written either by the marshaller or by the UnknownFieldSet
      for (FieldDescriptor fd : fieldDescriptors) {
         if (fd.isRequired() && !messageContext.isFieldMarked(fd.getNumber())
               && (unknownFieldSet == null || !unknownFieldSet.hasTag(fd.getWireTag()))) {
            throw new IllegalStateException("Required field \"" + fd.getFullName()
                  + "\" should have been written by a calling a suitable method of "
                  + MessageMarshaller.ProtoStreamWriter.class.getCanonicalName());
         }
      }

      writer.exitContext();
   }

   @Override
   public T unmarshall(ProtoStreamMarshaller.ReadContext ctx, FieldDescriptor fieldDescriptor) throws IOException {
      ProtoStreamReaderImpl reader = (ProtoStreamReaderImpl) ctx.getReader();
      ProtoStreamReaderImpl.ReadMessageContext messageContext = reader.enterContext(fieldDescriptor, messageDescriptor, (TagReaderImpl) ctx);

      T message = marshaller.readFrom(reader);

      UnknownFieldSet unknownFieldSet = messageContext.getUnknownFieldSet();

      unknownFieldSet.readAllFields(ctx.getIn());

      if (unknownFieldSetHandler != null && !unknownFieldSet.isEmpty()) {
         unknownFieldSetHandler.setUnknownFieldSet(message, unknownFieldSet);
      }

      // check that all required fields were seen in the stream, even if not actually read (because are unknown)
      for (FieldDescriptor fd : fieldDescriptors) {
         if (fd.isRequired()
               && !messageContext.isFieldMarked(fd.getNumber())
               && !unknownFieldSet.hasTag(fd.getWireTag())) {
            throw new IOException("Required field \"" + fd.getFullName() + "\" was not encountered in the stream");
         }
      }

      reader.exitContext();
      return message;
   }
}
