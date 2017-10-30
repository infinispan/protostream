package org.infinispan.protostream.impl;

import java.io.IOException;
import java.util.List;

import org.infinispan.protostream.Message;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.UnknownFieldSet;
import org.infinispan.protostream.UnknownFieldSetHandler;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
final class MessageMarshallerDelegate<T> implements BaseMarshallerDelegate<T> {

   private final SerializationContextImpl ctx;

   private final MessageMarshaller<T> marshaller;

   private final Descriptor messageDescriptor;

   private final FieldDescriptor[] fieldDescriptors;

   MessageMarshallerDelegate(SerializationContextImpl ctx, MessageMarshaller<T> marshaller, Descriptor messageDescriptor) {
      this.ctx = ctx;
      this.marshaller = marshaller;
      this.messageDescriptor = messageDescriptor;
      List<FieldDescriptor> fields = messageDescriptor.getFields();
      fieldDescriptors = fields.toArray(new FieldDescriptor[fields.size()]);
   }

   @Override
   public MessageMarshaller<T> getMarshaller() {
      return marshaller;
   }

   Descriptor getMessageDescriptor() {
      return messageDescriptor;
   }

   FieldDescriptor getFieldByName(String fieldName) throws IOException {
      FieldDescriptor fd = messageDescriptor.findFieldByName(fieldName);
      if (fd == null) {
         throw new IOException("Unknown field name : " + fieldName);
      }
      return fd;
   }

   @Override
   public void marshall(FieldDescriptor fieldDescriptor, T message, ProtoStreamWriterImpl writer, RawProtoStreamWriter out) throws IOException {
      if (writer == null) {
         writer = new ProtoStreamWriterImpl(ctx);
      }
      WriteMessageContext messageContext = writer.pushContext(fieldDescriptor, this, out);

      marshaller.writeTo(writer, message);

      UnknownFieldSet unknownFieldSet = null;
      if (marshaller instanceof UnknownFieldSetHandler) {
         unknownFieldSet = ((UnknownFieldSetHandler<T>) marshaller).getUnknownFieldSet(message);
      } else if (message instanceof Message) {
         unknownFieldSet = ((Message) message).getUnknownFieldSet();
      }

      if (unknownFieldSet != null) {
         // validate that none of the unknown fields are actually declared by the known descriptor
         for (FieldDescriptor fd : fieldDescriptors) {
            if (unknownFieldSet.hasTag(WireFormat.makeTag(fd.getNumber(), fd.getType().getWireType()))) {
               throw new IOException("Field " + fd.getFullName() + " is a known field so it is illegal to be present in the unknown field set");
            }
         }
         // write the unknown fields
         unknownFieldSet.writeTo(messageContext.out);
      }

      // validate that all the required fields were written either by the marshaller or by the UnknownFieldSet
      for (FieldDescriptor fd : fieldDescriptors) {
         if (fd.isRequired() && !messageContext.isFieldMarked(fd.getNumber())
               && (unknownFieldSet == null || !unknownFieldSet.hasTag(WireFormat.makeTag(fd.getNumber(), fd.getType().getWireType())))) {
            throw new IllegalStateException("Required field \"" + fd.getFullName()
                  + "\" should have been written by a calling a suitable method of "
                  + MessageMarshaller.ProtoStreamWriter.class.getName());
         }
      }

      writer.popContext();
   }

   @Override
   public T unmarshall(FieldDescriptor fieldDescriptor, ProtoStreamReaderImpl reader, RawProtoStreamReader in) throws IOException {
      if (reader == null) {
         reader = new ProtoStreamReaderImpl(ctx);
      }
      ReadMessageContext messageContext = reader.pushContext(fieldDescriptor, this, in);

      T message = marshaller.readFrom(reader);

      messageContext.unknownFieldSet.readAllFields(in);

      if (!messageContext.unknownFieldSet.isEmpty()) {
         if (marshaller instanceof UnknownFieldSetHandler) {
            ((UnknownFieldSetHandler<T>) marshaller).setUnknownFieldSet(message, messageContext.unknownFieldSet);
         } else if (message instanceof Message) {
            ((Message) message).setUnknownFieldSet(messageContext.unknownFieldSet);
         }
      }

      // check that all required fields were seen in the stream, even if not actually read (because are unknown)
      for (FieldDescriptor fd : fieldDescriptors) {
         if (fd.isRequired()
               && !messageContext.isFieldMarked(fd.getNumber())
               && !messageContext.unknownFieldSet.hasTag(WireFormat.makeTag(fd.getNumber(), fd.getType().getWireType()))) {
            throw new IOException("Required field \"" + fd.getFullName() + "\" was not encountered in the stream");
         }
      }

      reader.popContext();
      return message;
   }
}
