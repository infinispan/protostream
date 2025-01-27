package org.infinispan.protostream.impl.json;

import static org.infinispan.protostream.WrappedMessage.PROTOBUF_TYPE_ID;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_CONTAINER_MESSAGE;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_CONTAINER_SIZE;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_CONTAINER_TYPE_NAME;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_DATE_MILLIS;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_ENUM;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_INSTANT_NANOS;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_INSTANT_SECONDS;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_MESSAGE;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_TYPE_ID;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_TYPE_NAME;
import static org.infinispan.protostream.impl.json.JsonHelper.JSON_TYPE_FIELD;
import static org.infinispan.protostream.impl.json.JsonHelper.JSON_VALUE_FIELD;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufParser;
import org.infinispan.protostream.TagHandler;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.descriptors.AnnotatedDescriptor;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.EnumValueDescriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.descriptors.Type;

/**
 * Base class for all writers.
 * <p>
 * This base class handles all the base types, i.e., the minimal unit to write to the JSON output. The base types include
 * those written with a {@link WrappedMessage} and primitive types (int, long, etc.). This writer is only responsible for
 * unwrapping these primitives and delegating to the appropriate writer, it does not handle special formatting.
 * </p>
 *
 * <p>
 * This class maintains the internal stack with the JSON tokens to write later, and it propagates these values when
 * creating a new delegate writer.
 *
 * <h2>JSON Requirements:</h2>
 *
 * <p>
 * The JSON create by the conversion of the ProtoStream serialized object has some
 * requirements:
 *
 * <ul>
 *    <li>New objects must identify the {@link Descriptor} to detail the object. This is the first entry in the
 *    JSON object with the {@link JsonHelper#JSON_TYPE_FIELD} followed by the type name.</li>
 *    <li>Base (unwrapped) primitives use the spacial {@link JsonHelper#JSON_VALUE_FIELD} to write the value.</li>
 * </ul>
 *
 * These requirements are necessary when parsing the JSON back into the byte stream. This allows to transfer objects
 * in JSON format for easy visualization and convert the back to the ProtoStream format.
 * </p>
 *
 * <h2>Writing strategy:</h2>
 * <p>
 * To maintain these requirements, this class has the follow approach:
 *
 * <ul>
 *    <li>{@link #onStart(GenericDescriptor)}: When beginning to process a new entry of the proto stream.
 *    Write the {@link JsonHelper#JSON_TYPE_FIELD} field at this stage.
 *    </li>
 *
 *    <li>{@link #onTag(int, FieldDescriptor, Object)}: When reading fields from the proto object. This method will
 *    handle the base primitives (int, long, etc.), wrapped primitives, and special field for wrapped objects. At this
 *    stage, a delegate might be created to handle the remaining fields in the stream.
 *    </li>
 *
 *    <li>{@link #onEnd()}: When the object/stream has finished. First, close the delegate and then close the outer
 *    object.
 *    </li>
 * </ul>
 *
 * When creating a new delegate, we must ensure whether it requires a new instance. Repeated objects might be written
 * sequentially pointing to the same field number. This requires to identify the field and keep utilizing the same
 * delegate for every subsequent field. When the field changes, we need to guarantee the delegate was closed.
 * </p>
 *
 * <p>
 * The base writer follow a similar strategy of the <code>VALUE</code> state machine:
 *
 * <pre>
 *                            ┌────────┐
 *                       ┌────┤ STRING ├───┐
 *                       │    └────────┘   │
 *                       │    ┌────────┐   │
 *                       │────┼ NUMBER ├───┼
 *                       │    └────────┘   │
 *                       │    ┌────────┐   │
 *                       │────┤ OBJECT ├───┤
 *     ┌─────────┐       │    └────────┘   │
 *     │  VALUE  ┼───────┤                 ├─────►
 *     └─────────┘       │                 │
 *                       │    ┌───────┐    │
 *                       ┼────┼ ARRAY ├────┤
 *                       │    └───────┘    │
 *                       │    ┌──────┐     │
 *                       ┼────┤ BOOL ┼─────┤
 *                       │    └──────┘     │
 *                       │    ┌──────┐     │
 *                       └────┤ NULL ├─────┘
 *                            └──────┘
 * </pre>
 *
 * Where we have the specialized handlers:
 * <ul>
 *    <li><code>OBJECT</code>: Delegates to {@link ObjectJsonWriter} or {@link MapJsonWriter}, depending on the field.</li>
 *    <li><code>ARRAY</code>: Delegates to {@link ArrayJsonWriter}.</li>
 *    <li>The remaining fields are the primitives handled directly by this writer.</li>
 * </ul>
 * </p>
 *
 * @author José Bolina
 * @see <a href="https://www.json.org/json-en.html">JSON specification.</a>
 * @see <a href="https://protobuf.dev/programming-guides/json/">ProtoJSON Format</a>
 */
abstract class BaseJsonWriter implements FieldAwareTagHandler {
   // Z-normalized RFC 3339 format
   private static final String RFC_3339_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
   static final int EMPTY_FIELD_NUMBER = Integer.MIN_VALUE;

   // The descriptor of the current field.
   // This is not-null only when dealing with nested messages:
   // {"key": {...}}
   // This will hold information about the field "key".
   protected final FieldDescriptor descriptor;

   // Holds the tokens to generate the JSON in string format and the context to read the fields/descriptors.
   protected final List<JsonTokenWriter> ast;
   protected final ImmutableSerializationContext ctx;

   // The current delegate to redirect operations.
   private FieldAwareTagHandler delegate;

   // Maintain information about the wrapper.
   // This is a ONE_OF element, it will be either the ID or the name.
   protected Integer wrappedTypeId;
   protected String wrappedTypeName;

   protected BaseJsonWriter(ImmutableSerializationContext ctx, List<JsonTokenWriter> ast, FieldDescriptor descriptor) {
      this.ctx = ctx;
      this.ast = ast;
      this.descriptor = descriptor;
   }

   protected BaseJsonWriter(ImmutableSerializationContext ctx, List<JsonTokenWriter> ast) {
      this(ctx, ast, null);
   }

   @Override
   public int field() {
      return descriptor != null
            ? descriptor.getNumber()
            : EMPTY_FIELD_NUMBER;
   }

   @Override
   public boolean isDone() {
      return false;
   }

   @Override
   public boolean acceptField(int fieldNumber) {
      return true;
   }

   protected abstract boolean isRoot();

   @Override
   public final void onStart(GenericDescriptor descriptor) {
      // Special case when dealing with a container wrapper.
      // This means the actual message is not wrapped withing a WrappedMessage, it is written directly to the stream.
      // We create the delegate during the start to parse the fields of the container.
      if (JsonHelper.isContainerAdapter(ctx, descriptor)) {
         delegate = new ContainerObjectWriter(ctx, ast, null);
      }
      pushToken(JsonToken.LEFT_BRACE);
      if (descriptor == null) return;

      writeType(descriptor);
   }

   @Override
   public abstract void onStartNested(int fieldNumber, FieldDescriptor fieldDescriptor);

   @Override
   public void onTag(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
      if (delegate != null) {
         // There are cases when the field the delegate was handling has ended without a `onEnd` invocation.
         // Then we verify whether the fields have changed and that it can't handle the new field.
         // We force it to finish and continue handling the field.
         if (delegate.field() != fieldNumber && !delegate.acceptField(fieldNumber)) {
            delegate.onEndNested(delegate.field(), null);
            delegate = null;
            onTag(fieldNumber, fieldDescriptor, tagValue);
            return;
         }
         delegate.onTag(fieldNumber, fieldDescriptor, tagValue);
         return;
      }

      // Ignore unknown fields.
      if (fieldDescriptor == null)
         return;

      // When we are handling a deeper message, we might have primitive values which are not wrapped.
      // We identify this when this is not the root writer (we are handling an objects and the fields),
      // and the field descriptor doesn't point to a message or an enum.
      if (fieldDescriptor.getMessageType() == null && fieldDescriptor.getEnumType() == null && !WrappedMessage.isWrappedMessageField(fieldDescriptor)) {
         writeTerminalField(fieldNumber, fieldDescriptor, tagValue);
         return;
      }

      switch (fieldNumber) {
         // Protobuf contains an embedded type identified by an identifier. This is a descriptor of a nested message.
         case WRAPPED_TYPE_ID:
            wrappedTypeId = (Integer) tagValue;
            pushToken(JsonToken.COMMA);
            break;

         // Protobuf contains an embedded type identified by a name. This is the descriptor of a nested message.
         case WRAPPED_TYPE_NAME:
            wrappedTypeName = (String) tagValue;
            pushToken(JsonToken.COMMA);
            break;

         // The field is a Java enum.
         // The wire serialization is an integer number.
         // We use the descriptor to translate the number to the appropriate enum, and write the name in the JSON.
         case WRAPPED_ENUM: {
            if (field() != fieldNumber) {
               pushToken(JsonTokenWriter.string(fieldDescriptor.getName()));
               pushToken(JsonToken.COLON);

               EnumDescriptor descriptor = (EnumDescriptor) getDescriptor();
               String enumConstantName = descriptor.findValueByNumber((Integer) tagValue).getName();
               TagHandler child = objectWriter(fieldNumber, fieldDescriptor);
               child.onStart(descriptor);
               child.onTag(fieldNumber, fieldDescriptor, enumConstantName);
               child.onEnd();
            } else {
               writeValue(fieldDescriptor, tagValue);
            }

            break;
         }

         // The contents of a nested message.
         // When reaching this point, either the type id or name should be set.
         case WRAPPED_MESSAGE: {
            JsonToken lt = lastToken();
            if (lt != null && lt != JsonToken.COLON) {
               // If the last seen thing was a value, we need a comma before inserting the new field.
               if (JsonToken.followedByComma(lt)) {
                  pushToken(JsonToken.COMMA);
               }
               pushToken(JsonTokenWriter.string(fieldDescriptor.getName()));
               pushToken(JsonToken.COLON);
            }
            wrappedMessage((byte[]) tagValue, fieldDescriptor);
            break;
         }

         // A container is a sized wrapper around many elements.
         // It starts as an object, we read 3 fields: name, size, message.
         // After that, there will be <size> elements to parse.
         case WRAPPED_CONTAINER_TYPE_NAME: {
            if (field() != fieldNumber) {
               String type = (String) tagValue;
               GenericDescriptor descriptor = ctx.getDescriptorByName(type);
               delegate = objectWriter(fieldNumber, fieldDescriptor);

               // This means it is missing the comma, the field name, and then it can start the nested object.
               if (JsonToken.followedByComma(lastToken())) {
                  pushToken(JsonToken.COMMA);
                  pushToken(JsonTokenWriter.string(fieldDescriptor.getName()));
                  pushToken(JsonToken.COLON);
               }

               delegate.onStart(descriptor);
            }
            break;
         }

         // Identifies a java.time.Instant object.
         // These objects are here for backwards compatibility. They are hand-written by the WrappedMessage class.
         case WRAPPED_INSTANT_NANOS:
         case WRAPPED_INSTANT_SECONDS:
            if (field() == EMPTY_FIELD_NUMBER) {
               GenericDescriptor descriptor = getProtoStreamDescriptor(Instant.class);
               delegate = objectWriter(fieldNumber, fieldDescriptor);
               delegate.onStart(descriptor);
               delegate.onTag(fieldNumber, fieldDescriptor, tagValue);
               break;
            }
            writeValue(fieldDescriptor, tagValue);
            break;

         // Reads a java.util.Date object.
         // This has the same reason as the Instant objects above.
         case WRAPPED_DATE_MILLIS: {
            if (field() != fieldNumber) {
               GenericDescriptor descriptor = getProtoStreamDescriptor(Date.class);
               delegate = objectWriter(fieldNumber, fieldDescriptor);
               delegate.onStart(descriptor);
               delegate.onTag(fieldNumber, fieldDescriptor, tagValue);
               break;
            }
            writeValue(fieldDescriptor, tagValue);
            break;
         }

         case WRAPPED_CONTAINER_MESSAGE:

         // Wrapped container fallthrough in this case to write the container size.
         case WRAPPED_CONTAINER_SIZE:
         // Handle every other primitive value.
         // These primitives are wrapped by a message.
         case WrappedMessage.WRAPPED_BYTE:
         case WrappedMessage.WRAPPED_SHORT:
         case WrappedMessage.WRAPPED_DOUBLE:
         case WrappedMessage.WRAPPED_FLOAT:
         case WrappedMessage.WRAPPED_INT64:
         case WrappedMessage.WRAPPED_UINT64:
         case WrappedMessage.WRAPPED_INT32:
         case WrappedMessage.WRAPPED_FIXED64:
         case WrappedMessage.WRAPPED_FIXED32:
         case WrappedMessage.WRAPPED_BOOL:
         case WrappedMessage.WRAPPED_STRING:
         case WrappedMessage.WRAPPED_BYTES:
         case WrappedMessage.WRAPPED_UINT32:
         case WrappedMessage.WRAPPED_SFIXED32:
         case WrappedMessage.WRAPPED_SFIXED64:
         case WrappedMessage.WRAPPED_SINT32:
         case WrappedMessage.WRAPPED_SINT64:
            writeTerminalField(fieldNumber, fieldDescriptor, tagValue);
            break;
      }
   }

   @Override
   public void onEnd() {
      if (delegate != null) {
         delegate.onEnd();
         delegate = null;
      }
      pushToken(JsonToken.RIGHT_BRACE);
   }

   private void writeTerminalField(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue) {
      if (field() != fieldNumber) {
         if (fieldDescriptor.isRepeated()) {
            delegate = repeatedWriter(fieldNumber, fieldDescriptor);
            delegate.onStartNested(fieldNumber, fieldDescriptor);
            delegate.onTag(fieldNumber, fieldDescriptor, tagValue);
            return;
         }

         // If we are the current root and are writing a primitive, we need to initialize a proper delegate.
         if (isRoot()) {
            TagHandler root = new RootJsonWriter(ctx, ast, fieldDescriptor, true);
            root.onStartNested(fieldNumber, fieldDescriptor);
            root.onTag(fieldNumber, fieldDescriptor, tagValue);
            root.onEndNested(fieldNumber, fieldDescriptor);
            return;
         }
      }
      writeValue(fieldDescriptor, tagValue);
   }

   private void writeValue(FieldDescriptor fieldDescriptor, Object tagValue) {
      // Type is already written, now we write the comma and the value.
      if (JsonToken.followedByComma(lastToken()))
         pushToken(JsonToken.COMMA);

      // Writing a primitive from the root means this is just a primitive wrapped value, i.e., it is not contained by an object.
      // Therefore, we need to use the reserved field "_value" to write the primitive value.
      // In any other case, this field is contained by an object, we can use the field name to write the value.
      String name = isRoot() ? JSON_VALUE_FIELD : fieldDescriptor.getName();
      pushToken(JsonTokenWriter.string(name));
      pushToken(JsonToken.COLON);
      writeTagValue(fieldDescriptor, tagValue);
   }

   protected final void writePrimitiveOrDispatch(int fieldNumber, FieldDescriptor fieldDescriptor, Object tagValue, TagHandler dispatcher) {
      switch (fieldNumber) {
         case WrappedMessage.WRAPPED_BYTE:
         case WrappedMessage.WRAPPED_SHORT:
         case WrappedMessage.WRAPPED_DOUBLE:
         case WrappedMessage.WRAPPED_FLOAT:
         case WrappedMessage.WRAPPED_INT64:
         case WrappedMessage.WRAPPED_UINT64:
         case WrappedMessage.WRAPPED_INT32:
         case WrappedMessage.WRAPPED_FIXED64:
         case WrappedMessage.WRAPPED_FIXED32:
         case WrappedMessage.WRAPPED_BOOL:
         case WrappedMessage.WRAPPED_STRING:
         case WrappedMessage.WRAPPED_BYTES:
         case WrappedMessage.WRAPPED_UINT32:
         case WrappedMessage.WRAPPED_SFIXED32:
         case WrappedMessage.WRAPPED_SFIXED64:
         case WrappedMessage.WRAPPED_SINT32:
         case WrappedMessage.WRAPPED_SINT64:
            writeRawPrimitive(fieldDescriptor, tagValue);
            break;
         default:
            if (dispatcher == null)
               throw new IllegalStateException("Dispatcher required to handle field: " + fieldDescriptor);

            dispatcher.onTag(fieldNumber, fieldDescriptor, tagValue);
            break;
      }
   }

   private void writeRawPrimitive(FieldDescriptor fieldDescriptor, Object tagValue) {
      if (JsonToken.followedByComma(lastToken()))
         pushToken(JsonToken.COMMA);

      writeTagValue(fieldDescriptor, tagValue);
   }

   protected final void writeType(AnnotatedDescriptor descriptor) {
      String type;
      if (descriptor instanceof FieldDescriptor fd) {
         if (fd.getType() == Type.MESSAGE) {
            writeType(fd.getMessageType());
            return;
         }

         type = fd.getTypeName();
      } else {
         type = descriptor.getFullName();
      }

      pushToken(JsonTokenWriter.string(JSON_TYPE_FIELD));
      pushToken(JsonToken.COLON);
      pushToken(JsonTokenWriter.value(type, true));
   }

   private GenericDescriptor getDescriptor() {
      if (wrappedTypeId != null)
         return ctx.getDescriptorByTypeId(wrappedTypeId);

      if (wrappedTypeName != null)
         return ctx.getDescriptorByName(wrappedTypeName);

      return ctx.getDescriptorByTypeId(PROTOBUF_TYPE_ID);
   }

   private void wrappedMessage(byte[] message, FieldDescriptor fd) {
      try {
         Descriptor descriptor = (Descriptor) getDescriptor();
         BaseJsonWriter handler = new ObjectJsonWriter(ctx, ast, fd);
         ProtobufParser.INSTANCE.parse(handler, descriptor, message);
      } catch (IOException e) {
         throw new RuntimeException("Failed handling nested message", e);
      }
   }

   private GenericDescriptor getProtoStreamDescriptor(Class<?> clazz) {
      BaseMarshaller<?> marshaller = ctx.getMarshaller(clazz);
      if (marshaller == null)
         throw new IllegalStateException(String.format("Unable to convert %s to JSON", clazz));

      return ctx.getDescriptorByName(marshaller.getTypeName());
   }

   protected void pushToken(JsonTokenWriter token) {
      ast.add(token);
   }

   protected void replaceLastToken(JsonToken prev, JsonTokenWriter curr) {
      if (ast.isEmpty()) return;

      int last = ast.size() - 1;
      JsonTokenWriter jtw = Objects.requireNonNull(ast.get(last));
      if (jtw.token() != prev)
         throw new IllegalArgumentException("Last token is not " + prev);

      ast.set(last, curr);
   }

   protected final JsonToken lastToken() {
      if (ast.isEmpty()) return null;

      JsonTokenWriter jtw = Objects.requireNonNull(ast.get(ast.size() - 1));
      return jtw.token();
   }

   protected final void writeTagValue(FieldDescriptor fd, Object tagValue) {
      switch (fd.getType()) {
         case STRING -> pushToken(JsonTokenWriter.value(escapeJson((String) tagValue, true), true));
         case UINT64, FIXED64 -> pushToken(JsonTokenWriter.value(Long.toUnsignedString((Long) tagValue), false));
         case UINT32, FIXED32 -> pushToken(JsonTokenWriter.value(Integer.toUnsignedString((Integer) tagValue), false));
         case FLOAT -> {
            Float f = (Float) tagValue;
            if (f.isInfinite() || f.isNaN()) {
               pushToken(JsonTokenWriter.value(Float.toString((Float) tagValue), true));
            } else {
               pushToken(JsonTokenWriter.value(tagValue, false));
            }
         }
         case DOUBLE -> {
            Double d = (Double) tagValue;
            if (d.isInfinite() || d.isNaN()) {
               pushToken(JsonTokenWriter.value(Double.toString((Double) tagValue), true));
            } else {
               pushToken(JsonTokenWriter.value(tagValue, false));
            }
         }
         case ENUM -> {
            EnumValueDescriptor evd = fd.getEnumType().findValueByNumber((Integer) tagValue);
            pushToken(JsonTokenWriter.value(evd.getName(), true));
         }
         case BYTES -> {
            String encoded = Base64.getEncoder().encodeToString((byte[]) tagValue);
            pushToken(JsonTokenWriter.value(encoded, true));
         }
         default -> {
            if (tagValue instanceof Date d) {
               pushToken(JsonTokenWriter.value(formatDate(d), true));
               break;
            }

            pushToken(JsonTokenWriter.value(tagValue, tagValue instanceof CharSequence));
         }
      }
   }

   protected final FieldAwareTagHandler objectWriter(int fieldNumber, FieldDescriptor fd) {
      FieldAwareTagHandler d = verifyDelegate(fieldNumber, fd);
      return d == null ? new ObjectJsonWriter(ctx, ast, fd) : d;
   }

   protected final FieldAwareTagHandler repeatedWriter(int fieldNumber, FieldDescriptor fd) {
      FieldAwareTagHandler d = verifyDelegate(fieldNumber, fd);
      return d == null ? new ArrayJsonWriter(ctx, ast, fd) : d;
   }

   protected final FieldAwareTagHandler mapWriter(int fieldNumber, FieldDescriptor fd) {
      FieldAwareTagHandler d = verifyDelegate(fieldNumber, fd);
      return d == null ? new MapJsonWriter(ctx, ast, fd) : d;
   }

   private FieldAwareTagHandler verifyDelegate(int fieldNumber, FieldDescriptor fd) {
      if (delegate == null) return null;

      if (delegate.field() == fieldNumber)
         return delegate;

      delegate.onEndNested(fieldNumber, fd);
      delegate = null;
      return null;
   }

   /**
    * Escapes a string literal in order to have a valid JSON representation. Optionally it can also escape some html chars.
    */
   private static String escapeJson(String unsafe, boolean htmlSafe) {
      StringBuilder out = new StringBuilder();
      int prev = 0;
      int len = unsafe.length();
      for (int cur = 0; cur < len; cur++) {
         char ch = unsafe.charAt(cur);
         String esc = null;
         if (ch < ' ') {
            esc = switch (ch) {
               case '\t' -> "\\t";
               case '\b' -> "\\b";
               case '\n' -> "\\n";
               case '\r' -> "\\r";
               case '\f' -> "\\f";
               default -> String.format("\\u%04x", (int) ch);
            };
         } else if (ch < 128) {
            if (ch == '"') {
               esc = "\\\"";
            } else if (ch == '\\') {
               esc = "\\\\";
            } else if (htmlSafe) {
               esc = switch (ch) {
                  case '<' -> "\\u003c";
                  case '>' -> "\\u003e";
                  case '&' -> "\\u0026";
                  case '=' -> "\\u003d";
                  case '\'' -> "\\u0027";
                  default -> null;
               };
            }
         } else if (ch == '\u2028') {
            esc = "\\u2028";
         } else if (ch == '\u2029') {
            esc = "\\u2029";
         } else {
            continue;
         }
         if (esc != null) {
            if (prev < cur) {
               out.append(unsafe, prev, cur);
            }
            prev = cur + 1;
            out.append(esc);
         }
      }
      if (prev < len) {
         out.append(unsafe, prev, len);
      }

      return out.toString();
   }

   private static String formatDate(Date date) {
      return timestampFormat.get().format(date);
   }

   private static final ThreadLocal<DateFormat> timestampFormat = ThreadLocal.withInitial(() -> {
      // Z-normalized RFC 3339 format
      SimpleDateFormat sdf = new SimpleDateFormat(RFC_3339_DATE_FORMAT);
      GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
      calendar.setGregorianChange(new Date(Long.MIN_VALUE));
      sdf.setCalendar(calendar);
      return sdf;
   });

   static boolean isComplexType(FieldDescriptor descriptor) {
      Objects.requireNonNull(descriptor, "descriptor is null");
      return descriptor.getMessageType() != null && descriptor.getMessageType().getFields().size() > 1;
   }
}
