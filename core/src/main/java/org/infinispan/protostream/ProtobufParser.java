package org.infinispan.protostream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import org.infinispan.protostream.impl.WireFormat;

import java.io.IOException;
import java.io.InputStream;

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
      CodedInputStream in = CodedInputStream.newInstance(input);

      parseInternal(tagHandler, messageDescriptor, in);
   }

   public void parse(TagHandler tagHandler, Descriptor messageDescriptor, byte[] buf, int off, int len) throws IOException {
      if (messageDescriptor == null) {
         throw new IllegalArgumentException("messageDescriptor cannot be null");
      }
      CodedInputStream in = CodedInputStream.newInstance(buf, off, len);

      parseInternal(tagHandler, messageDescriptor, in);
   }

   public void parse(TagHandler tagHandler, Descriptor messageDescriptor, byte[] buf) throws IOException {
      if (messageDescriptor == null) {
         throw new IllegalArgumentException("messageDescriptor cannot be null");
      }
      CodedInputStream in = CodedInputStream.newInstance(buf);

      parseInternal(tagHandler, messageDescriptor, in);
   }

   public void parse(TagHandler tagHandler, Descriptor messageDescriptor, CodedInputStream in) throws IOException {
      if (messageDescriptor == null) {
         throw new IllegalArgumentException("messageDescriptor cannot be null");
      }

      parseInternal(tagHandler, messageDescriptor, in);
   }

   private void parseInternal(TagHandler tagHandler, Descriptor messageDescriptor, CodedInputStream in) throws IOException {
      tagHandler.onStart();

      int tag;
      while ((tag = in.readTag()) != 0) {
         final int fieldNumber = WireFormat.getTagFieldNumber(tag);
         final int wireType = WireFormat.getTagWireType(tag);
         final FieldDescriptor fd = messageDescriptor != null ? messageDescriptor.findFieldByNumber(fieldNumber) : null;

         switch (wireType) {
            case WireFormat.WIRETYPE_LENGTH_DELIMITED: {
               if (fd == null) {
                  byte[] value = in.readBytes().toByteArray();
                  tagHandler.onTag(fieldNumber, null, FieldDescriptor.Type.BYTES, FieldDescriptor.JavaType.BYTE_STRING, value);
               } else if (fd.getType() == FieldDescriptor.Type.STRING) {
                  String value = in.readString();
                  tagHandler.onTag(fieldNumber, fd.getName(), fd.getType(), fd.getJavaType(), value);
               } else if (fd.getType() == FieldDescriptor.Type.BYTES) {
                  byte[] value = in.readBytes().toByteArray();
                  tagHandler.onTag(fieldNumber, fd.getName(), fd.getType(), fd.getJavaType(), value);
               } else if (fd.getType() == FieldDescriptor.Type.MESSAGE) {
                  int length = in.readRawVarint32();
                  int oldLimit = in.pushLimit(length);
                  tagHandler.onStartNested(fieldNumber, fd.getName(), fd.getMessageType());
                  parseInternal(tagHandler, fd.getMessageType(), in);
                  tagHandler.onEndNested(fieldNumber, fd.getName(), fd.getMessageType());
                  in.checkLastTagWas(0);
                  in.popLimit(oldLimit);
               }
               break;
            }

            case WireFormat.WIRETYPE_START_GROUP: {
               if (fd != null) {
                  tagHandler.onStartNested(fieldNumber, null, null);
                  parseInternal(tagHandler, null, in);
                  in.checkLastTagWas(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_END_GROUP));
                  tagHandler.onEndNested(fieldNumber, null, null);
               } else {
                  tagHandler.onStartNested(fieldNumber, fd.getName(), fd.getMessageType());
                  parseInternal(tagHandler, fd.getMessageType(), in);
                  in.checkLastTagWas(WireFormat.makeTag(fieldNumber, WireFormat.WIRETYPE_END_GROUP));
                  tagHandler.onEndNested(fieldNumber, fd.getName(), fd.getMessageType());
               }
               break;
            }

            case WireFormat.WIRETYPE_FIXED32:
            case WireFormat.WIRETYPE_FIXED64:
            case WireFormat.WIRETYPE_VARINT: {
               if (fd == null) {
                  if (wireType == WireFormat.WIRETYPE_FIXED32) {
                     tagHandler.onTag(fieldNumber, null, null, null, in.readFixed32());
                  } else if (wireType == WireFormat.WIRETYPE_FIXED64) {
                     tagHandler.onTag(fieldNumber, null, null, null, in.readFixed64());
                  } else if (wireType == WireFormat.WIRETYPE_VARINT) {
                     tagHandler.onTag(fieldNumber, null, null, null, in.readRawVarint64());
                  }
               } else {
                  Object value;
                  switch (fd.getType()) {
                     case DOUBLE:
                        value = in.readDouble();
                        break;
                     case FLOAT:
                        value = in.readFloat();
                        break;
                     case BOOL:
                        value = in.readBool();
                        break;
                     case INT32:
                        value = in.readInt32();
                        break;
                     case SFIXED32:
                        value = in.readSFixed32();
                        break;
                     case FIXED32:
                        value = in.readFixed32();
                        break;
                     case UINT32:
                        value = in.readUInt32();
                        break;
                     case SINT32:
                        value = in.readSInt32();
                        break;
                     case INT64:
                        value = in.readInt64();
                        break;
                     case UINT64:
                        value = in.readUInt64();
                        break;
                     case FIXED64:
                        value = in.readFixed64();
                        break;
                     case SFIXED64:
                        value = in.readSFixed64();
                        break;
                     case SINT64:
                        value = in.readSInt64();
                        break;
                     case ENUM:
                        value = in.readEnum();
                        break;
                     default:
                        throw new IOException("Unexpected field type : " + fd.getType());
                  }
                  tagHandler.onTag(fieldNumber, fd.getName(), fd.getType(), fd.getJavaType(), value);     //todo all fd.getName() calls are a perf problem!
               }
               break;
            }

            default:
               throw new IOException("Found tag with invalid wire type : " + tag);
         }
      }

      tagHandler.onEnd();
   }
}
