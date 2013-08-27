package org.infinispan.protostream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;
import org.infinispan.protostream.impl.ProtoStreamReaderImpl;
import org.infinispan.protostream.impl.ProtoStreamWriterImpl;
import org.infinispan.protostream.impl.SerializationContextImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author anistor@redhat.com
 */
public class ProtobufUtil {

   public static SerializationContext newSerializationContext() {
      return new SerializationContextImpl();
   }

   public static <A> void writeTo(SerializationContext ctx, CodedOutputStream out, A t) throws IOException {
      if (t == null) {
         throw new IllegalArgumentException("Object to marshall cannot be null");
      }
      ProtoStreamWriterImpl writer = new ProtoStreamWriterImpl(ctx);
      writer.write(out, t);
   }

   public static void writeTo(SerializationContext ctx, OutputStream out, Object t) throws IOException {
      writeTo(ctx, CodedOutputStream.newInstance(out), t);
   }

   public static byte[] toByteArray(SerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      writeTo(ctx, baos, t);
      return baos.toByteArray();
   }

   public static <A> A readFrom(SerializationContext ctx, CodedInputStream in, Class<A> clazz) throws IOException {
      ProtoStreamReaderImpl reader = new ProtoStreamReaderImpl(ctx);
      return reader.read(in, clazz);
   }

   public static <A> A readFrom(SerializationContext ctx, InputStream in, Class<A> clazz) throws IOException {
      return readFrom(ctx, CodedInputStream.newInstance(in), clazz);
   }

   public static <A> A fromByteArray(SerializationContext ctx, byte[] bytes, Class<A> clazz) throws IOException {
      return readFrom(ctx, new ByteArrayInputStream(bytes), clazz);
   }

   public static <A> A fromByteArray(SerializationContext ctx, byte[] bytes, int offset, int length, Class<A> clazz) throws IOException {
      return readFrom(ctx, new ByteArrayInputStream(bytes, offset, length), clazz);
   }

   private static final int wrappedDouble = 1;
   private static final int wrappedFloat = 2;
   private static final int wrappedInt64 = 3;
   private static final int wrappedUInt64 = 4;
   private static final int wrappedInt32 = 5;
   private static final int wrappedFixed64 = 6;
   private static final int wrappedFixed32 = 7;
   private static final int wrappedBool = 8;
   private static final int wrappedString = 9;
   private static final int wrappedBytes = 10;
   private static final int wrappedUInt32 = 11;
   private static final int wrappedSFixed32 = 12;
   private static final int wrappedSFixed64 = 13;
   private static final int wrappedSInt32 = 14;
   private static final int wrappedSInt64 = 15;
   private static final int wrappedDescriptorFullName = 16;
   private static final int wrappedMessageBytes = 17;
   private static final int wrappedEnum = 18;

   public static byte[] toWrappedByteArray(SerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      CodedOutputStream out = CodedOutputStream.newInstance(baos);
      toWrappedByteArray(ctx, out, t);
      return baos.toByteArray();
   }

   //todo find better name
   public static void toWrappedByteArray(SerializationContext ctx, CodedOutputStream out, Object t) throws IOException {
      if (t == null) {
         return;
      }

      if (t instanceof String) {
         out.writeString(wrappedString, (String) t);
      } else if (t instanceof Long) {
         out.writeInt64(wrappedInt64, (Long) t);
      } else if (t instanceof Integer) {
         out.writeInt32(wrappedInt32, (Integer) t);
      } else if (t instanceof Double) {
         out.writeDouble(wrappedDouble, (Double) t);
      } else if (t instanceof Float) {
         out.writeFloat(wrappedFloat, (Float) t);
      } else if (t instanceof Boolean) {
         out.writeBool(wrappedBool, (Boolean) t);
      } else if (t instanceof byte[]) {
         byte[] bytes = (byte[]) t;
         out.writeTag(wrappedBytes, WireFormat.WIRETYPE_LENGTH_DELIMITED);
         out.writeRawVarint32(bytes.length);
         out.writeRawBytes(bytes);
      } else if (t instanceof Enum) {
         // use an enum encoder
         EnumMarshaller enumMarshaller = (EnumMarshaller) ctx.getMarshaller((Class<Enum>) t.getClass());
         out.writeString(wrappedDescriptorFullName, enumMarshaller.getFullName());
         out.writeEnum(wrappedEnum, enumMarshaller.encode((Enum) t));
      } else {
         // this is either an unknown primitive type or a message type
         // try to use a message marshaller
         BaseMarshaller marshaller = ctx.getMarshaller(t.getClass());
         out.writeString(wrappedDescriptorFullName, marshaller.getFullName());

         ByteArrayOutputStream buffer = new ByteArrayOutputStream();      //todo [anistor] here we should use a better buffer allocation strategy
         ProtoStreamWriterImpl writer = new ProtoStreamWriterImpl(ctx);
         writer.write(CodedOutputStream.newInstance(buffer), t);

         out.writeTag(wrappedMessageBytes, WireFormat.WIRETYPE_LENGTH_DELIMITED);
         out.writeRawVarint32(buffer.size());
         out.writeRawBytes(buffer.toByteArray());
      }
      out.flush();
   }

   /**
    * Parses a top-level message that was wrapped according to the org.infinispan.protostream.WrappedMessage proto
    * definition.
    *
    * @param ctx
    * @param bytes
    * @return
    * @throws IOException
    */
   public static Object fromWrappedByteArray(SerializationContext ctx, byte[] bytes) throws IOException {
      return fromWrappedByteArray(ctx, bytes, 0, bytes.length);
   }

   public static Object fromWrappedByteArray(SerializationContext ctx, byte[] bytes, int offset, int length) throws IOException {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes, offset, length);
      CodedInputStream in = CodedInputStream.newInstance(bais);
      return fromWrappedByteArray(ctx, in);
   }

   public static Object fromWrappedByteArray(SerializationContext ctx, CodedInputStream in) throws IOException {
      String descriptorFullName = null;
      int enumValue = -1;
      byte[] messageBytes = null;
      Object value = null;
      int readTags = 0;

      int tag;
      while ((tag = in.readTag()) != 0) {
         readTags++;
         switch (tag) {
            case wrappedDescriptorFullName << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED:
               descriptorFullName = in.readString();
               break;
            case wrappedEnum << 3 | WireFormat.WIRETYPE_VARINT:
               enumValue = in.readEnum();
               break;
            case wrappedMessageBytes << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED:
               messageBytes = in.readBytes().toByteArray();
               break;
            case wrappedString << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED:
               value = in.readString();
               break;
            case wrappedBytes << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED:
               value = in.readBytes().toByteArray();
               break;
            case wrappedBool << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readBool();
               break;
            case wrappedDouble << 3 | WireFormat.WIRETYPE_FIXED64:
               value = in.readDouble();
               break;
            case wrappedFloat << 3 | WireFormat.WIRETYPE_FIXED32:
               value = in.readFloat();
               break;
            case wrappedFixed32 << 3 | WireFormat.WIRETYPE_FIXED32:
               value = in.readFixed32();
               break;
            case wrappedSFixed32 << 3 | WireFormat.WIRETYPE_FIXED32:
               value = in.readSFixed32();
               break;
            case wrappedFixed64 << 3 | WireFormat.WIRETYPE_FIXED64:
               value = in.readFixed64();
               break;
            case wrappedSFixed64 << 3 | WireFormat.WIRETYPE_FIXED64:
               value = in.readSFixed64();
               break;
            case wrappedInt64 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readInt64();
               break;
            case wrappedUInt64 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readUInt64();
               break;
            case wrappedSInt64 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readSInt64();
               break;
            case wrappedInt32 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readInt32();
               break;
            case wrappedUInt32 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readUInt32();
               break;
            case wrappedSInt32 << 3 | WireFormat.WIRETYPE_VARINT:
               value = in.readSInt32();
               break;
            default:
               throw new IllegalStateException("Unexpected tag : " + tag);
         }
      }

      if (value == null && descriptorFullName == null && messageBytes == null) {
         return null;
      }

      if (value != null) {
         if (readTags == 1) {
            return value;
         }
         throw new IOException("Invalid message encoding.");
      }

      if (descriptorFullName == null || readTags != 2) {
         throw new IOException("Invalid message encoding.");
      }

      if (messageBytes != null) {
         BaseMarshaller marshaller = ctx.getMarshaller(descriptorFullName);
         ByteArrayInputStream bais2 = new ByteArrayInputStream(messageBytes);
         CodedInputStream in2 = CodedInputStream.newInstance(bais2);
         if (marshaller instanceof MessageMarshaller) {
            ProtoStreamReaderImpl reader = new ProtoStreamReaderImpl(ctx);
            return reader.read(in2, (MessageMarshaller) marshaller);
         } else {
            return ((RawProtobufMarshaller) marshaller).readFrom(ctx, in2);
         }
      } else {
         EnumMarshaller enumMarshaller = (EnumMarshaller) ctx.getMarshaller(descriptorFullName);
         return enumMarshaller.decode(enumValue);
      }
   }
}
