package org.infinispan.protostream.impl;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors;
import org.infinispan.protostream.Message;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.UnknownFieldSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class MessageMarshallerDelegate<T> implements BaseMarshallerDelegate<T> {

   private final MessageMarshaller<T> marshaller;

   private final Descriptors.Descriptor messageDescriptor;

   private final Descriptors.FieldDescriptor[] fieldDescriptors;

   private final Map<String, Descriptors.FieldDescriptor> fieldsByName;

   public MessageMarshallerDelegate(MessageMarshaller<T> marshaller, Descriptors.Descriptor messageDescriptor) {
      this.marshaller = marshaller;
      this.messageDescriptor = messageDescriptor;
      List<Descriptors.FieldDescriptor> fields = messageDescriptor.getFields();
      fieldDescriptors = fields.toArray(new Descriptors.FieldDescriptor[fields.size()]);
      fieldsByName = new HashMap<String, Descriptors.FieldDescriptor>(fieldDescriptors.length);
      for (Descriptors.FieldDescriptor fd : fieldDescriptors) {
         fieldsByName.put(fd.getName(), fd);
      }
   }

   @Override
   public MessageMarshaller<T> getMarshaller() {
      return marshaller;
   }

   public Descriptors.Descriptor getMessageDescriptor() {
      return messageDescriptor;
   }

   public Descriptors.FieldDescriptor[] getFieldDescriptors() {
      return fieldDescriptors;
   }

   public Map<String, Descriptors.FieldDescriptor> getFieldsByName() {
      return fieldsByName;
   }

   @Override
   public void marshall(String fieldName, Descriptors.FieldDescriptor fieldDescriptor, T value, ProtoStreamWriterImpl writer, CodedOutputStream out) throws IOException {
      WriteMessageContext messageContext = writer.pushContext(fieldName, this, out);

      marshaller.writeTo(writer, value);

      if (value instanceof Message) {
         UnknownFieldSet unknownFieldSet = ((Message) value).getUnknownFieldSet();
         if (unknownFieldSet != null) {
            // validate that none of the unknown fields are also declared by the known descriptor
            for (Descriptors.FieldDescriptor fd : getFieldDescriptors()) {
               if (unknownFieldSet.hasTag(WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType()))) {
                  throw new IOException("Field " + fd.getFullName() + " is a known field so it is illegal to be present in the unknown field set");
               }
            }
            // write the unknown fields
            unknownFieldSet.writeTo(messageContext.out);
         }
      }

      UnknownFieldSet unknownFieldSet = value instanceof Message ? ((Message) value).getUnknownFieldSet() : null;

      // validate that all the required fields were written either by the marshaller or by the UnknownFieldSet
      for (Descriptors.FieldDescriptor fd : getFieldDescriptors()) {
         if (fd.isRequired() && !messageContext.isFieldMarked(fd.getNumber())
               && (unknownFieldSet == null || !unknownFieldSet.hasTag(WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType())))) {
            throw new IllegalStateException("Required field \"" + fd.getFullName()
                                                  + "\" should have been written by a calling a suitable method of "
                                                  + MessageMarshaller.ProtoStreamWriter.class.getName());
         }
      }

      writer.popContext();
   }

   @Override
   public T unmarshall(String fieldName, Descriptors.FieldDescriptor fieldDescriptor, ProtoStreamReaderImpl reader, CodedInputStream in) throws IOException {
      ReadMessageContext messageContext = reader.pushContext(fieldName, this, in);

      T a = marshaller.readFrom(reader);

      messageContext.unknownFieldSet.readAllFields(in);

      if (a instanceof Message && !messageContext.unknownFieldSet.isEmpty()) {
         ((Message) a).setUnknownFieldSet(messageContext.unknownFieldSet);
      }

      // check that all required fields were seen in the stream, even if not actually read (because are unknown)
      for (Descriptors.FieldDescriptor fd : getFieldDescriptors()) {
         if (fd.isRequired()
               && !messageContext.isFieldMarked(fd.getNumber())
               && !messageContext.unknownFieldSet.hasTag(WireFormat.makeTag(fd.getNumber(), fd.getLiteType().getWireType()))) {
            throw new IOException("Required field \"" + fd.getFullName() + "\" was not encountered in the stream");
         }
      }

      reader.popContext();
      return a;
   }
}
