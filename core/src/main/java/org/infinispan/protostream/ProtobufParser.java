package org.infinispan.protostream;

import java.io.IOException;
import java.io.InputStream;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.Type;
import org.infinispan.protostream.descriptors.WireType;
import org.infinispan.protostream.impl.TagReaderImpl;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class ProtobufParser {

   public static final ProtobufParser INSTANCE = new ProtobufParser();

   public void parse(TagHandler tagHandler, Descriptor messageDescriptor, InputStream input) throws IOException {
      if (messageDescriptor == null) {
         throw new IllegalArgumentException("messageDescriptor cannot be null");
      }
      TagReader in = TagReaderImpl.newInstance(null, input);

      parseInternal(tagHandler, messageDescriptor, in);
   }

   public void parse(TagHandler tagHandler, Descriptor messageDescriptor, byte[] buf, int off, int len) throws IOException {
      if (messageDescriptor == null) {
         throw new IllegalArgumentException("messageDescriptor cannot be null");
      }
      TagReader in = TagReaderImpl.newInstance(null, buf, off, len);

      parseInternal(tagHandler, messageDescriptor, in);
   }

   public void parse(TagHandler tagHandler, Descriptor messageDescriptor, byte[] buf) throws IOException {
      if (messageDescriptor == null) {
         throw new IllegalArgumentException("messageDescriptor cannot be null");
      }
      TagReader in = TagReaderImpl.newInstance(null, buf);

      parseInternal(tagHandler, messageDescriptor, in);
   }

   public void parse(TagHandler tagHandler, Descriptor messageDescriptor, TagReader in) throws IOException {
      if (messageDescriptor == null) {
         throw new IllegalArgumentException("messageDescriptor cannot be null");
      }

      parseInternal(tagHandler, messageDescriptor, in);
   }

   private void parseInternal(TagHandler tagHandler, Descriptor messageDescriptor, TagReader in) throws IOException {
      tagHandler.onStart(messageDescriptor);
      parseMessage(tagHandler, messageDescriptor, in);
      tagHandler.onEnd();
   }

   private void parseMessage(TagHandler tagHandler, Descriptor messageDescriptor, TagReader in) throws IOException {
      int tag;
      while ((tag = in.readTag()) != 0) {
         final int fieldNumber = WireType.getTagFieldNumber(tag);
         final WireType wireType = WireType.fromTag(tag);
         final FieldDescriptor fd = messageDescriptor != null ? messageDescriptor.findFieldByNumber(fieldNumber) : null;

         switch (wireType) {
            case LENGTH_DELIMITED: {
               if (fd == null) {
                  byte[] value = in.readByteArray();
                  tagHandler.onTag(fieldNumber, null, value);
               } else if (fd.getType() == Type.STRING) {
                  String value = in.readString();
                  tagHandler.onTag(fieldNumber, fd, value);
               } else if (fd.getType() == Type.BYTES) {
                  byte[] value = in.readByteArray();
                  tagHandler.onTag(fieldNumber, fd, value);
               } else if (fd.getType() == Type.MESSAGE) {
                  int length = in.readUInt32();
                  int oldLimit = in.pushLimit(length);
                  tagHandler.onStartNested(fieldNumber, fd);
                  parseMessage(tagHandler, fd.getMessageType(), in);
                  tagHandler.onEndNested(fieldNumber, fd);
                  in.checkLastTagWas(0);
                  in.popLimit(oldLimit);
               }
               break;
            }

            case START_GROUP: {
               tagHandler.onStartNested(fieldNumber, fd);
               parseMessage(tagHandler, fd == null ? null : fd.getMessageType(), in);
               in.checkLastTagWas(WireType.makeTag(fieldNumber, WireType.WIRETYPE_END_GROUP));
               tagHandler.onEndNested(fieldNumber, fd);
               break;
            }

            case FIXED32:
            case FIXED64:
            case VARINT: {
               if (fd == null) {
                  if (wireType == WireType.FIXED32) {
                     tagHandler.onTag(fieldNumber, null, in.readFixed32());
                  } else if (wireType == WireType.FIXED64) {
                     tagHandler.onTag(fieldNumber, null, in.readFixed64());
                  } else {
                     tagHandler.onTag(fieldNumber, null, in.readUInt64());
                  }
               } else {
                  Object value = switch (fd.getType()) {
                     case DOUBLE -> in.readDouble();
                     case FLOAT -> in.readFloat();
                     case BOOL -> in.readBool();
                     case INT32 -> in.readInt32();
                     case SFIXED32 -> in.readSFixed32();
                     case FIXED32 -> in.readFixed32();
                     case UINT32 -> in.readUInt32();
                     case SINT32 -> in.readSInt32();
                     case INT64 -> in.readInt64();
                     case UINT64 -> in.readUInt64();
                     case FIXED64 -> in.readFixed64();
                     case SFIXED64 -> in.readSFixed64();
                     case SINT64 -> in.readSInt64();
                     case ENUM -> in.readEnum();
                     default ->
                           throw new IOException("Unexpected field type " + fd.getType() + " for field " + fieldNumber);
                  };
                  tagHandler.onTag(fieldNumber, fd, value);
               }
               break;
            }

            default:
               throw new IOException("Found tag with invalid wire type : tag=" + tag + ", wireType=" + wireType);
         }
      }
   }
}
